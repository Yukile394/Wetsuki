package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PanicCommand extends Command {

    public PanicCommand() {
        super("panic");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Manager.FUNCTION_MANAGER.getFunctions().stream().filter(function -> function.state).forEach(function -> function.setState(false));
            ClientManager.message("Disabled all modules!");
            return SINGLE_SUCCESS;
        });
    }
}
