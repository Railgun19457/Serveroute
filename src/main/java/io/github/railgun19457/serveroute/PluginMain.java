package io.github.railgun19457.serveroute;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.railgun19457.serveroute.command.AdminCommand;
import io.github.railgun19457.serveroute.command.LineCommand;
import io.github.railgun19457.serveroute.command.ServerCommand;
import io.github.railgun19457.serveroute.listener.DisconnectListener;
import io.github.railgun19457.serveroute.listener.InitialServerListener;
import io.github.railgun19457.serveroute.service.LineSwitcher;
import io.github.railgun19457.serveroute.service.MessageService;
import io.github.railgun19457.serveroute.service.ReconnectTracker;
import io.github.railgun19457.serveroute.service.ServerRouter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = PluginMain.PLUGIN_ID,
        name = "Serveroute",
        version = PluginMain.VERSION,
        description = "Replace /server with config-routed internal connect and Transfer, plus entry-line switching.",
        authors = {"Railgun19457"}
)
public final class PluginMain {
    public static final String PLUGIN_ID = "serveroute";
    public static final String VERSION = BuildConstants.VERSION;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public PluginMain(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ConfigManager configManager = new ConfigManager(
                logger,
                dataDirectory,
                name -> proxyServer.getServer(name).isPresent()
        );
        try {
            configManager.initialize();
        } catch (IOException ex) {
            logger.error("[Serveroute][lifecycle] Failed to initialize configuration.", ex);
            return;
        }

        MessageService messageService = new MessageService();
        messageService.apply(configManager.messages());
        ReconnectTracker reconnectTracker = new ReconnectTracker();
        ServerRouter serverRouter = new ServerRouter(proxyServer, configManager, messageService, logger);
        LineSwitcher lineSwitcher = new LineSwitcher(configManager, messageService, reconnectTracker, logger);

        proxyServer.getEventManager().register(this, new InitialServerListener(proxyServer, configManager, reconnectTracker, logger));
        proxyServer.getEventManager().register(this, new DisconnectListener(reconnectTracker));
        registerCommands(configManager, messageService, serverRouter, lineSwitcher);

        logger.info("[Serveroute][lifecycle] Enabled. version={} servers={} lines={}",
                VERSION,
                configManager.runtime().servers().size(),
                configManager.runtime().lines().size());
    }

    private void registerCommands(
            ConfigManager configManager,
            MessageService messageService,
            ServerRouter serverRouter,
            LineSwitcher lineSwitcher
    ) {
        CommandManager commandManager = proxyServer.getCommandManager();
        if (configManager.runtime().replaceServer()) {
            commandManager.unregister("server");
            commandManager.register(
                    commandManager.metaBuilder("server").plugin(this).build(),
                    new ServerCommand(messageService, serverRouter)
            );
        }

        String[] aliases = configManager.runtime().lineAliases().toArray(String[]::new);
        commandManager.register(
                commandManager.metaBuilder("line")
                        .aliases(aliases)
                        .plugin(this)
                        .build(),
                new LineCommand(messageService, lineSwitcher)
        );
        commandManager.register(
                commandManager.metaBuilder("serveroute").plugin(this).build(),
                new AdminCommand(configManager, messageService)
        );
    }
}
