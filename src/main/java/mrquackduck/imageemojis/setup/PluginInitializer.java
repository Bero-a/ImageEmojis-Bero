package mrquackduck.imageemojis.setup;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.tchristofferson.configupdater.ConfigUpdater;
import mrquackduck.imageemojis.ImageEmojisPlugin;
import mrquackduck.imageemojis.configuration.Configuration;
import mrquackduck.imageemojis.services.implementations.EmojiRepository;
import mrquackduck.imageemojis.services.implementations.EmojiResourcePackGenerator;
import mrquackduck.imageemojis.types.models.EmojiModel;
import mrquackduck.imageemojis.utils.ColorUtil;
import mrquackduck.imageemojis.utils.TextComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginInitializer {
    private final ImageEmojisPlugin plugin;

    public PluginInitializer(ImageEmojisPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.saveDefaultConfig();

        // Update the config with missing key-pairs (and remove redundant ones if present)
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try { ConfigUpdater.update(plugin, "config.yml", configFile, new ArrayList<>()); }
        catch (IOException e) { e.printStackTrace(); }

        createEmojisFolderIfNotExists();

        plugin.reloadConfig();
        saveDefaultPackPng();

        plugin.setEmojiRepository(new EmojiRepository(plugin));
        EmojiResourcePackGenerator generator = new EmojiResourcePackGenerator(plugin);
        plugin.setResourcePack(generator.generate());

        addPacketListener();
    }

    private void createEmojisFolderIfNotExists() {
        File emojiFolder = new File(plugin.getDataFolder(), "resized_emojis");
        if (!emojiFolder.exists()) {
            if (emojiFolder.mkdirs()) plugin.getLogger().info("Created emojis folder.");
            else plugin.getLogger().warning("Failed to create emojis folder.");
        }
    }

    private void saveDefaultPackPng() {
        File pack = new File(plugin.getDataFolder(), "pack.png");
        if (!pack.exists()) {
            plugin.saveResource("pack.png", false);
        }
    }

    /**
     * {@link org.bukkit.command.CommandSender#sendMessage(String)} 을 처리합니다.
     */
    private void addPacketListener() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        List<EmojiModel> emojis = plugin.getEmojiRepository().getEmojis();
        Configuration config = new Configuration(plugin);

        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                Player player = event.getPlayer();

                WrappedChatComponent chatComponent = packet.getChatComponents().read(0);
                if (chatComponent == null) return;
                String jsonMessage = chatComponent.getJson();
                Component originalComponent = GsonComponentSerializer.gson().deserialize(jsonMessage);

                for (EmojiModel emoji : emojis) {
                    if (emoji.getChars().isEmpty()) continue;
                    TextComponent replacement = Component.text(emoji.getAsUtf8Symbol());
                    if (config.isEmojiHoverEnabled()) replacement = replacement.hoverEvent(HoverEvent.showText(Component.text(emoji.getTemplate()).color(TextColor.color(ColorUtil.hexToColor(config.emojiHoverColor())))));

                    // The replacement config to replace the emoji template to an actual emoji
                    TextReplacementConfig templateToUtf8ReplacementConfig = TextReplacementConfig.builder()
                            .match(emoji.getTemplate())
                            .replacement(replacement)
                            .build();

                    // The replacement config to apply the hover effect on UTF-8 symbols (if are present in the chat)
                    TextReplacementConfig utf8ToUtf8ReplacementConfig = TextReplacementConfig.builder()
                            .match(emoji.getAsUtf8Symbol())
                            .replacement(replacement)
                            .build();

                    originalComponent = originalComponent.replaceText(templateToUtf8ReplacementConfig);
                    originalComponent = originalComponent.replaceText(utf8ToUtf8ReplacementConfig);
                }

                Component modifiedComponent = originalComponent;
                String newJson = GsonComponentSerializer.gson().serialize(modifiedComponent);

                packet.getChatComponents().write(0, WrappedChatComponent.fromJson(newJson));
            }
        });
    }
}
