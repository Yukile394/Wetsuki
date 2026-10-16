package dev.wetsuki.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.util.move.NetworkUtils;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.input.EventKeyBoard;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.events.impl.player.EventSprint;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.mixin.iface.ClientPlayerEntityAccessor;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.movement.ElytraTarget;
import dev.wetsuki.modules.render.littlePet.GhostWolfEntity;
import dev.wetsuki.modules.setting.BindBooleanSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.math.RayTraceUtil;
import dev.wetsuki.util.move.MoveUtil;
import dev.wetsuki.util.player.AuraUtil;
import dev.wetsuki.util.player.InventoryUtil;
import dev.wetsuki.util.vector.VectorUtil;

import java.util.*;

import static net.minecraft.util.Hand.MAIN_HAND;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AttackAura", keywords = {"Smash","Pound","KillAura"}, desc = "Smashes everyone's head around you", type = Type.Combat)
public class AttackAura extends Function {

    private final ModeSetting mode = new ModeSetting("Mode",
            "ReallyWorld",
            "ReallyWorld",
            "HollyWorld",
            "FunTime",
            "SpookyTime",
            "Snap",
            "KoopinAc",
            "LonyGrief",
            "1.8.8"
    );

    private final MultiSetting targets = new MultiSetting(
            "Targets",
            Arrays.asList("Players", "Naked", "Mobs", "Monsters"),
            new String[]{"Players", "Naked", "Friends", "Mobs", "Monsters", "Villagers"}
    );

    private final ModeSetting sort = new ModeSetting("Sort",
            "By health",
            "By health",
            "By distance",
            "By armor"
    );

    private final MultiSetting setting = new MultiSetting(
            "Settings",
            Arrays.asList("Crits only", "Break shield", "Shield release"),
            new String[]{"Crits only", "Break shield", "Shield release"}
    );

    private final SliderSetting distance = new SliderSetting("Attack radius", 3.0f, 1.8f, 6f, 0.1f);
    private final SliderSetting rotateDistance = new SliderSetting("Detection radius", 5f, 0.0f, 10f, 0.1f);

    private final SliderSetting elytraDistance = new SliderSetting("Elytra radius", 40f, 0f, 80f, 1f);
    private final SliderSetting snapSpeed = new SliderSetting("Snap speed", 150, 50, 300, 50f, () -> mode.is("Snap"));

    private final BindBooleanSetting onlySpaceCritical = new BindBooleanSetting("Only with space", false, () -> setting.get("Crits only"));
    private final BooleanSetting noAttackIfEat = new BooleanSetting("Don't hit while eating", false);
    private final BooleanSetting raycast = new BooleanSetting("Check aim", false);

    public final BooleanSetting correction = new BooleanSetting("Correction", true);
    public final ModeSetting correctionType = new ModeSetting(() -> correction.get(), "Correction type", "Free", "Free", "Focus");
    private final ModeSetting sprintreset = new ModeSetting("Sprint type", "Rage", "Rage", "Legit", "None");

    // ---- Çember hareketi (her özellik bağımsız açılıp kapanır, ayarlar config'e kaydedilir) ----
    private final BooleanSetting circleStrafe = new BooleanSetting("Circle strafe", false, "Orbits the target on a circle instead of running into it");
    private final SliderSetting circleRadius = new SliderSetting("Circle radius", 4.0f, 2.5f, 8.0f, 0.1f, () -> circleStrafe.get());
    private final BooleanSetting circleDash = new BooleanSetting("Dash to hit", true, "Moves in to hit range when the hit is ready, then goes back to the circle", () -> circleStrafe.get());
    private final BooleanSetting circleSwitch = new BooleanSetting("Switch direction", true, "Changes orbit direction from time to time", () -> circleStrafe.get());
    private final SliderSetting circleSwitchTime = new SliderSetting("Switch time (s)", 3.0f, 1.0f, 8.0f, 0.5f, () -> circleStrafe.get() && circleSwitch.get());
    private final SliderSetting circleSmooth = new SliderSetting("Smoothing", 0.4f, 0.1f, 1.0f, 0.05f, () -> circleStrafe.get());
    private final BooleanSetting circleSafe = new BooleanSetting("Avoid walls and edges", true, "Reverses the orbit instead of walking into walls or off ledges", () -> circleStrafe.get());

