package dev.wetsuki.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector2f;
import dev.wetsuki.manager.fontManager.FontUtils;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.render.EventRender2D;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.color.ColorUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.wetsuki.util.render.RenderUtil.drawTexture;

@FunctionAnnotation(name = "Arrows", desc = "On-screen arrows pointing to players", type = Type.Render)
public class Arrows extends Function {
    private final SliderSetting radius = new SliderSetting("Radius", 70f, 50f, 160f, 1f);
    private final BooleanSetting dynamic = new BooleanSetting("Dynamic", true);
    private float animatedRadius = radius.get().floatValue();
    private final Map<UUID, Float> smoothedAngles = new HashMap<>();

    public Arrows() {
        addSettings(radius, dynamic);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventRender2D render2D)) return;
        var player = mc.player;
        if (player == null) return;

        float targetRadius = radius.get().floatValue();
        if (dynamic.get() && player.isSprinting()) {
            targetRadius += 20f;
        }
        animatedRadius += (targetRadius - animatedRadius) * 0.1f;

        var players = Manager.SYNC_MANAGER.getPlayers();
        for (PlayerEntity other : players) {
            if (other.equals(player) || Manager.FUNCTION_MANAGER.antiBot.check(other)) continue;

            drawArrow(render2D.getMatrixStack(), other, other.getX(), other.getZ(), ColorUtil.getColorStyle(360));
        }
    }

    private void drawArrow(MatrixStack stack, PlayerEntity target, double x, double z, int baseColor) {
        var player = mc.player;
        var window = mc.getWindow();
        int width = window.getScaledWidth();
        int height = window.getScaledHeight();

        float centerX = width / 2f;
        float centerY = height / 2f;

        float desiredAngle = MathHelper.wrapDegrees(getRotationTo(new Vector2f((float) x, (float) z)) - player.getYaw());
        float angle = smoothAngle(target.getUuid(), desiredAngle);

        stack.push();
        stack.translate(centerX, centerY, 0.0F);
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        stack.translate(-centerX, -centerY, 0.0F);

        int color = Manager.FRIEND_MANAGER.isFriend(target.getName().getString())
                ? ColorUtil.rgba(20, 255, 20, 255)
                : Manager.FUNCTION_MANAGER.attackAura.target == target
                ? ColorUtil.rgba(255, 50, 50, 255)
                : baseColor;

        drawTexture(stack, "images/triangle.png", centerX - 7, centerY - animatedRadius, 20, 20, 1, color);
        stack.pop();
    }

    private float smoothAngle(UUID id, float targetAngle) {
        float prev = smoothedAngles.getOrDefault(id, targetAngle);
        float delta = MathHelper.wrapDegrees(targetAngle - prev);
        float factor = 0.08f;
        float result = prev + delta * factor;
        smoothedAngles.put(id, result);
        return result;
    }

    private float getRotationTo(Vector2f vec) {
        var player = mc.player;
        if (player == null) return 0f;

        double dx = vec.x - player.getX();
        double dz = vec.y - player.getZ();

        return (float) -(Math.atan2(dx, dz) * (180.0 / Math.PI));
    }
}