package mrquackduck.imageemojis.server.listeners;

import mrquackduck.imageemojis.ImageEmojisPlugin;
import mrquackduck.imageemojis.configuration.Configuration;
import mrquackduck.imageemojis.configuration.Permissions;
import mrquackduck.imageemojis.types.models.EmojiModel;
import mrquackduck.imageemojis.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookEditListener implements Listener {
    private final ImageEmojisPlugin plugin;
    private final Configuration config;

    public BookEditListener(ImageEmojisPlugin plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @EventHandler
    public void onBookEdited(PlayerEditBookEvent event) {
        if (!config.isBookReplacementEnabled()) return;

        Player player = event.getPlayer();
        List<EmojiModel> emojis = plugin.getEmojiRepository().getEmojis();
        BookMeta newBookMeta = event.getNewBookMeta();
        List<Component> pageComponents = newBookMeta.pages();
        List<Component> newPageComponents = new ArrayList<>();

        for (int i = 0; i < pageComponents.size(); i++) {
            Component pageComponent = pageComponents.get(i);

            for (EmojiModel emoji : emojis) {
                if (emoji.getChars().isEmpty()) continue;
                TextComponent replacement = Component.text(emoji.getAsUtf8Symbol());
                if (config.isEmojiHoverEnabled()) replacement = replacement.hoverEvent(HoverEvent.showText(Component.text(emoji.getTemplate()).color(TextColor.color(ColorUtil.hexToColor(config.emojiHoverColor())))));
                if (!player.hasPermission(Permissions.USE)) replacement = Component.empty();

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

                if (player.hasPermission(Permissions.USE)) {
                    pageComponent = pageComponent.replaceText(templateToUtf8ReplacementConfig);
                }
                pageComponent = pageComponent.replaceText(utf8ToUtf8ReplacementConfig);
            }

            newPageComponents.add(pageComponent);
        }


        BookMeta modifiedBookMeta = (BookMeta) newBookMeta.pages(newPageComponents);
        event.setNewBookMeta(modifiedBookMeta);
    }
}
