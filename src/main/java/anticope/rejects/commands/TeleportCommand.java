package anticope.rejects.commands;

import anticope.rejects.arguments.ClientPosArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand extends Command {


    public TeleportCommand() {
        super("teleport", "Sends a packet to the server with new position. Allows to teleport small distances.", "tp");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("pos", ClientPosArgumentType.pos()).executes(ctx -> {
            if (mc.player == null) return SINGLE_SUCCESS;
            Vec3 pos = ClientPosArgumentType.getPos(ctx, "pos");
            mc.player.absSnapTo(pos.x(), pos.y(), pos.z());
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        pos.x(), pos.y(), pos.z(), mc.player.onGround(), mc.player.horizontalCollision));
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("pos", ClientPosArgumentType.pos()).then(argument("yaw", FloatArgumentType.floatArg()).then(argument("pitch", FloatArgumentType.floatArg()).executes(ctx -> {
            if (mc.player == null) return SINGLE_SUCCESS;
            Vec3 pos = ClientPosArgumentType.getPos(ctx, "pos");
            float yaw = FloatArgumentType.getFloat(ctx, "yaw");
            float pitch = net.minecraft.util.Mth.clamp(FloatArgumentType.getFloat(ctx, "pitch"), -90.0f, 90.0f);
            mc.player.absSnapTo(pos.x(), pos.y(), pos.z(), yaw, pitch);
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                        pos.x(), pos.y(), pos.z(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
            }
            return SINGLE_SUCCESS;
        }))));
    }
}