    // ---- FakeLag: hareket paketlerini aralıklarla geciktirir (kendi ekranında lag yok) ----
    private final BooleanSetting fakeLag = new BooleanSetting("Fake lag", false, "Delays your movement packets in bursts; your own game stays smooth");
    private final SliderSetting fakeLagDelay = new SliderSetting("Lag duration (ms)", 250f, 50f, 800f, 10f, () -> fakeLag.get());
    private final SliderSetting fakeLagPause = new SliderSetting("Lag pause (ms)", 400f, 100f, 2000f, 50f, () -> fakeLag.get());
    private final BooleanSetting fakeLagOnlyTarget = new BooleanSetting("Only near target", true, "Lags only while an Aura target is close", () -> fakeLag.get());
    private final SliderSetting fakeLagRange = new SliderSetting("Lag range", 10f, 4f, 20f, 1f, () -> fakeLag.get() && fakeLagOnlyTarget.get());

    public LivingEntity target = null;

    private int circDir = 1;
    private long circSwitchAt = 0L;
    private long circFlipLock = 0L;
    private Vec3d circSmooth = Vec3d.ZERO;

    private final java.util.concurrent.ConcurrentLinkedQueue<Packet<?>> lagBuffer = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private volatile boolean lagging = false;
    private volatile long lagPhaseStart = 0L;

    public double getAttackRadius() {
        return distance.get().doubleValue();
    }
    private long cpsLimit = 0L;
    private long lastHitMs = 0L;
    private int preSprintTicks = 0;

    public AttackAura() {
        addSettings(
                mode,
                targets,
                sort,
                setting,
                distance,
                rotateDistance,
                elytraDistance,
                snapSpeed,
                correction,
                correctionType,
                sprintreset,
                onlySpaceCritical,
                noAttackIfEat,
                raycast,
                circleStrafe, circleRadius, circleDash, circleSwitch, circleSwitchTime, circleSmooth, circleSafe,
                fakeLag, fakeLagDelay, fakeLagPause, fakeLagOnlyTarget, fakeLagRange
        );
    }

