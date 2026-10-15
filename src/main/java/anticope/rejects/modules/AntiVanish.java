package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AntiVanish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
            .name("interval")
            .description("Vanish check interval.")
            .defaultValue(100)
            .min(0)
            .sliderMax(300)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .defaultValue(Mode.LeaveMessage)
            .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
            .name("command")
            .description("The completion command to detect player names.")
            .defaultValue("minecraft:msg")
            .visible(() -> mode.get() == Mode.RealJoinMessage)
            .build()
    );

    private Map<UUID, String> playerCache = new HashMap<>();
    private final List<String> messageCache = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final Random random = new Random();
    private final List<Integer> completionIDs = new java.util.concurrent.CopyOnWriteArrayList<>();
    private List<String> completionPlayerCache = new ArrayList<>();

    private int timer = 0;

    public AntiVanish() {
        super(MeteorRejectsAddon.CATEGORY, "anti-vanish", "Notifies user when a admin uses /vanish");
    }

    @Override
    public void onActivate() {
        timer = 0;
        completionIDs.clear();
        messageCache.clear();
        playerCache.clear();
        completionPlayerCache = new ArrayList<>();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();
        l.add(theme.label("LeaveMessage: If client didn't receive a quit game message (like essentials)."));
        l.add(theme.label("RealJoinMessage: Tell whether the player is really left by using player name completion."));
        return l;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (mode.get() == Mode.RealJoinMessage && event.packet instanceof ClientboundCommandSuggestionsPacket packet) {
            if (completionIDs.contains(packet.id())) {
                var lastUsernames = completionPlayerCache.stream().toList();

                completionPlayerCache = packet.toSuggestions().getList().stream()
                        .map(Suggestion::getText)
                        .toList();

                if (lastUsernames.isEmpty()) {
                    completionIDs.remove(Integer.valueOf(packet.id()));
                    event.cancel();
                    return;
                }

                String selfName = mc.player.getName().getString();
                Predicate<String> joinedOrQuit = playerName -> lastUsernames.contains(playerName) != completionPlayerCache.contains(playerName);

                for (String playerName : completionPlayerCache) {
                    if (Objects.equals(playerName, selfName)) continue;
                    if (playerName.contains(" ")) continue;
                    if (playerName.length() < 3 || playerName.length() > 16) continue;
                    if (joinedOrQuit.test(playerName)) {
                        info("Player joined: " + playerName);
                    }
                }

                for (String playerName : lastUsernames) {
                    if (Objects.equals(playerName, selfName)) continue;
                    if (playerName.contains(" ")) continue;
                    if (playerName.length() < 3 || playerName.length() > 16) continue;
                    if (joinedOrQuit.test(playerName)) {
                        info("Player left: " + playerName);
                    }
                }

                completionIDs.remove(Integer.valueOf(packet.id()));
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (event.getMessage() != null) {
            messageCache.add(event.getMessage().getString());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getConnection() == null) return;
        timer++;
        if (timer < interval.get()) return;

        switch (mode.get()) {
            case LeaveMessage -> {
                Map<UUID, String> oldPlayers = Map.copyOf(playerCache);
                playerCache = mc.getConnection().getOnlinePlayers().stream()
                        .filter(e -> e.getProfile() != null && e.getProfile().id() != null && e.getProfile().name() != null)
                        .collect(Collectors.toMap(
                                e -> e.getProfile().id(),
                                e -> e.getProfile().name(),
                                (a, b) -> b
                        ));

                for (UUID uuid : oldPlayers.keySet()) {
                    if (playerCache.containsKey(uuid)) continue;
                    String name = oldPlayers.get(uuid);
                    if (name == null || name.contains(" ")) continue;
                    if (name.length() < 3 || name.length() > 16) continue;
                    if (messageCache.stream().noneMatch(s -> s.contains(name))) {
                        warning(name + " has gone into vanish.");
                    }
                }
            }
            case RealJoinMessage -> {
                int id = random.nextInt(200);
                if (completionIDs.size() >= 64) {
                    completionIDs.removeFirst();
                }
                completionIDs.add(id);
                mc.getConnection().send(new ServerboundCommandSuggestionPacket(id, command.get() + " "));
            }
        }
        timer = 0;
        messageCache.clear();
    }

    public enum Mode {
        LeaveMessage,
        RealJoinMessage//https://github.com/xtrm-en/meteor-antistaff/blob/main/src/main/java/me/xtrm/meteorclient/antistaff/modules/AntiStaff.java
    }
}
