package dev.wetsuki.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.Command;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class BlockESPCommand extends Command {

    private static final Map<String, Color> COLOR_NAMES = Map.ofEntries(
            Map.entry("white", Color.WHITE),
            Map.entry("black", Color.BLACK),
            Map.entry("red", Color.RED),
            Map.entry("green", Color.GREEN),
            Map.entry("blue", Color.BLUE),
            Map.entry("yellow", Color.YELLOW),
            Map.entry("cyan", Color.CYAN),
            Map.entry("magenta", Color.MAGENTA),
            Map.entry("gray", Color.GRAY),
            Map.entry("darkgray", Color.DARK_GRAY),
            Map.entry("lightgray", Color.LIGHT_GRAY),
            Map.entry("orange", Color.ORANGE),
            Map.entry("pink", Color.PINK)
    );

    public BlockESPCommand() {
        super("blockesp");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> root) {

        root.then(literal("add")
                .then(arg("block", StringArgumentType.word())
                        .suggests(this::suggestBlocks)
                        .then(arg("color", StringArgumentType.word())
                                .suggests(this::suggestColors)
                                .executes(ctx -> addBlock(ctx)))));

        root.then(literal("remove")
                .then(arg("block", StringArgumentType.word())
                        .suggests(this::suggestBlocks)
                        .executes(ctx -> removeBlock(ctx))));
    }

    private int addBlock(CommandContext<CommandSource> ctx) {
        String blockName = StringArgumentType.getString(ctx, "block");
        String colorStr = StringArgumentType.getString(ctx, "color").toLowerCase();

        if (!blockName.contains(":")) blockName = "minecraft:" + blockName;

        Color color = COLOR_NAMES.get(colorStr);
        if (color == null) {
            try {
                if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                color = Color.decode(colorStr);
            } catch (NumberFormatException ex) {
                ClientManager.message(Formatting.RED + "Invalid color. Use a color name or HEX (e.g. ff00ff)");
                return SINGLE_SUCCESS;
            }
        }

        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 150);
        Manager.FUNCTION_MANAGER.blockESP.addCustomBlock(blockName, color);
        ClientManager.message(Formatting.GREEN + "Added block " + blockName + " with color " + colorStr);
        return SINGLE_SUCCESS;
    }

    private int removeBlock(CommandContext<CommandSource> ctx) {
        String blockName = StringArgumentType.getString(ctx, "block");
        if (!blockName.contains(":")) blockName = "minecraft:" + blockName;

        Manager.FUNCTION_MANAGER.blockESP.removeCustomBlock(blockName);
        ClientManager.message(Formatting.GREEN + "Removed block " + blockName + " from highlight");
        return SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSource> ctx, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(COLOR_NAMES.keySet(), builder);
    }

    private CompletableFuture<Suggestions> suggestBlocks(CommandContext<CommandSource> ctx, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(
                Registries.BLOCK.stream().map(b -> Registries.BLOCK.getId(b).getPath()).toList(),
                builder
        );
    }
}
