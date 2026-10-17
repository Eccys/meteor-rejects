package anticope.rejects.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.apache.commons.codec.binary.Base64;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SaveSkinCommand extends Command {

    private final static SimpleCommandExceptionType IO_EXCEPTION = new SimpleCommandExceptionType(Component.literal("An exception occurred"));

    private final PointerBuffer filters;
    private final Gson GSON = new Gson();

    public SaveSkinCommand() {
        super("save-skin", "Download a player's skin by name.", "skin", "skinsteal");

        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer pngFilter = MemoryUtil.memASCII("*.png");

        filters.put(pngFilter);
        filters.rewind();
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(ctx -> {
            UUID id = PlayerListEntryArgumentType.get(ctx).getProfile().id();
            filters.rewind();
            String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, filters, null);
            if (path == null || path.isBlank()) return SINGLE_SUCCESS;
            if (!path.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) path += ".png";
            saveSkin(id.toString(), path);

            return SINGLE_SUCCESS;
        }));
    }

    private void saveSkin(String uuid, String path) throws CommandSyntaxException {
        try {
            //going to explain what happens so I don't forget
            //request their minecraft profile, all so we can get a base64 encoded string that contains ANOTHER json that then has the skin URL
            String PROFILE_REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

            String rawProfileJson = Http.get(String.format(PROFILE_REQUEST_URL, uuid)).sendString();
            String skinURL = anticope.rejects.utils.RejectsUtils.extractSkinUrlFromProfileJson(rawProfileJson);
            if (skinURL == null) {
                throw IO_EXCEPTION.create();
            }

            java.net.URLConnection conn = new URL(skinURL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[1024];
                int n;
                while (-1 != (n = in.read(buf))) {
                    out.write(buf, 0, n);
                }
                byte[] response = out.toByteArray();
                File file = new File(path);
                try (FileOutputStream fos = new FileOutputStream(file.getPath())) {
                    fos.write(response);
                }
            }
        } catch (IOException | RuntimeException e) {
            throw IO_EXCEPTION.create();
        }
    }
}