    @Override
    public void onEvent(Event event) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.isDead()) {
            target = null;
            preSprintTicks = 0;
            circSmooth = Vec3d.ZERO;
            dropFakeLag();
            return;
        }

        if (event instanceof EventPacket packetEvent) {
            handleLagPacket(packetEvent);
            return;
        }

        if (event instanceof EventKeyBoard e) {
            if (correction.get() && correctionType.is("Free")) {
                MoveUtil.fixMovement(e, Manager.FUNCTION_MANAGER.autoPotion.isActivePotion ? Manager.ROTATION.getPitch() : Manager.ROTATION.getYaw());
            }
            applyCircleStrafe(e);
        }

        if (event instanceof EventSprint sprint) {
            if (sprintreset.is("Legit")) {
                if (canAttack() && target != null  && player.isSprinting()) {
                    sprint.setSprinting(false);
                }
            }
        }

        if (event instanceof EventUpdate) {
            if (target == null || !isValidTarget(target)) {
                target = findTarget();
            }
            tickFakeLag();

            if (target == null) {
                Manager.ROTATION.set(player.getYaw(), player.getPitch());
                cpsLimit = System.currentTimeMillis();
                return;
            }

            handleAttackAndRotation(target);
        }

        if (event instanceof EventMotion motion) {
            motion.setYaw(Manager.ROTATION.getYaw());
            motion.setPitch(Manager.ROTATION.getPitch());
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        if (target != null && isValidTarget(target)) {
            String modeName = mode.get();
            if (modeName.equals("FunTime") || modeName.equals("HollyWorld") || modeName.equals("ReallyWorld")) {
                Manager.ROTATION.smoothReturn(350);
            } else {
                Manager.ROTATION.set(mc.player.getYaw(), mc.player.getPitch());
            }
        }

        target = null;
        cpsLimit = System.currentTimeMillis();
        circSmooth = Vec3d.ZERO;
        releaseFakeLag();
        super.onDisable();
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity.isDead() || !entity.isAlive() || entity == mc.player) return false;

        double dist = AuraUtil.getDistance(entity);
        double attackRange = distance.get().doubleValue();
        double detectRange = mc.player.isGliding() ? elytraDistance.get().doubleValue() : rotateDistance.get().doubleValue();

        if (dist > attackRange && (detectRange <= 0 || dist > detectRange)) return false;
        if (Manager.FUNCTION_MANAGER.antiBot.check(entity)) return false;

        if (entity instanceof PlayerEntity) {
            if (!targets.get("Players")) return false;
            if (!targets.get("Friends") && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString())) return false;
        } else if (entity instanceof VillagerEntity && !targets.get("Villagers")) return false;
        else if (entity instanceof MobEntity || entity instanceof AnimalEntity) {
            if (!targets.get("Mobs")) return false;
        } else if (entity instanceof Monster && !targets.get("Monsters")) return false;

        if (entity instanceof ArmorStandEntity) return false;
        if (Manager.FUNCTION_MANAGER.littleSnickers.state && (entity instanceof GhostWolfEntity)) return false;

        return true;
    }

    private LivingEntity findTarget() {
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : Manager.SYNC_MANAGER.getEntities()) {
            if (e instanceof LivingEntity le && isValidTarget(le)) list.add(le);
        }
        if (list.isEmpty()) return null;

        switch (sort.get()) {
            case "By health":
                list.sort(Comparator.comparing(LivingEntity::getHealth));
                break;
            case "By distance":
                list.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
            case "By armor":
                list.sort(Comparator.comparingDouble(AuraUtil::getArmor));
                break;
            default:
                break;
        }
        return list.get(0);
    }

    private float randomYawOffset = 0;
    private float randomPitchOffset = 0;
    private int randomUpdateTicks = 0;
    private float bodyYaw, bodyPitch, prevBodyYaw, prevBodyPitch;
    private float headYaw, headPitch, prevHeadYaw, prevHeadPitch;

    private final int updateInterval = 2;
    private final float maxYawShake = 0.3f;
    private final float maxPitchShake = 0.25f;
    private final Random random = new Random();


    private long shakeStartTime = 0L;
    private void handleAttackAndRotation(LivingEntity t) {
        float currYaw = Manager.ROTATION.getYaw();
        float currPitch = Manager.ROTATION.getPitch();

        boolean canAttackNow = shouldAttack(t);
        boolean passRay = !raycast.get() || RayTraceUtil.getMouseOver(t, currYaw, currPitch, distance.get().floatValue()) == t;
        boolean noPotion = !Manager.FUNCTION_MANAGER.autoPotion.isActivePotion;

        if (handleElytraRotation(t)) {
            if (canAttackNow && passRay && noPotion && elytraHitAllowed()) attackTarget(mc.player);
            return;
        }

        if (mode.is("KoopinAc") || mode.is("1.8.8")) {
            koopinVector(t, true);
            if (canAttackNow && passRay && noPotion) attackTarget(mc.player);
            return;
        }

        if (mode.is("SpookyTime")) {
            if (mc.player == null) return;

            if (target == null) {
                randomYawOffset = 0;
                randomPitchOffset = 0;
                float centerYaw = mc.player.getYaw();
                float centerPitch = mc.player.getPitch();

                {
                    prevBodyYaw = bodyYaw;
                    prevBodyPitch = bodyPitch;

                    float yawDiff = MathHelper.wrapDegrees(centerYaw - bodyYaw);
                    float pitchDiff = centerPitch - bodyPitch;

                    float yawStep = MathHelper.clamp(yawDiff, -45, 45);
                    float pitchStep = MathHelper.clamp(pitchDiff, -45, 45);

                    bodyYaw += yawStep;
                    bodyPitch = MathHelper.clamp(bodyPitch + pitchStep, -90f, 90f);
                }

                {
                    prevHeadYaw = headYaw;
                    prevHeadPitch = headPitch;

                    float yawDiff = MathHelper.wrapDegrees(centerYaw - headYaw);
                    float pitchDiff = centerPitch - headPitch;

                    float yawStep = MathHelper.clamp(yawDiff, -50, 50);
                    float pitchStep = MathHelper.clamp(pitchDiff, -50, 50);

                    headYaw += yawStep;
                    headPitch = MathHelper.clamp(headPitch + pitchStep, -90f, 90f);
                }
                return;
            }

            randomUpdateTicks++;
            if (randomUpdateTicks >= updateInterval) {
                randomUpdateTicks = 0;
                randomYawOffset = (random.nextFloat() * 2 - 1) * maxYawShake;
                randomPitchOffset = (random.nextFloat() * 2 - 1) * maxPitchShake;
            }

            Vec3d targetPos;
            {
                double x = target.getBoundingBox().getCenter().x;
                double y = target.getY();
                double z = target.getBoundingBox().getCenter().z;

                int randPoint = random.nextInt(4);
                switch (randPoint) {
                    case 0 -> y += target.getHeight() * 0.9;
                    case 1 -> y += target.getHeight() * 0.75;
                    case 2 -> y += target.getHeight() * 0.5;
                    case 3 -> y += target.getHeight() * 0.25;
                }

                x += (random.nextDouble() * 0.4 - 0.2);
                z += (random.nextDouble() * 0.4 - 0.2);

                targetPos = new Vec3d(x, y, z);
            }

            Vec2f rot;
            {
                Vec3d eyePos = mc.player.getEyePos();
                double deltaX = targetPos.x - eyePos.x;
                double deltaY = targetPos.y - eyePos.y;
                double deltaZ = targetPos.z - eyePos.z;
                double hDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                double yaw = (Math.atan2(deltaZ, deltaX) * 180 / Math.PI) - 90.0f;
                double pitch = -(Math.atan2(deltaY, hDistance) * 180 / Math.PI);

                rot = new Vec2f((float) pitch, (float) yaw);
            }
            {
                prevBodyYaw = bodyYaw;
                prevBodyPitch = bodyPitch;

                float yawDiff = MathHelper.wrapDegrees(rot.y - bodyYaw);
                float pitchDiff = rot.x - bodyPitch;

                bodyYaw += MathHelper.clamp(yawDiff, -45, 45);
                bodyPitch = MathHelper.clamp(bodyPitch + pitchDiff, -45, 45);
            }

            Vec2f headRotation;
            {
                prevHeadYaw = headYaw;
                prevHeadPitch = headPitch;

                float yawDiff = MathHelper.wrapDegrees(rot.y - headYaw);
                float pitchDiff = rot.x - headPitch;

                headYaw += MathHelper.clamp(yawDiff, -50, 50);
                headPitch = MathHelper.clamp(pitchDiff, -50, 50);

                headRotation = new Vec2f(
                        rot.x + randomPitchOffset,
                        rot.y + randomYawOffset
                );
            }
            Manager.ROTATION.setSmooth(headRotation.y, headRotation.x, 0.8f, 60, 90, true);
            if (canAttackNow && passRay && noPotion) {
                attackTarget(mc.player);
            }

            return;
        }

        if (mode.is("LonyGrief")) {
            Vec3d tp = predictPos(t);
            double yawToTarget = Math.toDegrees(Math.atan2(tp.z - mc.player.getZ(), tp.x - mc.player.getX())) - 90.0;
            double yawDiff = Math.abs(MathHelper.wrapDegrees((float) yawToTarget - currYaw));
            if (yawDiff <= 180 && canAttackNow && passRay && noPotion) attackTarget(mc.player);
            Manager.ROTATION.set(mc.player.getYaw(), mc.player.getPitch());
            return;
        }

        if (mode.is("FunTime")) {
            if (canAttackNow && canAttack() && noPotion) {
                if (passRay) {
                    attackTarget(mc.player);
                }
                lastHitMs = System.currentTimeMillis();
            }
           if (System.currentTimeMillis() - lastHitMs < 450) {
               funtime(t);

           } else {
               long currentTime = System.currentTimeMillis();
               if (shakeStartTime == 0L)
                   shakeStartTime = currentTime;
               float elapsedSec = (currentTime - shakeStartTime) / 1000f;

               double angle = 2 * Math.PI * 2.4 * elapsedSec;
               float yawOffset = (float) Math.sin(angle) * 24f;

               double angle2 = 2 * Math.PI * 0.08f * elapsedSec;
               double[] options = {5.0, 5.5, 5.8, 6.0};
               double randAmplitude = options[(int)(Math.random() * options.length)];
               float yawOffset2 = (float) ((float) Math.sin(angle2) * randAmplitude);


               float finalYaw = mc.player.getYaw() + yawOffset + yawOffset2;
               float finalPitch = 0.0f + yawOffset2;
               Manager.ROTATION.setSmooth(finalYaw, finalPitch, 1.0f, 20f, 10f, true);
           }

            return;
        }

        if (mode.is("HollyWorld")) {
            if (canAttackNow && canAttack() && passRay && noPotion) {
                hollyworld(t, true);
                attackTarget(mc.player);
            } else {
                hollyworld(t, false);
            }
            return;
        }

        if (mode.is("Snap")) {
            if (canAttackNow && canAttack() && passRay && noPotion) {
                attackTarget(mc.player);
                lastHitMs = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - lastHitMs < (long) snapSpeed.get().floatValue()) {
                setRotation(t, true);
            } else {
                Manager.ROTATION.set(mc.player.getYaw(), mc.player.getPitch());
            }
            return;
        }

        if (canAttackNow && passRay && noPotion) {
            attackTarget(mc.player);
        }
        setRotation(t, true);
    }
    private void setRotation(LivingEntity entity, boolean applyGcd) {
        Vec3d tp = predictPos(entity);
        double dx = tp.x - mc.player.getX();
        double dy = (tp.y + entity.getEyeHeight(entity.getPose()) / 2.0) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = tp.z - mc.player.getZ();

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        Manager.ROTATION.setSmooth(yaw, pitch, 1.2f, 180f, 15f, applyGcd);
    }

    private void koopinVector(LivingEntity entity, boolean attackContext) {
        Vec3d head = entity.getEyePos().add(0, entity.getHeight(), 0);
        Vec3d chest = entity.getEyePos().add(0, entity.getStandingEyeHeight() / 2.0f, 0);
        Vec3d legs = entity.getEyePos().add(0, 0.05, 0);
        Vec3d[] points = new Vec3d[]{head, chest, legs};

        float bestPitchDelta = Float.MAX_VALUE;
        Vec3d best = chest;
        float currPitch = Manager.ROTATION.getPitch();
        float currYaw = Manager.ROTATION.getYaw();

        for (Vec3d p : points) {
            Vec3d eye = mc.player.getEyePos();
            double dx = p.x - eye.x;
            double dy = p.y - eye.y;
            double dz = p.z - eye.z;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            float pitchDelta = Math.abs(pitch - currPitch);
            if (pitchDelta < bestPitchDelta) {
                bestPitchDelta = pitchDelta;
                best = p;
            }
        }

        Vec3d eye = mc.player.getEyePos();
        double dx = best.x - eye.x;
        double dy = best.y - eye.y;
        double dz = best.z - eye.z;
        double dst = Math.sqrt(dx * dx + dz * dz);

        float yawTo = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitchTo = (float) (-Math.toDegrees(Math.atan2(dy, dst)));

        float yawDelta = MathHelper.wrapDegrees(yawTo - currYaw);
        float pitchDelta = pitchTo - currPitch;

        float addYaw = Math.min(Math.max(Math.abs(yawDelta), 1), 80);
        if (Math.abs(addYaw) <= 3.0f) addYaw = 3.1f;

        float addPitch = Math.max(attackContext ? Math.abs(pitchDelta) : 1.0f, 2.0f);

        float ny = currYaw + (yawDelta > 0 ? addYaw : -addYaw);
        float np = MathHelper.clamp(currPitch + (pitchDelta > 0 ? addPitch : -addPitch), -90.0f, 90.0f);

        Manager.ROTATION.set(ny, np);
    }

    private boolean swingSideRight = false;
    private float jitterYaw = 0f, jitterYawTarget = 0f, jitterYawSpeed = 0f;
    private float microJitter = 0f;
    private float swayPhase = 0f;
    private float swaySpeed = 0.04f;
    private float swayAmplitude = 2.5f;
    private long lastSwitch = 0L;
    private long lastBreathChange = 0L;


    private void funtime(LivingEntity entity) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d base = entity.getPos();

        float[] points = new float[]{0.82f, 0.67f, 0.43f, 0.27f};
        float mul = points[(int) (System.currentTimeMillis() / 180 % points.length)];
        Vec3d targetPos = new Vec3d(base.x, base.y + entity.getHeight() * mul, base.z);

        double halfWidth = entity.getWidth() / 2.0;
        double sideOffset = swingSideRight ? halfWidth * 1.2f : -halfWidth * 1;

        double yawToEntity = Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX());
        double offsetX = Math.cos(yawToEntity + Math.PI / 2) * sideOffset;
        double offsetZ = Math.sin(yawToEntity + Math.PI / 2) * sideOffset;
        targetPos = targetPos.add(offsetX, 0, offsetZ);

        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float baseYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float basePitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        long now = System.currentTimeMillis();

        if (now - lastSwitch > 200 + random.nextInt(250)) {
            lastSwitch = now;
            swingSideRight = !swingSideRight;

            float distanceFactor = (float) MathHelper.clamp(dist / 6.0f, 0.4f, 1.0f);
            float maxDeviation = 4.0f * distanceFactor;

            jitterYawTarget = (swingSideRight ? maxDeviation : -maxDeviation) + (float) (random.nextGaussian() * 0.6f);
        }

        float diff = jitterYawTarget - jitterYaw;
        jitterYawSpeed += diff * 0.05f;
        jitterYawSpeed *= 0.88f;
        jitterYaw += jitterYawSpeed;
        jitterYaw *= 0.985f;

        if (now - lastBreathChange > 2000 + random.nextInt(1500)) {
            lastBreathChange = now;
            swaySpeed = 0.035f + random.nextFloat() * 0.02f;
            swayAmplitude = 2.0f + random.nextFloat() * 1.2f;
        }

        swayPhase += swaySpeed;
        float sway = (float) Math.sin(swayPhase) * swayAmplitude;
        float totalYawOffset = (float) MathHelper.clamp(jitterYaw + sway, -halfWidth * 8.5f, halfWidth * 8.5f);
        microJitter += (random.nextFloat() - 0.5f) * 0.25f;
        microJitter *= 0.85f;

        float finalYaw = baseYaw + totalYawOffset + microJitter;
        float finalPitch = basePitch + (float) Math.sin(swayPhase * 0.8f) * 0.5f;


        Manager.ROTATION.setSmooth(finalYaw, finalPitch, 1.1f, 180f, 15f, true);
    }



    private void hollyworld(LivingEntity entity, boolean force) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d base = entity.getPos();
        float[] points = new float[]{0.85f, 0.65f, 0.35f, 0.25f};
        float mul = points[(int) (System.nanoTime() % points.length)];
        Vec3d aim = new Vec3d(base.x, base.y + entity.getHeight() * mul, base.z);

        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;

        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, hd));

        if (force) {
            Manager.ROTATION.set(yaw, MathHelper.clamp(pitch, -89.9f, 89.9f));
        } else {
            Manager.ROTATION.setSmooth(yaw, pitch, 0.25f, 45f, 12f, true);
        }
    }

    private boolean handleElytraRotation(LivingEntity t) {
        ElytraTarget ely = Manager.FUNCTION_MANAGER.elytraTarget;
        if (ely.state && mc.player.isGliding()) {
            if (ely.mode.is("Advanced")) ely.overtakingElytra(t, false);
            else ely.targetDefault(t, false);
            return true;
        }
        return false;
    }

    /** ElytraTarget açıkken uçuşta: sadece kılıçla ve kritik şartları hazırken vur. */
    private boolean elytraHitAllowed() {
        if (!Manager.FUNCTION_MANAGER.elytraTarget.state || !mc.player.isGliding()) return true;
        if (!mc.player.getMainHandStack().isIn(ItemTags.SWORDS)) return false;
        return mc.player.fallDistance > 0.0f
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle();
    }

    public void attackTarget(PlayerEntity player) {
        boolean sprintStop = false;
        boolean canStartSprint = mc.player.input.movementForward > 0
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.isGliding()
                && !mc.player.isUsingItem()
                && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6
                && !mc.player.isSneaking();

        if (setting.get("Shield release") && mc.player.isBlocking()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (sprintreset.is("Legit") && !mc.player.isGliding()) {
            if (mc.player.isSprinting() || canAttack()) {
                if (mc.player.isSprinting()) return;
            }
        }

        boolean critSprintStop = Manager.FUNCTION_MANAGER.elytraTarget.state && mc.player.isGliding();

        if (sprintreset.is("Rage") || critSprintStop) {
            if (((ClientPlayerEntityAccessor) mc.player).getLastSprinting()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.player.setSprinting(false);
                sprintStop = true;
            }
        }

        cpsLimit = System.currentTimeMillis() + 500L;

        flushLagBuffer(true);   // vuruştan önce bekleyen hareketleri gönder: sunucu sırayı doğru görsün
        mc.interactionManager.attackEntity(player, target);
        mc.player.swingHand(MAIN_HAND);

        ElytraTarget elytraTarget = Manager.FUNCTION_MANAGER.elytraTarget;
        if (elytraTarget.mode.is("Advanced")) {
            elytraTarget.trueFireWork = true;
            if (elytraTarget.prefer.get()) elytraTarget.nextPhase(target);
        }

        if (setting.get("Break shield")) shieldBreaker(false);

        if (sprintreset.is("Rage") && sprintStop && canStartSprint) {
            if (((ClientPlayerEntityAccessor) mc.player).getLastSprinting()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                mc.player.setSprinting(true);
            }
        }
    }

    private boolean shouldAttack(LivingEntity e) {
        if (e == null || cpsLimit > System.currentTimeMillis()) return false;
        if (AuraUtil.getDistance(e) > distance.get().doubleValue()) return false;
        return canAttack();
    }

    private boolean canAttack() {
        if (noAttackIfEat.get() && mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD)) return false;

        if (System.currentTimeMillis() < cpsLimit
                || (!(mc.player.getMainHandStack().isOf(Items.MACE))
                && mc.player.getAttackCooldownProgress(mc.getRenderTickCounter().getTickDelta(true)) < 0.9F)) return false;

        boolean restrict = mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || mc.player.isInLava()
                || mc.player.inPowderSnow
                || mc.player.isClimbing()
                || mc.player.hasVehicle()
                || mc.player.getAbilities().flying
                || mc.player.isGliding()
                || (mc.player.isInFluid() && !mc.options.jumpKey.isPressed())
                || MoveUtil.isInWeb();

        boolean needSpace = onlySpaceCritical.get()
                && mc.player.isOnGround()
                && !mc.options.jumpKey.isPressed();

        if (setting.get("Crits only") && !restrict) {
            return needSpace || (!mc.player.isOnGround() && mc.player.fallDistance > 0.0f);
        }
        return true;
    }

    private boolean shieldBreaker(boolean instant) {
        int axeSlot = InventoryUtil.getAxe().slot();
        if (axeSlot == -1) return false;
        if (!(target instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) target).isUsingItem() && !instant) return false;
        if (((PlayerEntity) target).getOffHandStack().getItem() != Items.SHIELD
                && ((PlayerEntity) target).getMainHandStack().getItem() != Items.SHIELD) return false;

        if (axeSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
        return true;
    }

    // ======================= Çember hareketi =======================

    private boolean circleBlocked(Vec3d dir) {
        Vec3d from = mc.player.getPos();
        for (double step : new double[]{0.6, 1.2}) {
            double px = from.x + dir.x * step;
            double pz = from.z + dir.z * step;
            BlockPos feet = BlockPos.ofFloored(px, from.y + 0.1, pz);
            BlockPos head = feet.up();
            if (!mc.world.getBlockState(feet).getCollisionShape(mc.world, feet).isEmpty()) return true;
            if (!mc.world.getBlockState(head).getCollisionShape(mc.world, head).isEmpty()) return true;
            if (mc.player.isOnGround()) {
                BlockPos d1 = feet.down();
                BlockPos d2 = feet.down(2);
                boolean hole = mc.world.getBlockState(d1).getCollisionShape(mc.world, d1).isEmpty()
                        && mc.world.getBlockState(d2).getCollisionShape(mc.world, d2).isEmpty()
                        && mc.world.getFluidState(d1).isEmpty();
                if (hole) return true;
            }
        }
        return false;
    }

    /** İstenen yarıçapa göre yarıçap yönü + teğet bileşenini birleştirir (birim uzunlukta). */
    private Vec3d circleVector(Vec3d radial, double dist, double want, int dirSign) {
        double err = want - dist;                                   // > 0: dışarı, < 0: içeri
        double radialMag = MathHelper.clamp(err * 1.2, -1.0, 1.0);
        Vec3d radialVel = radial.multiply(radialMag);
        double tangentMag = Math.sqrt(Math.max(0.0, 1.0 - radialMag * radialMag));
        Vec3d tangent = new Vec3d(-radial.z, 0, radial.x).multiply(dirSign * tangentMag);
        return radialVel.add(tangent);
    }

    private void applyCircleStrafe(EventKeyBoard e) {
        LivingEntity t = target;
        if (!circleStrafe.get() || t == null
                || mc.player.isGliding() || t.isGliding()
                || mc.player.isTouchingWater() || mc.player.isInLava()
                || mc.player.hasVehicle()
                || Manager.FUNCTION_MANAGER.targetStrafe.state) {
            circSmooth = Vec3d.ZERO;
            return;
        }

        Vec3d me = mc.player.getPos();
        Vec3d tp = t.getPos();
        double dx = me.x - tp.x;
        double dz = me.z - tp.z;
        double dist = Math.hypot(dx, dz);

        double minCenter = t.getWidth() / 2.0 + mc.player.getWidth() / 2.0 + 0.6;       // hedefin içine girme
        double hitCenter = Math.max(minCenter + 0.2, distance.get().doubleValue() + t.getWidth() / 2.0 - 0.6);
        double orbit = Math.max(circleRadius.get().doubleValue(), hitCenter + 0.6);

        if (dist > orbit + 5.0) {                                  // çok uzak: normal hareket/Aura yaklaşması
            circSmooth = Vec3d.ZERO;
            return;
        }

        long now = System.currentTimeMillis();
        if (circleSwitch.get() && now >= circSwitchAt) {
            circDir = -circDir;
            circSwitchAt = now + (long) (circleSwitchTime.get().doubleValue() * 1000.0);
        }

        Vec3d radial = dist < 1.0E-4 ? new Vec3d(1, 0, 0) : new Vec3d(dx / dist, 0, dz / dist);

        boolean ready = mc.player.getAttackCooldownProgress(mc.getRenderTickCounter().getTickDelta(true)) >= 0.9f;
        double want = (circleDash.get() && ready) ? hitCenter : orbit;
        if (dist < minCenter) want = Math.max(want, minCenter + 0.4);  // içine girdiyse hemen dışarı çık

        Vec3d desired = circleVector(radial, dist, want, circDir);

        if (circleSafe.get() && circleBlocked(desired.normalize())) {
            if (now >= circFlipLock) {                             // sık sık yön değiştirip titremesin
                circDir = -circDir;
                circFlipLock = now + 500L;
                circSwitchAt = now + (long) (circleSwitchTime.get().doubleValue() * 1000.0);
            }
            desired = circleVector(radial, dist, want, circDir);
            if (circleBlocked(desired.normalize())) {
                desired = radial.multiply(MathHelper.clamp((want - dist) * 1.2, -1.0, 1.0));
                if (circleBlocked(desired.normalize())) desired = Vec3d.ZERO;
            }
        }

        double alpha = MathHelper.clamp(circleSmooth.get().doubleValue(), 0.1, 1.0);
        circSmooth = circSmooth.add(desired.subtract(circSmooth).multiply(alpha));
        Vec3d out = circSmooth.lengthSquared() > 1.0 ? circSmooth.normalize() : circSmooth;

        // Dünya yönünü, hareketin hesaplandığı bakış açısına göre ileri/yan girişe çevir
        boolean correcting = correction.get() || Manager.ROTATION.isControlling();
        float yaw = correcting ? Manager.ROTATION.getYaw() : mc.player.getYaw();
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        float forward = (float) (-out.x * sin + out.z * cos);
        float strafe = (float) (out.x * cos + out.z * sin);

        e.setMovementForward(forward);
        e.setMovementStrafe(strafe);
    }

    // ======================= FakeLag =======================

    public boolean isFakeLagBuffering() {
        return lagging;
    }

    private boolean fakeLagAllowed() {
        if (!fakeLag.get() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        if (mc.player.isDead() || mc.player.isGliding()) return false;      // elytra uçuşunu ElytraTarget'in FakeLag'i yönetir
        if (mc.isInSingleplayer()) return false;
        if (fakeLagOnlyTarget.get()) {
            LivingEntity t = target;
            if (t == null || AuraUtil.getDistance(t) > fakeLagRange.get().doubleValue()) return false;
        }
        return true;
    }

    private void tickFakeLag() {
        if (!fakeLagAllowed()) {
            releaseFakeLag();
            return;
        }
        long now = System.currentTimeMillis();
        if (lagging) {
            long hold = Math.min(fakeLagDelay.get().longValue(), 1000L);    // sunucu zaman aşımına girmesin
            if (now - lagPhaseStart >= hold) {
                lagging = false;
                flushLagBuffer(true);
                lagPhaseStart = now;
            }
        } else if (now - lagPhaseStart >= fakeLagPause.get().longValue()) {
            lagging = true;
            lagPhaseStart = now;
        }
    }

    private void handleLagPacket(EventPacket e) {
        Packet<?> p = e.getPacket();
        if (e.isReceivePacket()) {
            if (p instanceof PlayerPositionLookS2CPacket) {                 // sunucu bizi ışınladı: eski konumları gönderme
                lagBuffer.clear();
                lagging = false;
                lagPhaseStart = System.currentTimeMillis();
            }
            return;
        }
        if (!lagging || !fakeLag.get()) return;

        if (p instanceof PlayerMoveC2SPacket || p instanceof PlayerInputC2SPacket) {
            lagBuffer.add(p);
            e.setCancel(true);
            return;
        }
        // Başka bir paket (saldırı, eşya, komut...) gidiyorsa önce bekleyen hareketleri gönder, sıra bozulmasın
        if (!(p instanceof KeepAliveC2SPacket) && !(p instanceof CommonPongC2SPacket)) {
            flushLagBuffer(true);
        }
    }

    private void flushLagBuffer(boolean send) {
        if (lagBuffer.isEmpty()) return;
        boolean canSend = send && mc.player != null && mc.player.networkHandler != null
                && mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection().isOpen();
        Packet<?> pk;
        while ((pk = lagBuffer.poll()) != null) {
            if (!canSend) continue;
            try {
                NetworkUtils.sendSilentPacket(pk);
            } catch (Exception ex) {
                lagBuffer.clear();
                break;
            }
        }
    }

    private void releaseFakeLag() {
        lagging = false;
        flushLagBuffer(true);
    }

    /** Bağlantı/oyuncu yokken: bekleyenleri gönderme, sadece temizle. */
    private void dropFakeLag() {
        lagging = false;
        lagBuffer.clear();
        lagPhaseStart = System.currentTimeMillis();
    }

    private Vec3d predictPos(LivingEntity e) {
        Vec3d p = e.getPos();
        var ts = Manager.FUNCTION_MANAGER.targetStrafe;
        if (ts.state && ts.predictCheck.get()) {
            float pr = ts.predict.get().floatValue();
            if (pr > 0) {
                Vec3d v = e.getVelocity();
                p = p.add(v.x * pr, v.y * pr, v.z * pr);
            }
        }
        return p;
    }
}