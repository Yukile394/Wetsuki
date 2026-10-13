package dev.wetsuki.modules.movement.freelook;

public interface CameraOverriddenEntity {
    float getCameraPitch();
    float getCameraYaw();
    void setCameraPitch(float pitch);
    void setCameraYaw(float yaw);
}
