package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import java.io.IOException;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("cfg");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> root) {
        root.then(literal("save")
                .then(arg("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Manager.CONFIG_MANAGER.getAllConfigurations()
                                    .forEach(s -> builder.suggest(s.replace(".json", "")));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name").toUpperCase();
                            Manager.CONFIG_MANAGER.saveConfiguration(name);
                            ClientManager.message("Config named " + name + " saved");
                            return SINGLE_SUCCESS;
                        })));

        root.then(literal("load")
                .then(arg("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Manager.CONFIG_MANAGER.getAllConfigurations()
                                    .forEach(s -> builder.suggest(s.replace(".json", "")));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name").toUpperCase();
                            Manager.CONFIG_MANAGER.loadConfiguration(name, false);
                            return SINGLE_SUCCESS;
                        })));

        root.then(literal("remove")
                .then(arg("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Manager.CONFIG_MANAGER.getAllConfigurations()
                                    .forEach(s -> builder.suggest(s.replace(".json", "")));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name").toUpperCase();
                            Manager.CONFIG_MANAGER.deleteConfig(name);
                            ClientManager.message("Config " + name + " deleted");
                            return SINGLE_SUCCESS;
                        })));

        root.then(literal("list")
                .executes(ctx -> {
                    List<String> configs = Manager.CONFIG_MANAGER.getAllConfigurations();
                    if (configs.isEmpty()) {
                        ClientManager.message("Config list is empty");
                    } else {
                        ClientManager.message("Config list:");
                        configs.forEach(s -> ClientManager.message(s.replace(".json", "")));
                    }
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("reset")
                .executes(ctx -> {
                    Manager.FUNCTION_MANAGER.getFunctions().stream()
                            .filter(f -> f.state)
                            .forEach(f -> f.setState(false));
                    ClientManager.message("Config reset");
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("dir")
                .executes(ctx -> {
                    try {
                        Runtime.getRuntime().exec("explorer " + Manager.CONFIG_MANAGER.CONFIG_DIR.getAbsolutePath());
                    } catch (IOException e) {
                        ClientManager.message("Error opening folder: " + e.getMessage());
                    }
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("clear")
                .executes(ctx -> {
                    List<String> configs = Manager.CONFIG_MANAGER.getAllConfigurations();
                    if (configs.isEmpty()) {
                        ClientManager.message("Config list is empty");
                        return SINGLE_SUCCESS;
                    }
                    configs.forEach(Manager.CONFIG_MANAGER::deleteConfig);
                    ClientManager.message("All configs deleted");
                    return SINGLE_SUCCESS;
                }));
    }
}
