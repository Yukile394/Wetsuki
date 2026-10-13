package dev.wetsuki.events.impl.world;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import dev.wetsuki.events.Event;


@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventObsidianPlace extends Event {
    private final Block block;
    private final BlockPos pos;
}
