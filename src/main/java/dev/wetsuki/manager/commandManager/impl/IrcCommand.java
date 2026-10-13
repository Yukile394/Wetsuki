package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class IrcCommand extends Command {

    public IrcCommand() {
        super("irc");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {

        RequiredArgumentBuilder<CommandSource, String> ignoreArg =
                RequiredArgumentBuilder.<CommandSource, String>argument("nick", StringArgumentType.word())
                        .executes(context -> {
                            String nick = StringArgumentType.getString(context, "nick");
                            Manager.IRC_MANAGER.ignoreNick(nick);
                            Manager.IRC_MANAGER.messageClient(nick + " added to ignore list");
                            return SINGLE_SUCCESS;
                        });
        builder.then(literal("ignore").then(ignoreArg));

        RequiredArgumentBuilder<CommandSource, String> unignoreArg =
                RequiredArgumentBuilder.<CommandSource, String>argument("nick", StringArgumentType.word())
                        .executes(context -> {
                            String nick = StringArgumentType.getString(context, "nick");
                            Manager.IRC_MANAGER.unignoreNick(nick);
                            Manager.IRC_MANAGER.messageClient(nick + " removed from ignore list");
                            return SINGLE_SUCCESS;
                        });
        builder.then(literal("unignore").then(unignoreArg));

        builder.then(literal("ignorelist")
                .executes(context -> {
                    if (Manager.IRC_MANAGER.getIgnoredNicks().isEmpty()) {
                        Manager.IRC_MANAGER.messageClient("Ignore list is empty");
                    } else {
                        Manager.IRC_MANAGER.messageClient("Ignore list: " + String.join(", ", Manager.IRC_MANAGER.getIgnoredNicks()));
                    }
                    return SINGLE_SUCCESS;
                }));

        RequiredArgumentBuilder<CommandSource, String> msgArg =
                RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            String message = StringArgumentType.getString(context, "message");
                            if (message.matches("(https?://|www\\.)\\S+")) {
                                Manager.IRC_MANAGER.messageClient("Your message contains a link.");
                                return SINGLE_SUCCESS;
                            }
                            if (Manager.FUNCTION_MANAGER.irc.state) {
                                Manager.IRC_MANAGER.messageHost(message);
                            } else {
                                Manager.IRC_MANAGER.messageClient("Please enable the IRC module");
                            }
                            return SINGLE_SUCCESS;
                        });
        builder.then(msgArg);
    }

}
