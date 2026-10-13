package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;
import dev.wetsuki.manager.macroManager.Macro;
import dev.wetsuki.util.KeyMappings;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MacroCommand extends Command {

    public MacroCommand() {
        super("macro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        SuggestionProvider<CommandSource> keySuggestions = (context, builder1) -> {
            for (String key : KeyMappings.getAllKeys()) {
                builder1.suggest(key);
            }
            return builder1.buildFuture();
        };
        SuggestionProvider<CommandSource> macroSuggestions = (context, builder1) -> {
            for (Macro macro : Manager.MACROS_MANAGER.getMacros()) {
                String keyName = KeyMappings.keyMappings(macro.getKey());
                if (keyName != null)
                    builder1.suggest(keyName);
            }
            return builder1.buildFuture();
        };

        RequiredArgumentBuilder<CommandSource, String> addKeyArg = arg("key", StringArgumentType.word()).suggests(keySuggestions);
        RequiredArgumentBuilder<CommandSource, String> addMessageArg = arg("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String keyName = StringArgumentType.getString(ctx, "key").toUpperCase();
                    Integer key = KeyMappings.keyCode(keyName);

                    if (key == null) {
                        ClientManager.message(Formatting.RED + "Key not found " + keyName);
                        return SINGLE_SUCCESS;
                    }

                    String message = StringArgumentType.getString(ctx, "message");
                    Manager.MACROS_MANAGER.addMacros(new Macro(message, key));

                    ClientManager.message(Formatting.GREEN + "Macro added for key "
                            + Formatting.RED + keyName + Formatting.WHITE + " with command " + Formatting.RED + message);
                    return SINGLE_SUCCESS;
                });
        addKeyArg.then(addMessageArg);
        builder.then(literal("add").then(addKeyArg));
        RequiredArgumentBuilder<CommandSource, String> removeKeyArg = arg("key", StringArgumentType.word())
                .suggests(macroSuggestions)
                .executes(ctx -> {
                    String keyName = StringArgumentType.getString(ctx, "key").toUpperCase();
                    Integer key = KeyMappings.keyCode(keyName);

                    if (key == null) {
                        ClientManager.message(Formatting.RED + "Key not found " + keyName);
                        return SINGLE_SUCCESS;
                    }

                    Macro macro = Manager.MACROS_MANAGER.getMacroByKey(key);
                    if (macro == null) {
                        ClientManager.message(Formatting.RED + "Key " + keyName + " has no macro.");
                        return SINGLE_SUCCESS;
                    }

                    Manager.MACROS_MANAGER.deleteMacro(key);
                    ClientManager.message(Formatting.GREEN + "Macro removed from key " + Formatting.RED + keyName);
                    return SINGLE_SUCCESS;
                });
        builder.then(literal("remove").then(removeKeyArg));

        builder.then(literal("list").executes(ctx -> {
            if (Manager.MACROS_MANAGER.getMacros().isEmpty()) {
                ClientManager.message("Macro list is empty");
            } else {
                ClientManager.message(Formatting.GREEN + "Macro list:");
                Manager.MACROS_MANAGER.getMacros().forEach(macro -> {
                    String keyName = KeyMappings.keyMappings(macro.getKey());
                    ClientManager.message(Formatting.WHITE + "Command: " + Formatting.RED + macro.getMessage()
                            + Formatting.WHITE + ", Key: " + Formatting.RED + keyName);
                });
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(ctx -> {
            if (Manager.MACROS_MANAGER.getMacros().isEmpty()) {
                ClientManager.message(Formatting.RED + "Macro list is empty");
            } else {
                Manager.MACROS_MANAGER.getMacros().clear();
                Manager.MACROS_MANAGER.updateFile();
                ClientManager.message(Formatting.GREEN + "Macro list cleared successfully");
            }
            return SINGLE_SUCCESS;
        }));
    }

}
