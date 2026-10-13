package dev.wetsuki.protect;


import dev.wetsuki.manager.Manager;
import dev.wetsuki.protect.loader.NativeProfile;

public class NativeHelper {
    public static void setProfile() {
        Manager.USER_PROFILE = new UserProfile(
                "levin1337",
                "Deleoper",
                "09.11.2025"
        );
    }
}
