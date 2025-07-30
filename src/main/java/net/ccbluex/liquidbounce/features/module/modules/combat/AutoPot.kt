/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.AttackEvent;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;

import java.util.concurrent.LinkedBlockingQueue;

public class LegitReach extends Module {

    private final ListValue mode = new ListValue("Mode", new String[]{"FakePlayer", "AllIncomingPackets", "TargetPackets"}, "FakePlayer");
    private final BoolValue aura = new BoolValue("Aura", false);
    private final IntegerValue pulseDelay = new IntegerValue("PulseDelay", 200, 50, 500);
    private final IntegerValue intavePacketDelay = new IntegerValue("Packets", 5, 0, 30);

    private final MSTimer pulseTimer = new MSTimer();
    private EntityLivingBase currentTarget = null;
    private EntityOtherPlayerMP fakePlayer = null;
    private boolean shown = false;
    private final LinkedBlockingQueue<Packet<INetHandlerPlayClient>> packets = new LinkedBlockingQueue<>();

    public LegitReach() {
        super("LegitReach", ModuleCategory.COMBAT);
    }

    @Override
    public void onEnable() {
        shown = false;
    }

    @Override
    public void onDisable() {
        removeFakePlayer();
        clearPackets();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mode.get().equalsIgnoreCase("FakePlayer")) {
            if (event.getTargetEntity() instanceof EntityLivingBase) {
                currentTarget = (EntityLivingBase) event.getTargetEntity();
                createFakePlayer();
                event.cancelEvent();
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (mode.get().equalsIgnoreCase("AllIncomingPackets") && currentTarget != null) {
            if (packet.getClass().getSimpleName().startsWith("S")) {
                event.cancelEvent();
                packets.add((Packet<INetHandlerPlayClient>) packet);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!mode.get().equalsIgnoreCase("FakePlayer")) {
            if (pulseTimer.hasTimePassed(pulseDelay.get())) {
                pulseTimer.reset();
                clearPackets();
            }
        } else {
            if (fakePlayer != null && currentTarget != null) {
                syncFakePlayer();
            }
        }
    }

    private void createFakePlayer() {
        if (currentTarget == null) return;

        fakePlayer = new EntityOtherPlayerMP(MinecraftInstance.mc.theWorld, currentTarget.getGameProfile());
        fakePlayer.copyLocationAndAnglesFrom(currentTarget);

        for (int i = 0; i <= 4; i++) {
            ItemStack stack = currentTarget.getEquipmentInSlot(i);
            if (stack != null) {
                fakePlayer.setCurrentItemOrArmor(i, ItemStack.copyItemStack(stack));
            }
        }

        MinecraftInstance.mc.theWorld.addEntityToWorld(-1337, fakePlayer);
        shown = true;
    }

    private void removeFakePlayer() {
        if (fakePlayer != null) {
            MinecraftInstance.mc.theWorld.removeEntityFromWorld(fakePlayer.getEntityId());
            fakePlayer = null;
            shown = false;
        }
    }

    private void syncFakePlayer() {
        if (fakePlayer == null || currentTarget == null) return;

        fakePlayer.setHealth(currentTarget.getHealth());

        for (int i = 0; i <= 4; i++) {
            ItemStack stack = currentTarget.getEquipmentInSlot(i);
            if (stack != null) {
                fakePlayer.setCurrentItemOrArmor(i, ItemStack.copyItemStack(stack));
            }
        }
    }

    private void clearPackets() {
        while (!packets.isEmpty()) {
            MinecraftInstance.mc.getNetHandler().addToSendQueue(packets.poll());
        }
    }
}
