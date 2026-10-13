package dev.wetsuki.manager.fontManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight;

/**
 * Minimal TrueType font wrapper backed by stb_truetype (via LWJGL's lwjgl-stb
 * module, which Minecraft already bundles for its own NativeImage/PNG decoding).
 *
 * This exists as a drop-in replacement for java.awt.Font in the glyph atlas
 * pipeline. On some Android/PojavLauncher JVMs, touching java.awt.Font,
 * Graphics2D, FontMetrics or ImageIO for the first time triggers a lazy
 * java.awt.Toolkit initialization that can hang the JVM forever instead of
 * throwing HeadlessException, because there is no real windowing system for
 * AWT to attach to. stb_truetype only ever reads raw bytes in memory, so it
 * behaves identically on every JVM Minecraft runs on.
 */
public final class TrueTypeFont {
    private final ByteBuffer fontData;
    private final STBTTFontinfo info;
    private final int ascent;
    private final int descent;
    private final int lineGap;

    private TrueTypeFont(ByteBuffer fontData, STBTTFontinfo info, int ascent, int descent, int lineGap) {
        this.fontData = fontData;
        this.info = info;
        this.ascent = ascent;
        this.descent = descent;
        this.lineGap = lineGap;
    }

    /**
     * Reads an entire TrueType font from the given stream and parses it with
     * stb_truetype. The returned instance keeps the raw font bytes alive for
     * as long as it exists, since stb_truetype reads directly from that
     * memory on every glyph lookup.
     */
    public static TrueTypeFont load(InputStream stream) throws IOException {
        byte[] bytes = stream.readAllBytes();

        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!stbtt_InitFont(fontInfo, buffer)) {
            throw new IOException("stb_truetype fontu ayristiramadi (bozuk veya desteklenmeyen .ttf)");
        }

        int ascent, descent, lineGap;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer a = stack.mallocInt(1);
            IntBuffer d = stack.mallocInt(1);
            IntBuffer lg = stack.mallocInt(1);
            stbtt_GetFontVMetrics(fontInfo, a, d, lg);
            ascent = a.get(0);
            descent = d.get(0);
            lineGap = lg.get(0);
        }

        return new TrueTypeFont(buffer, fontInfo, ascent, descent, lineGap);
    }

    STBTTFontinfo info() {
        return info;
    }

    /** Font-unit ascent (positive), not yet scaled to a pixel size. */
    public int ascent() {
        return ascent;
    }

    /** Font-unit descent (negative or zero), not yet scaled to a pixel size. */
    public int descent() {
        return descent;
    }

    public int lineGap() {
        return lineGap;
    }

    /** Scale factor to convert font units to pixels for the given pixel height. */
    public float scaleForPixelHeight(float sizePx) {
        return stbtt_ScaleForPixelHeight(info, sizePx);
    }
}
