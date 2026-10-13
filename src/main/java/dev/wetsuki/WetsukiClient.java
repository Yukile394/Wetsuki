package dev.wetsuki;

import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import dev.wetsuki.manager.*;
import dev.wetsuki.manager.accountManager.AccountManager;
import dev.wetsuki.manager.ircManager.IrcManager;
import dev.wetsuki.manager.modulesManager.ChestStealerManager;
import dev.wetsuki.manager.proxyManager.ProxyManager;
import dev.wetsuki.manager.themeManager.StyleManager;
import dev.wetsuki.protect.NativeHelper;
import dev.wetsuki.modules.setting.BindBooleanSetting;
import dev.wetsuki.modules.setting.Setting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.manager.commandManager.CommandManager;
import dev.wetsuki.manager.configManager.ConfigManager;
import dev.wetsuki.manager.dragManager.DragManager;
import dev.wetsuki.manager.dragManager.Dragging;
import dev.wetsuki.manager.friendManager.FriendManager;
import dev.wetsuki.manager.macroManager.MacroManager;
import dev.wetsuki.manager.notificationManager.NotificationManager;
import dev.wetsuki.manager.staffManager.StaffManager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionManager;
import dev.wetsuki.modules.misc.UnHook;
import dev.wetsuki.screens.dropdown.ClickGUI;
import dev.wetsuki.manager.fontManager.FontUtils;
import dev.wetsuki.util.color.ColorUtil;
import dev.wetsuki.util.player.AudioUtil;
import dev.wetsuki.util.render.providers.ResourceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Objects;

@SuppressWarnings("All")
public final class WetsukiClient implements ModInitializer {
	private static WetsukiClient instance;
	private final File directory;
	private final File directoryAddon;
	public final String name = "Wetsuki Client";
	@Getter
	boolean initialized;

	public static WetsukiClient getInstance() {
		return instance;
	}

	// init() tamamen basarili bittiginde true olur. Mixin'ler ve tus
	// dinleyicisi bu bayragi kontrol eder; boylece yarim kalmis bir
	// kurulumda (Manager.FUNCTION_MANAGER == null gibi) NPE firlatip oyunu
	// cokertmezler.
	private volatile boolean ready;
	private boolean initializing;

