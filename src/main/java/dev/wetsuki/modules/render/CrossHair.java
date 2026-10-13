package dev.wetsuki.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.EntityHitResult;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.color.ColorUtil;
import dev.wetsuki.util.math.MathUtil;
import dev.wetsuki.util.render.RenderUtil;

import java.awt.*;

@FunctionAnnotation(name = "CrossHair", type = Type.Render)
public class CrossHair extends Function {

    private final SliderSetting attackSetting = new SliderSetting("Size on hit",6,0,20,1);
    private final SliderSetting indentSetting = new SliderSetting("Gap",2,0,5,1);
    private final SliderSetting size1Setting = new SliderSetting("Line length",6,2,10,1);
    private final SliderSetting size2Setting = new SliderSetting("Line thickness",2,2,4,1);

    public CrossHair() {
        addSettings(attackSetting,indentSetting,size1Setting,size2Setting);
    }
    @Override
    public void onEvent(Event event) {

    }


    private float red = 0;
    public void render(DrawContext drawContext) {
        red = MathUtil.interpolateSmooth(2, red, mc.crosshairTarget instanceof EntityHitResult ? 5 : 1);
        int firstColor = ColorUtil.multRed(Color.WHITE.getRGB(), red), secondColor = Color.BLACK.getRGB();
        float x = mc.getWindow().getScaledWidth() / 2F, y = mc.getWindow().getScaledHeight() / 2F;
        float cooldown = attackSetting.get().intValue() - (attackSetting.get().intValue() * mc.player.getAttackCooldownProgress(mc.getRenderTickCounter().getTickDelta(true)));
        float size = size1Setting.get().floatValue(), size2 = size2Setting.get().floatValue(), offset = size2 / 2, indent = indentSetting.get().intValue() + cooldown;

        renderMain(drawContext,x, y, size, size2, 1, indent, offset, secondColor);
        renderMain(drawContext,x, y, size, size2, 0, indent, offset, firstColor);
    }

    private void renderMain(DrawContext drawContext, float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
        RenderUtil.drawRoundedRect(drawContext.getMatrices(),x - offset - padding / 2, y - size - indent - padding / 2, size2 + padding, size + padding,0, color);
        RenderUtil.drawRoundedRect(drawContext.getMatrices(),x - offset - padding / 2, y + indent - padding / 2, size2 + padding, size + padding,0, color);
        RenderUtil.drawRoundedRect(drawContext.getMatrices(),x - size - indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding,0, color);
        RenderUtil.drawRoundedRect(drawContext.getMatrices(),x + indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding,0, color);
    }
}