package dev.wetsuki.manager.fontManager;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import org.lwjgl.system.MemoryStack;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import dev.wetsuki.manager.IMinecraft;
import dev.wetsuki.util.render.RenderUtil;

import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_FreeBitmap;

@SuppressWarnings("All")
public class RenderFonts implements Closeable, IMinecraft {
    private static final ExecutorService ASYNC_WORKER = Executors.newSingleThreadExecutor();
    private static final float SCALE_FACTOR = 0.5f;
    private static final float INV_SCALE_FACTOR = 2.0f;
    private static final int COLOR_MASK_ALPHA = 0xFF000000;
    private static final int COLOR_MASK_RED = 0x00FF0000;
    private static final int COLOR_MASK_GREEN = 0x0000FF00;
    private static final int COLOR_MASK_BLUE = 0x000000FF;
    private static final int COLOR_SHIFT_ALPHA = 24;
    private static final int COLOR_SHIFT_RED = 16;
    private static final int COLOR_SHIFT_GREEN = 8;
    private static final int COLOR_SHIFT_BLUE = 0;
    private static final float COLOR_DIVISOR = 255f;
    private static final float INV_COLOR_DIVISOR = 1f / 255f;

    private final Map<Identifier, ObjectList<DrawEntry>> GLYPH_PAGE_CACHE = new ConcurrentHashMap<>();
    private final List<GlyphMap> maps = new ArrayList<>();
    private final Char2ObjectMap<Glyph> allGlyphs = new Char2ObjectArrayMap<>();
    private final int charsPerPage;
    private final int padding;
    private final String prebakeGlyphs;
    private final float originalSize;

    private float cachedMaxHeight = 0f;
    private boolean heightDirty = true;

    private final Map<String, Float> widthCache = new ConcurrentHashMap<>();
    private static final int MAX_WIDTH_CACHE_SIZE = 1000;

    private final TrueTypeFont font;
    private final float scale;
    private Future<Void> prebakeGlyphsFuture;


    private static final Char2ObjectMap<float[]> COLOR_CODES_FLOAT = new Char2ObjectArrayMap<>();
    static {
        COLOR_CODES_FLOAT.put('0', new float[]{0f, 0f, 0f});
        COLOR_CODES_FLOAT.put('1', new float[]{0f, 0f, 0.6666667f});
        COLOR_CODES_FLOAT.put('2', new float[]{0f, 0.6666667f, 0f});
        COLOR_CODES_FLOAT.put('3', new float[]{0f, 0.6666667f, 0.6666667f});
        COLOR_CODES_FLOAT.put('4', new float[]{0.6666667f, 0f, 0f});
        COLOR_CODES_FLOAT.put('5', new float[]{0.6666667f, 0f, 0.6666667f});
        COLOR_CODES_FLOAT.put('6', new float[]{1f, 0.6666667f, 0f});
        COLOR_CODES_FLOAT.put('7', new float[]{0.6666667f, 0.6666667f, 0.6666667f});
        COLOR_CODES_FLOAT.put('8', new float[]{0.33333334f, 0.33333334f, 0.33333334f});
        COLOR_CODES_FLOAT.put('9', new float[]{0.33333334f, 0.33333334f, 1f});
        COLOR_CODES_FLOAT.put('a', new float[]{0.33333334f, 1f, 0.33333334f});
        COLOR_CODES_FLOAT.put('b', new float[]{0.33333334f, 1f, 1f});
        COLOR_CODES_FLOAT.put('c', new float[]{1f, 0.33333334f, 0.33333334f});
        COLOR_CODES_FLOAT.put('d', new float[]{1f, 0.33333334f, 1f});
        COLOR_CODES_FLOAT.put('e', new float[]{1f, 1f, 0.33333334f});
        COLOR_CODES_FLOAT.put('f', new float[]{1f, 1f, 1f});
    }


    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL = ThreadLocal.withInitial(StringBuilder::new);
    private static final ThreadLocal<char[]> CHAR_ARRAY_BUFFER = ThreadLocal.withInitial(() -> new char[1024]);

    public RenderFonts(TrueTypeFont font, float sizePx, int charsPerPage, int padding, String prebakeGlyphs) {
        this.originalSize = sizePx;
        this.charsPerPage = charsPerPage;
        this.padding = padding;
        this.prebakeGlyphs = prebakeGlyphs;
        this.font = font;
        this.scale = font.scaleForPixelHeight(sizePx);

        if (prebakeGlyphs != null && !prebakeGlyphs.isEmpty()) {
            prebakeGlyphsFuture = ASYNC_WORKER.submit(() -> {
                for (char c : prebakeGlyphs.toCharArray()) {
                    if (Thread.interrupted()) break;
                    locateGlyph(c);
                }
                return null;
            });
        }
    }

