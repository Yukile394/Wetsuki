package dev.wetsuki.util.color;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import dev.wetsuki.manager.IMinecraft;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.themeManager.StyleManager;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static dev.wetsuki.util.math.MathUtil.interpolateInt;

@SuppressWarnings("All")
public class ColorUtil implements IMinecraft {
    public static final int hud_color = new Color(25, 22, 33, 220).getRGB();
    public static final int hud_color2 = new Color(16, 15, 19, 255).getRGB();

    // NOT: Bu sinif once javax.imageio.ImageIO/java.awt.image.BufferedImage
    // kullaniyordu. ImageIO ilk cagrildiginda dolayli olarak java.awt.Toolkit'i
    // baslatir; bazi Android/PojavLauncher JVM'lerinde bu baslatma islemi
    // hata firlatmak yerine sonsuza kadar donebiliyor. NativeImage,
    // Minecraft'in kendi PNG cozucusu oldugu icin AWT'ye hic dokunmuyor ve
    // bu ortamlarda da guvenle calisiyor.
    private static final Map<Identifier, NativeImage> CACHED_IMAGES = new HashMap<>();
    public static void loadImage(Identifier identifier) {
        if (!CACHED_IMAGES.containsKey(identifier)) {
            try {
                Optional<Resource> resourceOptional = mc.getResourceManager().getResource(identifier);
                if (resourceOptional.isPresent()) {
                    try (InputStream stream = resourceOptional.get().getInputStream()) {
                        CACHED_IMAGES.put(identifier, NativeImage.read(stream));
                    }
                }
            } catch (IOException ignored) {}
        }
    }
    public static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getPixelColor(Identifier id, float pixelX, float pixelY) {
        NativeImage image = CACHED_IMAGES.get(id);
        if (image == null) return 0;
        int x = Math.max(0, Math.min((int) (pixelX * image.getWidth()), image.getWidth() - 1));
        int y = Math.max(0, Math.min((int) (pixelY * image.getHeight()), image.getHeight() - 1));

        // NativeImage stores/returns colors packed as ABGR (a<<24|b<<16|g<<8|r),
        // not the standard Java ARGB that BufferedImage.getRGB used to return.
        // Convert so callers keep getting the format they already expect.
        int abgr = image.getColorArgb(x, y);
        int a = (abgr >>> 24) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);
        return (r << 16) | (g << 8) | b;
    }
    public static int blendColorsInt(int color1, int color2, float ratio) {
        float ir = 1.0f - ratio;
        int a1 = (color1 >> 24) & 0xff;
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;
        int a2 = (color2 >> 24) & 0xff;
        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;
        int a = (int)(a1 * ir + a2 * ratio);
        int r = (int)(r1 * ir + r2 * ratio);
        int g = (int)(g1 * ir + g2 * ratio);
        int b = (int)(b1 * ir + b2 * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static int withAlpha(int rgb, float a) {
        int ai = MathHelper.clamp((int)(a * 255f), 0, 255);
        return (rgb & 0x00FFFFFF) | (ai << 24);
    }
    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }
    public static int getRed(final int hex) {
        return hex >> 16 & 255;
    }
    public static int getGreen(final int hex) {
        return hex >> 8 & 255;
    }
    public static int getBlue(final int hex) {
        return hex & 255;
    }
    public static int getAlpha(final int hex) {
        return hex >> 24 & 255;
    }

    public static int getColorStyle(float index) {
        return getColorHud((int) index);
    }
    public static int getColorStyle(float index, float alpha) {
        return getColorHud((int) index, (int) alpha);
    }

    public static int getColorHud(int index) {
        StyleManager theme = Manager.STYLE_MANAGER;
        Color upColor = new Color(theme.getFirstColor());
        Color downColor = new Color(theme.getSecondColor());
        return ColorUtil.gradient(5, index, upColor.getRGB(),downColor.getRGB());
    }
    public static int getColorHud(int index, int alpha) {
        StyleManager theme = Manager.STYLE_MANAGER;
        Color upColor = new Color(theme.getFirstColor());
        Color downColor = new Color(theme.getSecondColor());
        int gradientColor = ColorUtil.gradient(5, index, upColor.getRGB(), downColor.getRGB());
        int red = (gradientColor >> 16) & 0xFF;
        int green = (gradientColor >> 8) & 0xFF;
        int blue = gradientColor & 0xFF;
        return new Color(red, green, blue, alpha).getRGB();
    }

    public static float[] rgba(final int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360f * colors.length - colorIndex);
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }
    public static int reAlphaInt(final int color, final int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }
    public static int multRed(int color, float percent01) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        g = Math.min(255, Math.round(g / percent01));
        b = Math.min(255, Math.round(b / percent01));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
