package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
public class DragCommand extends Command {

    public DragCommand() {
        super("drag");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> root) {
        root.then(literal("reset")
                .executes(ctx -> {
                    Manager.DRAG_MANAGER.reset();
                    ClientManager.message(Formatting.GREEN + "On-screen element positions were reset");
                    return SINGLE_SUCCESS;
                }));

        root.then(literal("save")
                .executes(ctx -> {
                    Manager.DRAG_MANAGER.save();
                    ClientManager.message(Formatting.GREEN + "On-screen elements were saved");
                    return SINGLE_SUCCESS;
                }));

    }
}