	public WetsukiClient() {
		// instance sadece BIR KEZ (Fabric entrypoint'i olusturdugunda) atanir.
		// Eskiden MixinKeyBoard her tusa basista "new WetsukiClient()" yapip
		// instance'i eziyordu; bu da initialized=false olan bos bir nesneyle
		// init()'in tekrar tekrar calismasina yol aciyordu.
		if (instance == null) instance = this;

		File runDir = null;
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) runDir = client.runDirectory;
		} catch (Throwable ignored) {
		}
		if (runDir == null) {
			runDir = FabricLoader.getInstance().getGameDir().toFile();
		}
		this.directory = new File(runDir, "files");
		this.directoryAddon = new File(runDir, "files/modules");
	}

	public boolean isReady() {
		return ready;
	}

	/** files/wetsuki_boot.log dosyasina zaman damgali satir yazar (takilma teshisi icin). */
	public void bootLog(String message) {
		try {
			if (!directory.exists()) directory.mkdirs();
			String line = "[" + java.time.LocalTime.now().withNano(0) + "] [" + Thread.currentThread().getName() + "] " + message + System.lineSeparator();
			Files.write(new File(directory, "wetsuki_boot.log").toPath(), line.getBytes(java.nio.charset.StandardCharsets.UTF_8),
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (Throwable ignored) {
		}
		System.out.println("[WetsukiClient] " + message);
	}

	private interface BootStep {
		void run() throws Throwable;
	}

	/** Tek bir kurulum adimini calistirir; hata verirse loglar ve false doner, oyunu cokertmez. */
	private boolean step(String stepName, BootStep action) {
		long start = System.currentTimeMillis();
		try {
			action.run();
			bootLog(stepName + " OK (" + (System.currentTimeMillis() - start) + " ms)");
			return true;
		} catch (Throwable t) {
			bootLog(stepName + " FAILED: " + t);
			t.printStackTrace();
			return false;
		}
	}

	private void setupProtection() {
		NativeHelper.setProfile();
	}

	// Takilma teshisi: oyun yukleme ekraninda (Mojang) takilirsa, "Render thread"
	// in o an NEREDE oldugunu files/wetsuki_boot.log dosyasina yazar.
	private void startWatchdog() {
		Thread t = new Thread(() -> {
			try {
				for (int i = 0; i < 30 && !initialized; i++) {
					Thread.sleep(10_000L);
					if (initialized) break;
					for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
						String n = e.getKey().getName();
						if (n.equals("Render thread") || n.equals("main")) {
							StringBuilder sb = new StringBuilder("WATCHDOG " + ((i + 1) * 10) + "s, init() henuz bitmedi. Thread '" + n + "' (" + e.getKey().getState() + "):");
							int c = 0;
							for (StackTraceElement el : e.getValue()) {
								sb.append(System.lineSeparator()).append("    at ").append(el);
								if (++c >= 40) break;
							}
							bootLog(sb.toString());
						}
					}
				}
			} catch (Throwable ignored) {
			}
		}, "WetsukiClient-Watchdog");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void onInitialize() {
		bootLog("onInitialize");
		startWatchdog();
		setupProtection();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutDown();
			} catch (Exception ignored) {}
		}));
	}

	public void init() {
		if (initialized || initializing) return;
		initializing = true;
		long total = System.currentTimeMillis();
		bootLog("init() basladi");
		try {
			ensureDirectoryExists();

			// Onceki (yarim kalmis) bir denemeden kalan modulleri temizle,
			// aksi halde ayni modullerin kopyalari listeye tekrar eklenir.
			FunctionManager.functions.clear();

			boolean core = step("SyncManager", () -> Manager.SYNC_MANAGER = new SyncManager());
			core &= step("FunctionManager", () -> Manager.FUNCTION_MANAGER = new FunctionManager());
			core &= step("StyleManager", () -> {
				Manager.STYLE_MANAGER = new StyleManager();
				Manager.STYLE_MANAGER.init();
			});
			step("AccountManager", () -> {
				Manager.ACCOUNT_MANAGER = new AccountManager();
				Manager.ACCOUNT_MANAGER.init();
			});
			core &= step("FontManager", () -> {
				Manager.FONT_MANAGER = new FontUtils();
				Manager.FONT_MANAGER.init();
			});
			step("CommandManager", () -> Manager.COMMAND_MANAGER = new CommandManager());
			step("DragManager", () -> {
				Manager.DRAG_MANAGER = new DragManager();
				Manager.DRAG_MANAGER.init();
			});
			step("MacroManager", () -> {
				Manager.MACROS_MANAGER = new MacroManager();
				Manager.MACROS_MANAGER.init();
			});
			step("FriendManager", () -> {
				Manager.FRIEND_MANAGER = new FriendManager();
				Manager.FRIEND_MANAGER.init();
			});
			step("StaffManager", () -> {
				Manager.STAFF_MANAGER = new StaffManager();
				Manager.STAFF_MANAGER.init();
			});
			step("NotificationManager", () -> Manager.NOTIFICATION_MANAGER = new NotificationManager());
			step("ChestStealerManager", () -> Manager.CHESTSTEALER_MANAGER = new ChestStealerManager());
			step("ProxyManager", () -> {
				Manager.PROXY_MANAGER = new ProxyManager();
				Manager.PROXY_MANAGER.init();
			});
			step("IrcManager", () -> {
				// IRC sunucu adresi gecersiz oldugu icin otomatik baglanti kapatildi.
				Manager.IRC_MANAGER = new IrcManager();
			});
			step("ConfigManager", () -> {
				Manager.CONFIG_MANAGER = new ConfigManager();
				Manager.CONFIG_MANAGER.init();
			});
			step("ColorImage", () -> ColorUtil.loadImage(ResourceProvider.color_image));
			step("JoinSound", () -> {
				if (Manager.FUNCTION_MANAGER.clientSounds.check.get("Client join")) {
					AudioUtil.playSound("join.wav");
				}
			});

			ready = core;
		} finally {
			// Basarili da olsa basarisiz da olsa tekrar tekrar denenmesin.
			initialized = true;
			initializing = false;
			bootLog("init() bitti, ready=" + ready + " (" + (System.currentTimeMillis() - total) + " ms)");
		}
	}

	public void keyPress(int key) {
		if (!ready || Manager.FUNCTION_MANAGER == null) return;
		int processedKey = key >= 0 ? key : -(100 + key + 2);
		Event.call(new EventKey(processedKey));

		if (key == Manager.FUNCTION_MANAGER.unHook.unHookKey.getKey() && ClientManager.legitMode) {
			UnHook.functionsToBack.forEach(function -> function.setState(true));
			File folder = new File("C:\\WetsukiClient");
			if (folder.exists()) {
				try {
					Path folderPathObj = folder.toPath();
					DosFileAttributeView attributes = Files.getFileAttributeView(folderPathObj, DosFileAttributeView.class);
					attributes.setHidden(false);
				} catch (IOException ignored) {
				}
			}
			UnHook.functionsToBack.clear();
			ClientManager.legitMode = false;
		}

		if (!ClientManager.legitMode) {
			for (Function module : Manager.FUNCTION_MANAGER.getFunctions()) {
				if (module.bind == processedKey) {
					module.toggle();
				}
				for (Setting setting : module.getSettings()) {
					if (setting instanceof BindBooleanSetting bindSetting) {
						bindSetting.onKeyPress(key, true);
					}
				}
			}

			if (key == Manager.FUNCTION_MANAGER.clickGUI.getBindCode()) {
				MinecraftClient.getInstance().setScreen(new ClickGUI());
			}
			if (Manager.MACROS_MANAGER != null) {
				Manager.MACROS_MANAGER.onKeyPressed(key);
			}
		}
	}

	public void shutDown() {
		if (!ready) return;
		try { if (Manager.DRAG_MANAGER != null) Manager.DRAG_MANAGER.save(); } catch (Throwable ignored) {}
		try { if (Manager.ACCOUNT_MANAGER != null) { Manager.ACCOUNT_MANAGER.saveAccounts(); Manager.ACCOUNT_MANAGER.saveLastAlt(); } } catch (Throwable ignored) {}
		try { if (Manager.CONFIG_MANAGER != null) Manager.CONFIG_MANAGER.saveConfiguration("autocfg"); } catch (Throwable ignored) {}
		try { if (Manager.IRC_MANAGER != null) Manager.IRC_MANAGER.shutdown(); } catch (Throwable ignored) {}
		try { if (Manager.FUNCTION_MANAGER != null) Manager.FUNCTION_MANAGER.globals.clear(); } catch (Throwable ignored) {}
		System.out.println("[-] Client shutdown");
	}
	public static void openURL(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();

			if (os.contains("win")) {
				Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
			} else if (os.contains("mac")) {
				Runtime.getRuntime().exec(new String[]{"open", url});
			} else {
				Runtime.getRuntime().exec(new String[]{"xdg-open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Dragging createDrag(Function function, String name, float x, float y) {
		DragManager.draggables.put(name, new Dragging(function, name, x, y));
		return DragManager.draggables.get(name);
	}
	private void ensureDirectoryExists() {
		if (!directory.exists() && !directory.mkdirs()) {
			System.err.println("Failed to create directory: " + directory.getAbsolutePath());
		}
		if (!directoryAddon.exists() && !directoryAddon.mkdirs()) {
			System.err.println("Failed to create directory: " + directoryAddon.getAbsolutePath());
		}
	}
}