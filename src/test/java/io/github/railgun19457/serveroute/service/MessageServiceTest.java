package io.github.railgun19457.serveroute.service;

import io.github.railgun19457.serveroute.model.MessageConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessageServiceTest {
    @Test
    void escapesPlayerInputBeforeMiniMessage() {
        MessageService service = new MessageService();
        service.apply(new MessageConfig(
                "[P] ",
                "",
                "",
                "",
                "",
                "",
                "",
                "<red>找不到服务器 <white>{input}</white>。</red>",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));

        String injected = MessageService.escape("<red>hack");
        String plain = PlainTextComponentSerializer.plainText().serialize(
                service.render(service.messages().serverNotFound(), Map.of("input", injected))
        );
        assertEquals("找不到服务器 <red>hack。", plain);
        assertFalse(plain.contains("hack。") && plain.startsWith("hack"));
    }
}
