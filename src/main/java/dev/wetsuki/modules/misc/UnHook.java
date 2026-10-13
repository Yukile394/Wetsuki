package dev.wetsuki.modules.misc;

import org.lwjgl.glfw.GLFW;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.screens.unhook.UnHookScreen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionAnnotation(name = "UnHook",keywords = {"SelfDestruct"}, desc = "Disables the cheat to pass a check", type = Type.Misc)
public class UnHook extends Function {
    public static BindSetting unHookKey = new BindSetting("Return key", GLFW.GLFW_KEY_INSERT);
    public static final List<Function> functionsToBack = new CopyOnWriteArrayList<>();
    public UnHook() {
        addSettings(unHookKey);
    }


    @Override
    public void onEvent(Event event) {
    }

    @Override
    protected void onEnable() {
        mc.setScreen(new UnHookScreen());
        super.onEnable();
    }

    public void onUnhook() {
        functionsToBack.clear();
        for (int i = 0; i < Manager.FUNCTION_MANAGER.getFunctions().size(); i++) {
            Function function = Manager.FUNCTION_MANAGER.getFunctions().get(i);
            if (function.state && function != this) {
                functionsToBack.add(function);
                function.setState(false);
            }
        }
        File folder = new File("C:\\WetsukiClient");

        if (folder.exists()) {
            try {
                Path folderPathObj = folder.toPath();
                DosFileAttributeView attributes = Files.getFileAttributeView(folderPathObj, DosFileAttributeView.class);
                attributes.setHidden(true);
            } catch (IOException e) {
            }

        }
        toggle();
    }
}
