package dev.wetsuki.manager.fontManager;

import java.io.InputStream;
import java.util.Objects;

@SuppressWarnings("All")
public class FontUtils {

    public final String fontsDir = "/wetsuki_data/fonts/";

    public volatile RenderFonts[] comfortaa = new RenderFonts[256];
    public static volatile RenderFonts[] durman = new RenderFonts[256];
    public static volatile RenderFonts[] glitched = new RenderFonts[256];
    public static volatile RenderFonts[] icons = new RenderFonts[256];
    public static volatile RenderFonts[] monsterrat = new RenderFonts[256];
    public static volatile RenderFonts[] profont = new RenderFonts[256];
    public static volatile RenderFonts[] sf_bold = new RenderFonts[256];
    public static volatile RenderFonts[] sf_medium = new RenderFonts[256];
    public static volatile RenderFonts[] iconsWex = new RenderFonts[256];
    public static volatile RenderFonts[] gilroy = new RenderFonts[256];
    public static volatile RenderFonts[] gilroy_bold = new RenderFonts[256];
    public static volatile RenderFonts[] hud = new RenderFonts[256];
    public static volatile RenderFonts[] icomoon = new RenderFonts[256];

    private static final int MIN_SIZE = 6;
    private static final int MAX_SIZE = 64;

    private volatile boolean initialized = false;
    private volatile boolean initStarted = false;

    public void init() {
        if (initStarted) return;
        initStarted = true;

        // Font yukleme artik AWT kullanmayan stb_truetype uzerinden yapiliyor ve
        // RenderFonts nesneleri tembel (lazy) calisiyor: glyph atlas'lari ilk
        // cizimde olusturuluyor. Bu yuzden butun boyutlari olusturmak neredeyse
        // bedava ve ayri thread + join() beklemesine gerek yok. Senkron yuklemek
        // hem 8 sn'lik gereksiz beklemeyi hem de "font henuz hazir degil" yarisini
        // ortadan kaldiriyor.
        try {
            // ONEMLI: Kodda kullanilan tum boyutlar (or. durman[16], durman[18],
            // sf_medium[14], sf_medium[18], sf_bold[17], sf_bold[22]) burada
            // bulunmali. Eskiden sadece birkac boyut olusturuluyordu ve eksik olan
            // boyutlar render sirasinda NullPointerException ile oyunu cokertiyordu.
            initializationFont(durman, "durman.ttf", MIN_SIZE, MAX_SIZE);
            initializationFont(sf_bold, "sf_bold.ttf", MIN_SIZE, MAX_SIZE);
            initializationFont(sf_medium, "sf_medium.ttf", MIN_SIZE, MAX_SIZE);
            initializationFont(icomoon, "icomoon.ttf", MIN_SIZE, MAX_SIZE);
            initializationFont(iconsWex, "iconsWex.ttf", MIN_SIZE, MAX_SIZE);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            initialized = true;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void initializationFont(RenderFonts[] fontArray, String fontName, int minSize, int maxSize) {
        if (fontArray == null) return;
        try (InputStream stream = Objects.requireNonNull(FontUtils.class.getResourceAsStream(fontsDir + fontName), "Font bulunamadi: " + fontName)) {
            TrueTypeFont font = TrueTypeFont.load(stream);
            for (int size = minSize; size <= maxSize && size < fontArray.length; size++) {
                try {
                    fontArray[size] = new RenderFonts(font, size);
                } catch (Throwable innerT) {
                    // Bu boyut icin basarisiz oldu, digerlerine devam et.
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
