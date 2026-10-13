package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class StaffCommand extends Command {

    public StaffCommand() {
        super("staff");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> root) {
        root.then(literal("add")
                .then(arg("name", StringArgumentType.word())
                        .suggests(this::suggestOnlinePlayers)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            if (Manager.STAFF_MANAGER.getStaffNames().contains(name)) {
                                ClientManager.message(Formatting.RED + "This player is already in the Staff List!");
                            } else {
                                Manager.STAFF_MANAGER.addStaff(name);
                                ClientManager.message(Formatting.GREEN + "Nickname " + Formatting.WHITE + name + Formatting.GREEN + " added to Staff List");
                            }
                            return SINGLE_SUCCESS;
                        })));

        root.then(literal("remove")
                .then(arg("name", StringArgumentType.word())
                        .suggests(this::suggestExistingStaff)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            if (Manager.STAFF_MANAGER.getStaffNames().contains(name)) {
                                Manager.STAFF_MANAGER.removeStaff(name);
                                ClientManager.message(Formatting.GREEN + "Nickname " + Formatting.WHITE + name + Formatting.GREEN + " removed from Staff List");
                            } else {
                                ClientManager.message(Formatting.RED + "This player is not in the Staff List!");
                            }
                            return SINGLE_SUCCESS;
                        })));

        root.then(literal("clear")
                .executes(ctx -> {
                    if (Manager.STAFF_MANAGER.getStaffNames().isEmpty()) {
                        ClientManager.message(Formatting.RED + "Staff List is empty!");
                    } else {
                        Manager.STAFF_MANAGER.clearStaffs();
                        ClientManager.message(Formatting.GREEN + "Staff List cleared");
                    }
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("list")
                .executes(ctx -> {
                    ClientManager.message(Formatting.GRAY + "Staff list:");
                    for (String name : Manager.STAFF_MANAGER.getStaffNames()) {
                        ClientManager.message(Formatting.WHITE + name);
                    }
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("reload")
                .executes(ctx -> {
                    Manager.STAFF_MANAGER.reload();
                    ClientManager.message(Formatting.GREEN + "List was reloaded");
                    return SINGLE_SUCCESS;
                }));
    }

    private CompletableFuture<Suggestions> suggestExistingStaff(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        List<String> staffNames = Manager.STAFF_MANAGER.getStaffNames();
        return CommandSource.suggestMatching(staffNames, builder);
    }

    private CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        if (mc.player.networkHandler == null) return builder.buildFuture();
        List<String> playerNames = mc.player.networkHandler
                .getPlayerList()
                .stream()
                .map(p -> p.getProfile().getName())
                .collect(Collectors.toList());
        return CommandSource.suggestMatching(playerNames, builder);
    }
}
