package dev.wetsuki.manager;

import dev.wetsuki.manager.accountManager.AccountManager;
import dev.wetsuki.manager.commandManager.CommandManager;
import dev.wetsuki.manager.configManager.ConfigManager;
import dev.wetsuki.manager.dragManager.DragManager;
import dev.wetsuki.manager.friendManager.FriendManager;
import dev.wetsuki.manager.ircManager.IrcManager;
import dev.wetsuki.manager.macroManager.MacroManager;
import dev.wetsuki.manager.modulesManager.ChestStealerManager;
import dev.wetsuki.manager.notificationManager.NotificationManager;
import dev.wetsuki.manager.proxyManager.ProxyManager;
import dev.wetsuki.manager.staffManager.StaffManager;
import dev.wetsuki.manager.themeManager.StyleManager;
import dev.wetsuki.modules.FunctionManager;
import dev.wetsuki.modules.combat.rotation.RotationController;
import dev.wetsuki.protect.UserProfile;
import dev.wetsuki.manager.fontManager.FontUtils;

public class Manager {
    public static final RotationController ROTATION = RotationController.get();
    public static UserProfile USER_PROFILE;
    public static FunctionManager FUNCTION_MANAGER;
    public static StyleManager STYLE_MANAGER;
    public static NotificationManager NOTIFICATION_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static ConfigManager CONFIG_MANAGER;
    public static MacroManager MACROS_MANAGER;
    public static StaffManager STAFF_MANAGER;
    public static CommandManager COMMAND_MANAGER;
    public static DragManager DRAG_MANAGER;
    public static SyncManager SYNC_MANAGER;
    public static FontUtils FONT_MANAGER;
    public static AccountManager ACCOUNT_MANAGER;
    public static ChestStealerManager CHESTSTEALER_MANAGER;
    public static IrcManager IRC_MANAGER;
    public static ProxyManager PROXY_MANAGER;
}
