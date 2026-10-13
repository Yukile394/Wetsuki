package dev.wetsuki.modules.render;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.render.EventRender3D;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.color.ColorUtil;
import dev.wetsuki.util.render.Render3DUtil;

@FunctionAnnotation(name = "BlockHighLight", desc = "Highlights the block you are looking at", type = Type.Render)
public class BlockHighLight extends Function {

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender3D e)) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;
        if (result.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = result.getBlockPos();
        if (pos == null) return;
        Render3DUtil.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), ColorUtil.getColorStyle(360), 2, true, true);

    }
}
