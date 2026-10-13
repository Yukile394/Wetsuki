package dev.wetsuki.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.render.EventRender3D;
import dev.wetsuki.events.impl.world.EventFog;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.color.ColorUtil;
import dev.wetsuki.util.math.MathUtil;
import dev.wetsuki.util.player.TimerUtil;
import dev.wetsuki.util.render.RenderUtil;
import dev.wetsuki.util.render.providers.ResourceProvider;
import dev.wetsuki.util.vector.VectorUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("All")
@FunctionAnnotation(name = "World", desc = "Lets you change time of day, weather and fog", type = Type.Render)
public class World extends Function {

    private final BooleanSetting timeBox = new BooleanSetting("Change time", true);
    private final ModeSetting timeMode = new ModeSetting(
            timeBox::get,
            "Time of day",
            "Day", "Day", "Night", "Morning", "Sunrise", "Custom"
    );
    private final SliderSetting customTime = new SliderSetting(
            "Custom time", 6000, 0, 24000, 100,
            () -> timeBox.get() && timeMode.is("Custom")
    );

    private final BooleanSetting weatherBox = new BooleanSetting("Change weather", true);
    private final ModeSetting weatherMode = new ModeSetting(
            weatherBox::get,
            "Weather",
            "Clear", "Clear", "Rain", "Thunder"
    );

    public final BooleanSetting fog = new BooleanSetting("Fog", false);
    public final SliderSetting fogEnd = new SliderSetting(
            "Fog distance", 200, 0, 500, 1, fog::get
    );

    public World() {
        addSettings(timeBox, timeMode, customTime, weatherBox, weatherMode, fog, fogEnd);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.world == null) return;
        if (event instanceof EventPacket packet && timeBox.get()) {
            if (packet.getPacket() instanceof WorldTimeUpdateS2CPacket) {
                packet.setCancel(true);
            }
        }

        if (event instanceof EventUpdate) {
            if (timeBox.get()) {
                mc.world.setTime(resolveTime(), resolveTime(), false);
            }

            if (weatherBox.get()) {
                switch (weatherMode.get()) {
                    case "Clear" -> {
                        mc.world.setRainGradient(0f);
                        mc.world.setThunderGradient(0f);
                    }
                    case "Rain" -> {
                        mc.world.setRainGradient(1f);
                        mc.world.setThunderGradient(0f);
                    }
                    case "Thunder" -> {
                        mc.world.setRainGradient(1f);
                        mc.world.setThunderGradient(1f);
                    }
                }
            }
        }

        if (event instanceof EventFog fogEvent && fog.get()) {
            int themeColor = ColorUtil.gradient(15, 360, Manager.STYLE_MANAGER.getFirstColor(), Manager.STYLE_MANAGER.getSecondColor());
            fogEvent.r = ((themeColor >> 16) & 0xFF) / 255.0f;
            fogEvent.g = ((themeColor >> 8) & 0xFF) / 255.0f;
            fogEvent.b = (themeColor & 0xFF) / 255.0f;
            fogEvent.alpha = 1.0f;
            fogEvent.start = 0.0f;
            fogEvent.end = fogEnd.get().floatValue();
            fogEvent.shape = FogShape.SPHERE;
            fogEvent.modified = true;
        }
    }

    private long resolveTime() {
        return switch (timeMode.get()) {
            case "Day" -> 1000L;
            case "Night" -> 13000L;
            case "Morning" -> 0L;
            case "Sunrise" -> 23000L;
            case "Custom" -> (long) customTime.get().floatValue();
            default -> 6000L;
        };
    }
}
