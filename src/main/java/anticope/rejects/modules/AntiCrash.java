package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class AntiCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> log = sgGeneral.add(new BoolSetting.Builder()
            .name("log")
            .description("Logs when crash packet detected.")
            .defaultValue(false)
            .build()
    );

    public AntiCrash() {
        super(MeteorRejectsAddon.CATEGORY, "anti-crash", "Attempts to cancel packets that may crash the client.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundExplodePacket packet) {
            Vec3 explodePos = packet.center();
            Vec3 playerKnockback = packet.playerKnockback().orElse(Vec3.ZERO);
            if (isOutOfBounds(explodePos, 30_000_000) || isOutOfBounds(playerKnockback, 30_000_000)) {
                cancel(event);
            }
        } else if (event.packet instanceof ClientboundLevelParticlesPacket packet) {
            // too many particles
            if (packet.getCount() > 100_000) cancel(event);
        } else if (event.packet instanceof ClientboundPlayerPositionPacket packet) {
            Vec3 playerPos = packet.change().position();
            // out of world movement
            if (isOutOfBounds(playerPos, 30_000_000)) cancel(event);
        } else if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            // velocity
            if (isOutOfBounds(packet.movement(), 1000)) cancel(event);
        }
    }

    private static boolean isOutOfBounds(Vec3 vec, double limit) {
        return vec == null
                || !Double.isFinite(vec.x) || !Double.isFinite(vec.y) || !Double.isFinite(vec.z)
                || Math.abs(vec.x) > limit || Math.abs(vec.y) > limit || Math.abs(vec.z) > limit;
    }

    private void cancel(PacketEvent.Receive event) {
        if (log.get()) warning("Server attempts to crash you");
        event.cancel();
    }
}
