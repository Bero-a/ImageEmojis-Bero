package mrquackduck.imageemojis.server.listeners;

import mrquackduck.imageemojis.ImageEmojisPlugin;
import mrquackduck.imageemojis.configuration.Configuration;
import mrquackduck.imageemojis.types.models.EmojiModel;
import mrquackduck.imageemojis.utils.ColorUtil;
import mrquackduck.imageemojis.utils.TextComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.BroadcastMessageEvent;

import java.util.List;

public class BlueMapWebChatListener implements Listener {
    private final ImageEmojisPlugin plugin;
    private final Configuration config;

    public BlueMapWebChatListener(ImageEmojisPlugin plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onWebChat(BroadcastMessageEvent event) {
        List<EmojiModel> emojis = plugin.getEmojiRepository().getEmojis();
        TextComponent messageComponent = (TextComponent)event.message();

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

            messageComponent = (TextComponent) messageComponent.replaceText(templateToUtf8ReplacementConfig);
            messageComponent = (TextComponent) messageComponent.replaceText(utf8ToUtf8ReplacementConfig);
        }

        messageComponent = TextComponentUtil.trim(messageComponent);

        event.message(messageComponent);
        if (TextComponentUtil.getFullContent(messageComponent).trim().isEmpty()) event.setCancelled(true);
    }
}