    public RenderFonts(TrueTypeFont font, float sizePx) {
        this(font, sizePx, 256, 5, null);
    }

    public void drawLeftAligned(MatrixStack ms, String text, float x, float y, int color) {
        render(ms, text, x, y, color, false);
    }

    public void drawRightAligned(MatrixStack ms, String text, float x, float y, int color) {
        float width = getWidth(text);
        render(ms, text, x - width, y, color, false);
    }

    public void centeredDraw(MatrixStack ms, String text, float x, float y, int color) {
        float width = getWidth(text);
        render(ms, text, x - width * 0.5f, y, color, false);
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0f;

        Float cached = widthCache.get(text);
        if (cached != null) return cached;

        float width = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if ((c == '§' || c == '&') && i + 1 < len) {
                i++;
                continue;
            }
            Glyph glyph = locateGlyph(c);
            if (glyph != null) width += glyph.width() * SCALE_FACTOR;
        }

        if (widthCache.size() < MAX_WIDTH_CACHE_SIZE) {
            widthCache.put(text, width);
        }

        return width;
    }

    public float getHeight() {
        if (heightDirty) {
            float max = 0;
            for (Glyph glyph : allGlyphs.values()) {
                if (glyph != null) {
                    float h = glyph.height();
                    if (h > max) max = h;
                }
            }
            cachedMaxHeight = max * SCALE_FACTOR;
            heightDirty = false;
        }
        return cachedMaxHeight;
    }

    public void drawClipped(MatrixStack ms, String text, float maxWidth, float x, float y, int color) {
        if (text == null || text.isEmpty()) return;

        StringBuilder clipped = STRING_BUILDER_POOL.get();
        clipped.setLength(0);

        float width = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if ((c == '§' || c == '&') && i + 1 < len) {
                i++;
                continue;
            }

            Glyph glyph = locateGlyph(c);
            if (glyph == null) continue;

            float glyphWidth = glyph.width() * SCALE_FACTOR;
            if (width + glyphWidth > maxWidth) break;

            clipped.append(c);
            width += glyphWidth;
        }
        render(ms, clipped.toString(), x, y, color, false);
    }
    public List<String> splitTextToLines(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            if (currentLine.length() > 0) {
                String testLine = currentLine + " " + word;
                if (getWidth(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            } else {
                if (getWidth(word) <= maxWidth) {
                    currentLine.append(word);
                } else {
                    StringBuilder part = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (getWidth(part.toString() + c) <= maxWidth) {
                            part.append(c);
                        } else {
                            lines.add(part.toString());
                            part = new StringBuilder("" + c);
                        }
                    }
                    currentLine = part;
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
    private void render(MatrixStack ms, String text, float x, float y, int color, boolean isGradient) {
        if (text == null || text.isEmpty()) return;

        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone()) {
            try {
                prebakeGlyphsFuture.get();
            } catch (Exception ignored) {}
        }

        float a = ((color >>> COLOR_SHIFT_ALPHA) & 0xFF) * INV_COLOR_DIVISOR;
        float r = ((color >>> COLOR_SHIFT_RED) & 0xFF) * INV_COLOR_DIVISOR;
        float g = ((color >>> COLOR_SHIFT_GREEN) & 0xFF) * INV_COLOR_DIVISOR;
        float b = (color & 0xFF) * INV_COLOR_DIVISOR;

        RenderSystem.setShaderColor(r, g, b, a);
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(SCALE_FACTOR, SCALE_FACTOR, 1);

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix4f = ms.peek().getPositionMatrix();

        Map<Identifier, List<DrawEntry>> localCache = new Object2ObjectOpenHashMap<>();
        GLYPH_PAGE_CACHE.forEach((key, value) -> localCache.put(key, new ObjectArrayList<>(value)));
        GLYPH_PAGE_CACHE.clear();

        float cursorX = 0;
        char[] chars = getCharArray(text);
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (!isGradient && (c == '§' || c == '&') && i + 1 < len) {
                char code = Character.toLowerCase(chars[++i]);
                float[] col = COLOR_CODES_FLOAT.get(code);
                if (col != null) {
                    r = col[0];
                    g = col[1];
                    b = col[2];
                }
                continue;
            }
            Glyph glyph = locateGlyph(c);
            if (glyph != null) {
                Identifier tex = glyph.owner().bindToTexture;
                localCache.computeIfAbsent(tex, k -> new ObjectArrayList<>()).add(new DrawEntry(cursorX, 0, r, g, b, glyph));
                cursorX += glyph.width();
            }
        }

        Tessellator tessellator = IMinecraft.tessellator();
        localCache.forEach((identifier, entries) -> {
            RenderSystem.setShaderTexture(0, identifier);
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (DrawEntry entry : entries) {
                Glyph g2 = entry.toDraw;
                GlyphMap gm = g2.owner();
                float w = g2.width();
                float h = g2.height();
                float u1 = g2.u() * gm.invWidth;
                float v1 = g2.v() * gm.invHeight;
                float u2 = (g2.u() + w) * gm.invWidth;
                float v2 = (g2.v() + h) * gm.invHeight;

                bufferBuilder.vertex(matrix4f, entry.atX, entry.atY + h, 0).texture(u1, v2).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX + w, entry.atY + h, 0).texture(u2, v2).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX + w, entry.atY, 0).texture(u2, v1).color(entry.r, entry.g, entry.b, a);
                bufferBuilder.vertex(matrix4f, entry.atX, entry.atY, 0).texture(u1, v1).color(entry.r, entry.g, entry.b, a);
            }
            RenderUtil.render3D.endBuilding(bufferBuilder);
        });

        ms.pop();
        RenderUtil.disableRender();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void renderGradientText(MatrixStack matrixStack, String text, float x, float y, int color1, int color2) {
        renderGradientInternal(matrixStack, text, x, y, color1, color2, 0f, false);
    }

    public void renderAnimatedGradientText(MatrixStack matrixStack, String text, float x, float y, int color1, int color2, float time) {
        renderGradientInternal(matrixStack, text, x, y, color1, color2, time, true);
    }

    private void renderGradientInternal(MatrixStack matrixStack, String text, float x, float y,
                                        int color1, int color2, float time, boolean animated) {
        if (text == null || text.isEmpty()) return;

        int length = text.length();
        if (length == 0) return;
        final float rStart = ((color1 >>> COLOR_SHIFT_RED) & 0xFF) * INV_COLOR_DIVISOR;
        final float gStart = ((color1 >>> COLOR_SHIFT_GREEN) & 0xFF) * INV_COLOR_DIVISOR;
        final float bStart = (color1 & 0xFF) * INV_COLOR_DIVISOR;
        final float aStart = ((color1 >>> COLOR_SHIFT_ALPHA) & 0xFF) * INV_COLOR_DIVISOR;

        final float rEnd = ((color2 >>> COLOR_SHIFT_RED) & 0xFF) * INV_COLOR_DIVISOR;
        final float gEnd = ((color2 >>> COLOR_SHIFT_GREEN) & 0xFF) * INV_COLOR_DIVISOR;
        final float bEnd = (color2 & 0xFF) * INV_COLOR_DIVISOR;
        final float aEnd = ((color2 >>> COLOR_SHIFT_ALPHA) & 0xFF) * INV_COLOR_DIVISOR;

        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone()) {
            try {
                prebakeGlyphsFuture.get();
            } catch (Exception ignored) {}
        }

        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale(SCALE_FACTOR, SCALE_FACTOR, 1);

        RenderUtil.enableRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        Map<Identifier, List<GradientDrawEntry>> textureMap = new Object2ObjectOpenHashMap<>();

        float cursorX = 0;
        final float invLength = length == 1 ? 0 : 1f / (length - 1);

        char[] chars = getCharArray(text);
        for (int i = 0; i < length; i++) {
            char c = chars[i];

            float t = i * invLength;
            if (animated) {
                float tRaw = t + (time % 1.0f);
                t = tRaw % 1.0f;
                t = t < 0.5f ? t * 2f : (1f - t) * 2f;
            }

            final float r = rStart + (rEnd - rStart) * t;
            final float g = gStart + (gEnd - gStart) * t;
            final float b = bStart + (bEnd - bStart) * t;
            final float a = aStart + (aEnd - aStart) * t;

            Glyph glyph = locateGlyph(c);
            if (glyph == null) continue;

            textureMap.computeIfAbsent(glyph.owner().bindToTexture, k -> new ObjectArrayList<>()).add(new GradientDrawEntry(cursorX, r, g, b, a, glyph));
            cursorX += glyph.width();
        }

        // Render the batched glyph quads
        Tessellator tessellator = IMinecraft.tessellator();
        textureMap.forEach((texture, entries) -> {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (GradientDrawEntry entry : entries) {
                Glyph glyph = entry.glyph;
                float w = glyph.width();
                float h = glyph.height();
                float u1 = glyph.u() * glyph.owner().invWidth;
                float v1 = glyph.v() * glyph.owner().invHeight;
                float u2 = (glyph.u() + w) * glyph.owner().invWidth;
                float v2 = (glyph.v() + h) * glyph.owner().invHeight;

                bufferBuilder.vertex(matrix4f, entry.x, h, 0).texture(u1, v2).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x + w, h, 0).texture(u2, v2).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x + w, 0, 0).texture(u2, v1).color(entry.r, entry.g, entry.b, entry.a);
                bufferBuilder.vertex(matrix4f, entry.x, 0, 0).texture(u1, v1).color(entry.r, entry.g, entry.b, entry.a);
            }
            RenderUtil.render3D.endBuilding(bufferBuilder);
        });

        matrixStack.pop();
        RenderUtil.disableRender();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Nullable
    private Glyph locateGlyph(char glyphChar) {
        Glyph existing = allGlyphs.get(glyphChar);
        if (existing != null) return existing;

        Glyph newGlyph = createGlyph(glyphChar);
        if (newGlyph != null) {
            allGlyphs.put(glyphChar, newGlyph);
            heightDirty = true;
        }
        return newGlyph;
    }

    @Nullable
    private Glyph createGlyph(char glyphChar) {
        for (GlyphMap map : maps) {
            if (map.contains(glyphChar)) return map.getGlyph(glyphChar);
        }

        int base = charsPerPage * (glyphChar / charsPerPage);
        String id = generateRandomId(16);
        GlyphMap newMap = new GlyphMap((char) base, (char) (base + charsPerPage), font, scale,
                Identifier.of("font", "temp/" + id), padding);
        maps.add(newMap);
        return newMap.getGlyph(glyphChar);
    }

    private char[] getCharArray(String text) {
        char[] buffer = CHAR_ARRAY_BUFFER.get();
        int len = text.length();
        if (len > buffer.length) {
            buffer = new char[len];
            CHAR_ARRAY_BUFFER.set(buffer);
        }
        text.getChars(0, len, buffer, 0);
        return buffer;
    }

    private static String generateRandomId(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private record Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {}

    private record DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {}

    private record GradientDrawEntry(float x, float r, float g, float b, float a, Glyph glyph) {}

    @Override
    public void close() {
        try {
            if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone() && !prebakeGlyphsFuture.isCancelled()) {
                prebakeGlyphsFuture.cancel(true);
                prebakeGlyphsFuture.get();
                prebakeGlyphsFuture = null;
            }
            maps.forEach(GlyphMap::destroy);
            maps.clear();
            allGlyphs.clear();
            GLYPH_PAGE_CACHE.clear();
            widthCache.clear();
        } catch (Exception ignored) {}
    }

    private class GlyphMap {
        private final Char2ObjectMap<Glyph> glyphs = new Char2ObjectArrayMap<>();
        private final TrueTypeFont font;
        private final float scale;
        private final Identifier bindToTexture;
        private final char fromIncl, toExcl;
        private final int pixelPadding;
        private int width, height;
        private float invWidth, invHeight;
        private boolean generated = false;

        GlyphMap(char from, char to, TrueTypeFont font, float scale, Identifier id, int padding) {
            this.fromIncl = from;
            this.toExcl = to;
            this.font = font;
            this.scale = scale;
            this.bindToTexture = id;
            this.pixelPadding = padding;
        }

        Glyph getGlyph(char c) {
            if (!generated) generate();
            return glyphs.get(c);
        }

        boolean contains(char c) {
            return c >= fromIncl && c < toExcl;
        }

        void destroy() {
            mc.getTextureManager().destroyTexture(bindToTexture);
            glyphs.clear();
            width = -1;
            height = -1;
            generated = false;
        }

        // NOT: Bu metod artik hicbir java.awt sinifina dokunmuyor (Graphics2D,
        // BufferedImage, FontMetrics, Toolkit vb.). Glyph rasterizasyonu
        // tamamen stb_truetype (LWJGL'in zaten Minecraft ile birlikte gelen
        // lwjgl-stb modulu) uzerinden yapiliyor. Bunun sebebi: bazi
        // Android/PojavLauncher JVM'lerinde java.awt.Toolkit'in ilk kez
        // dokunulmasi (Font.createFont, Graphics2D, ImageIO, FontMetrics
        // uzerinden dolayli olarak bile olsa) hata firlatmak yerine JVM'i
        // sonsuza kadar dondurebiliyor - cunku bu ortamlarda gercek bir
        // pencere sistemi yok. stb_truetype ham byte'lar uzerinde calisir,
        // hicbir native pencere/goruntu altyapisina ihtiyac duymaz, bu yuzden
        // Minecraft'in calistigi her yerde ayni sekilde calisir.
        private void generate() {
            if (generated) return;

            int range = toExcl - fromIncl;
            int charsPerRow = (int) Math.ceil(Math.sqrt(range));
            int charsPerCol = (int) Math.ceil((double) range / charsPerRow);

            int scaledAscent = Math.max(1, Math.round(font.ascent() * scale));
            int scaledDescent = Math.round(font.descent() * scale);
            int lineHeight = Math.max(1, scaledAscent - scaledDescent);
            int fallbackSize = lineHeight;

            int maxCharWidth = 0;
            CharMetrics[] metrics = new CharMetrics[range];

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer advanceWidth = stack.mallocInt(1);
                IntBuffer leftSideBearing = stack.mallocInt(1);

                for (int i = 0; i < range; i++) {
                    char c = (char) (fromIncl + i);
                    int w;
                    try {
                        stbtt_GetCodepointHMetrics(font.info(), c, advanceWidth, leftSideBearing);
                        w = Math.round(advanceWidth.get(0) * scale);
                    } catch (Throwable t) {
                        w = fallbackSize;
                    }
                    if (w <= 0) w = fallbackSize;

                    maxCharWidth = Math.max(maxCharWidth, w);
                    metrics[i] = new CharMetrics(c, w, lineHeight);
                }
            }

            this.width = Math.max((maxCharWidth + pixelPadding) * charsPerRow + pixelPadding, 1);
            this.height = Math.max((lineHeight + pixelPadding) * charsPerCol + pixelPadding, 1);
            this.invWidth = 1f / width;
            this.invHeight = 1f / height;

            try (NativeImage atlas = new NativeImage(NativeImage.Format.RGBA, width, height, false)) {
                for (int py = 0; py < height; py++) {
                    for (int px = 0; px < width; px++) {
                        atlas.setColorArgb(px, py, 0);
                    }
                }

                for (int i = 0; i < metrics.length; i++) {
                    CharMetrics cm = metrics[i];
                    int row = i / charsPerRow;
                    int col = i % charsPerRow;

                    int cellX = col * (maxCharWidth + pixelPadding) + pixelPadding;
                    int cellY = row * (lineHeight + pixelPadding) + pixelPadding;

                    glyphs.put(cm.character, new Glyph(cellX, cellY, cm.width, cm.height, cm.character, this));

                    try {
                        drawGlyphBitmap(atlas, cm.character, cellX, cellY + scaledAscent);
                    } catch (Throwable ignored) {
                        // Bu karakter cizilemedi, bos (seffaf) birak, istemciyi cokertme.
                    }
                }

                NativeImageBackedTexture texture = new NativeImageBackedTexture(atlas);
                texture.upload();
                mc.getTextureManager().registerTexture(bindToTexture, texture);
            } catch (Throwable ignored) {}

            generated = true;
        }

        /**
         * Rasterizes a single codepoint via stb_truetype and blits the resulting
         * 8-bit alpha bitmap into the atlas as a white/alpha glyph (color tinting
         * happens later at draw time via the vertex color), anchored so that
         * (originX, baselineY) matches the character's baseline position.
         */
        private void drawGlyphBitmap(NativeImage atlas, char c, int originX, int baselineY) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer bw = stack.mallocInt(1);
                IntBuffer bh = stack.mallocInt(1);
                IntBuffer xoff = stack.mallocInt(1);
                IntBuffer yoff = stack.mallocInt(1);

                ByteBuffer bitmap = stbtt_GetCodepointBitmap(font.info(), scale, scale, c, bw, bh, xoff, yoff);
                if (bitmap == null) return;

                try {
                    int glyphWidth = bw.get(0);
                    int glyphHeight = bh.get(0);
                    int startX = originX + xoff.get(0);
                    int startY = baselineY + yoff.get(0);

                    for (int gy = 0; gy < glyphHeight; gy++) {
                        int py = startY + gy;
                        if (py < 0 || py >= height) continue;
                        for (int gx = 0; gx < glyphWidth; gx++) {
                            int px = startX + gx;
                            if (px < 0 || px >= width) continue;
                            int alpha = bitmap.get(gy * glyphWidth + gx) & 0xFF;
                            if (alpha == 0) continue;
                            atlas.setColorArgb(px, py, (alpha << 24) | 0x00FFFFFF);
                        }
                    }
                } finally {
                    stbtt_FreeBitmap(bitmap);
                }
            }
        }
    }

    private record CharMetrics(char character, int width, int height) {}
}