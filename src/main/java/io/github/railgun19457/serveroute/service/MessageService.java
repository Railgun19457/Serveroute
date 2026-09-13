package io.github.railgun19457.serveroute.service;

import io.github.railgun19457.serveroute.model.MessageConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private volatile MessageConfig messages;

    public void apply(MessageConfig messages) {
        this.messages = messages;
    }

    public MessageConfig messages() {
        MessageConfig current = messages;
        if (current == null) {
            throw new IllegalStateException("Message config has not been initialized");
        }
        return current;
    }

    public Component render(String template, Map<String, String> placeholders) {
        return renderTemplate(template, placeholders);
    }

    public Component renderPrefixed(String template, Map<String, String> placeholders) {
        Component body = renderTemplate(template, placeholders);
        Component prefix = renderTemplate(messages().prefix(), Map.of());
        return prefix.append(body);
    }

    public static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return MINI_MESSAGE.escapeTags(input);
    }

    public static String plain(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    private Component renderTemplate(String template, Map<String, String> placeholders) {
        Map<String, String> safe = placeholders == null ? Map.of() : placeholders;
        String content = template == null ? "" : template;

        Map<String, String> replaced = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            replaced.put(entry.getKey(), value);
            content = content.replace("{" + entry.getKey() + "}", value);
        }

        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : replaced.entrySet()) {
            resolvers.add(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }

        try {
            if (resolvers.isEmpty()) {
                return MINI_MESSAGE.deserialize(content);
            }
            return MINI_MESSAGE.deserialize(content, TagResolver.resolver(resolvers));
        } catch (Exception ex) {
            String fallback = content;
            for (Map.Entry<String, String> entry : replaced.entrySet()) {
                fallback = fallback.replace("<" + entry.getKey() + ">", entry.getValue());
            }
            return Component.text(MINI_MESSAGE.stripTags(fallback));
        }
    }
}
