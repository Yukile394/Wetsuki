package dev.wetsuki.modules.movement;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import dev.wetsuki.util.player.AuraUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.render.EventRender3D;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BindBooleanSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.color.ColorUtil;
import dev.wetsuki.util.move.NetworkUtils;
import dev.wetsuki.util.player.InventoryUtil;
import dev.wetsuki.util.player.TimerUtil;
import dev.wetsuki.util.render.RenderAddon;
import dev.wetsuki.util.render.RenderUtil;

import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("All")
@FunctionAnnotation(name = "ElytraTarget", desc = "Target players to elytra!", type = Type.Move)
public class ElytraTarget extends Function {

    public final ModeSetting mode = new ModeSetting("Type", "Normal", "Normal", "Advanced");

    private final BooleanSetting frontMode = new BooleanSetting("Front of target", true, "Flies in front of the target and hits it");
    private final SliderSetting frontDistance = new SliderSetting("Front distance", 3.0, 1.0, 15.0, 0.5, () -> frontMode.get());
    private final BooleanSetting hitboxAim = new BooleanSetting("Hitbox aim", true, "Aims exactly at the target inside attack range so hits land", () -> frontMode.get());
    private final BooleanSetting frontHitbox = new BooleanSetting("Show hitbox", true, () -> frontMode.get());

    private final SliderSetting sila = new SliderSetting("Fly away", 8, 5, 40, 1, () -> mode.is("Advanced"));
    private final SliderSetting silaTime = new SliderSetting("Fly away time", 400f, 200f, 1000f, 10f, () -> mode.is("Advanced"));

    private final BindBooleanSetting perelet = new BindBooleanSetting("Fly over", true);
    private final SliderSetting predict = new SliderSetting("Predict", 2.6f, 0.1f, 6f, 0.1f, () -> perelet.get());

    private final BooleanSetting leaveHP = new BooleanSetting("Fly off", true, () -> mode.is("Advanced"));
    private final MultiSetting leaveList = new MultiSetting("Fly off when",
            Arrays.asList("Low health", "When using an item", "Shield release"),
            List.of("Low health", "When using an item"),
            () -> leaveHP.get() && mode.is("Advanced"));

    private final BindBooleanSetting resolver = new BindBooleanSetting("Resolver evasion", true, "Stops the opponent from hitting you, not effective on the ground", () -> mode.is("Advanced"));
    private final SliderSetting resolverStrength = new SliderSetting("Resolver strength", 12, 4, 30, 1, () -> resolver.get() && mode.is("Advanced"));

    public final BindBooleanSetting prefer = new BindBooleanSetting("Change direction", true, "Change flight direction when hit", () -> mode.is("Advanced"));

    private final MultiSetting preferDir = new MultiSetting("Direction",
            Arrays.asList("North", "South", "West", "East", "Up"),
            List.of("North", "South", "West", "East", "Up", "Down"),
            () -> mode.is("Advanced") && prefer.get());

    private final BooleanSetting avoidWalls = new BooleanSetting("Avoid walls", true, "Deflects the trajectory if there is a wall ahead", () -> mode.is("Advanced"));
    private final BooleanSetting firework = new BooleanSetting("Firework sending", true, () -> mode.is("Advanced"));
    private final SliderSetting fireworkTime = new SliderSetting("Send time", 800f, 100f, 2000f, 10.0f, () -> firework.get() && mode.is("Advanced"));

    private final BooleanSetting fakelags = new BooleanSetting("Fake Lags", true, () -> mode.is("Advanced"));
    private final SliderSetting fakelagsDistance = new SliderSetting("Flagger distance", 10.0, 5.0, 20.0, 0.5, () -> fakelags.get() && mode.is("Advanced"));
    private final SliderSetting fakelagsMaxDistance = new SliderSetting("Max flagger distance", 30.0, 15.0, 50.0, 1.0, () -> fakelags.get() && mode.is("Advanced"));

    private final CopyOnWriteArrayList<Packet<?>> packetBuffer = new CopyOnWriteArrayList<>();
    private boolean isAccumulatingPackets = false;
    private boolean wasInFarRange = false;
    private LivingEntity currentTarget = null;
    private Vec3d defensivePos = null;
    private final TimerUtil defensiveTimer = new TimerUtil();

    private final BooleanSetting visual = new BooleanSetting("Render position", false, () -> mode.is("Advanced") || mode.is("Normal"));

    @Getter
    boolean defensiveActive, lastDefensive;

    private int rotationPhase = 0;
    private int directionIndex = 0;
    private long lastHitTime = 0;
    private LivingEntity lastTarget = null;

    private long targetLastAttackTime = 0;

    private final Map<String, Vec3d> namedDirections = Map.of(
            "North", new Vec3d(0, 0, -1),
            "South", new Vec3d(0, 0, 1),
            "West", new Vec3d(-1, 0, 0),
            "East", new Vec3d(1, 0, 0),
            "Up", new Vec3d(0, 1, 0),
            "Down", new Vec3d(0, -1, 0)
    );

    private List<Vec3d> airDirections = new ArrayList<>();

    private final TimerUtil fireworkTimer = new TimerUtil();
    private final TimerUtil sanyaEblan = new TimerUtil();
    public boolean trueFireWork = false;

    private int x, y, z;

    private Vec3d frontFeet = null;
    private double frontWidth = 0.6, frontHeight = 1.8;
    private long frontTime = 0L;

    // Takılmayı önleyen yumuşatma durumu
    private LivingEntity frontEntity = null;
    private Vec3d smMotion = Vec3d.ZERO;
    private Vec3d smDir = null;
    private Vec3d smCenter = null;
    private boolean smMoving = false;
    private boolean smFast = false;
    private Vec3d prevRawDir = null;
    private double smTurn = 0.0;

    public ElytraTarget() {
        addSettings(
                mode,
                frontMode, frontDistance, hitboxAim, frontHitbox,
                sila, silaTime,
                perelet, predict,
                leaveHP, leaveList,
                resolver, resolverStrength,
                prefer, preferDir,
                avoidWalls,
                firework, fireworkTime,
                fakelags, fakelagsDistance, fakelagsMaxDistance,
                visual
        );
    }

    @Override
    public void onEnable() {
        rebuildAirDirections();
        packetBuffer.clear();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
        frontFeet = null;
        frontEntity = null;
        smDir = null;
        smCenter = null;
        smFast = false;
        prevRawDir = null;
        smTurn = 0.0;
    }

    private void rebuildAirDirections() {
        airDirections.clear();
        boolean isFlying = mc.player != null && mc.player.isGliding();

        for (String name : preferDir.getAvailableModes()) {
            if (!preferDir.get(name)) continue;
            if (name.equals("Down") && !isFlying) continue;
            Vec3d dir = namedDirections.get(name);
            if (dir != null) airDirections.add(dir);
        }

        if (airDirections.isEmpty()) {
            for (Map.Entry<String, Vec3d> entry : namedDirections.entrySet()) {
                if (entry.getKey().equals("Down") && !isFlying) continue;
                airDirections.add(entry.getValue());
            }
        }
        if (directionIndex >= airDirections.size()) directionIndex = 0;
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventRender3D) {
            renderFrontHitbox();
        }

        if (mode.is("Advanced") && mc.player.isGliding()) {
            if (event instanceof EventPacket packetEvent) {
                handlePacketEvent(packetEvent);
            }

            if (event instanceof EventUpdate) {
                updateFakeLagsState();

                if (firework.get()) {
                    if (fireworkTimer.hasTimeElapsed(fireworkTime.get().longValue())) {
                        if (trueFireWork) {
                            InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET, true, false);
                        }
                        fireworkTimer.reset();
                    }
                }
            }
            if (event instanceof EventUpdate) {
                defensiveActive = shouldLeave();
                lastDefensive = defensiveActive;

                if (resolver.get() && rotationPhase != 0) {
                    float evadePitch = (float) (Math.sin(System.currentTimeMillis() / 100.0) * resolverStrength.get().floatValue());
                    float evadeYaw = (float) (Math.cos(System.nanoTime() / 100.0) * resolverStrength.get().floatValue());
                    float cy = Manager.ROTATION.getYaw();
                    float cp = Manager.ROTATION.getPitch();
                    Manager.ROTATION.set(cy + evadeYaw, MathHelper.clamp(cp + evadePitch, -89.9f, 89.9f));
                }

                if (rotationPhase != 0 && System.currentTimeMillis() - lastHitTime > silaTime.get().floatValue()) {
                    rotationPhase = 0;
                }
            }
            if (event instanceof EventRender3D render) {
                if (visual.get() && mc.player.isGliding() && Manager.FUNCTION_MANAGER.attackAura.target != null) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    Box box = new Box(pos).contract(0.01);
                    RenderUtil.render3D.drawHoleOutline(box, ColorUtil.rgba(128, 255, 128, 255), 2);
                }

                if (defensivePos != null && fakelags.get() && isAccumulatingPackets) {
                    RenderAddon.renderFakePlayer(defensivePos, fakelags.get(), isAccumulatingPackets, render);
                }
            }
        }
    }

    private void handlePacketEvent(EventPacket packetEvent) {
        if (!fakelags.get() || !(packetEvent.getPacket() instanceof Packet)) return;

        if (isAccumulatingPackets && packetEvent.isSendPacket() && !(packetEvent.getPacket() instanceof KeepAliveC2SPacket)) {
            packetBuffer.add(packetEvent.getPacket());
            packetEvent.setCancel(true);
        }
    }

    private void updateFakeLagsState() {
        if (!fakelags.get()) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            return;
        }

        boolean isFlying = mc.player.isGliding();
        LivingEntity target = Manager.FUNCTION_MANAGER.attackAura.target;
        boolean hasTarget = target != null;

        if (target != null && !target.equals(currentTarget)) {
            currentTarget = target;
            if (isAccumulatingPackets) {
                sendBufferedPackets();
            }
        }

        if (!isFlying || !hasTarget || perelet.get() || defensiveActive) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }

        double distanceToTarget = mc.player.getPos().distanceTo(target.getPos());
        double triggerDistance = fakelagsDistance.get().doubleValue();
        double maxDistance = fakelagsMaxDistance.get().doubleValue();

        if (distanceToTarget > maxDistance) {
            if (isAccumulatingPackets) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
            }
            wasInFarRange = false;
            defensivePos = null;
            return;
        }
        if (distanceToTarget > triggerDistance && distanceToTarget <= maxDistance) {
            if (!isAccumulatingPackets) {
                isAccumulatingPackets = true;
                defensivePos = mc.player.getPos();
                defensiveTimer.reset();
            }
            wasInFarRange = true;
        } else if (distanceToTarget <= triggerDistance && wasInFarRange) {
            sendBufferedPackets();
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        } else if (distanceToTarget <= triggerDistance) {
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        }

        if (isAccumulatingPackets && defensiveTimer.hasTimeElapsed(1500)) {
            sendBufferedPackets();
            isAccumulatingPackets = true;
            defensiveTimer.reset();
        }
    }

    private void sendBufferedPackets() {
        if (packetBuffer.isEmpty()) return;
        for (Packet<?> packet : packetBuffer) {
            NetworkUtils.sendSilentPacket(packet);
        }
        packetBuffer.clear();
    }

    public void onTargetAttack(LivingEntity attacker) {
        if (attacker != null && attacker.equals(lastTarget)) {
            targetLastAttackTime = System.currentTimeMillis();
            if (fakelags.get()) {
                sendBufferedPackets();
                isAccumulatingPackets = false;
                wasInFarRange = false;
                defensivePos = null;
            }
        }
    }

    @Override
    public void onDisable() {
        sendBufferedPackets();
        isAccumulatingPackets = false;
        wasInFarRange = false;
        currentTarget = null;
        defensivePos = null;
    }

    private boolean shouldLeave() {
        boolean lowHP = mc.player.getHealth() <= 4.0f && leaveList.get("Low health");
        boolean usingItem = mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD) && leaveList.get("When using an item");
        return leaveHP.get() && (lowHP || usingItem);
    }

    public void nextPhase(LivingEntity target) {
        lastTarget = target;
        lastHitTime = System.currentTimeMillis();
        rotationPhase = 1;

        if (fakelags.get()) {
            sendBufferedPackets();
            isAccumulatingPackets = false;
            wasInFarRange = false;
            defensivePos = null;
        }

        boolean isFlying = mc.player != null && mc.player.isGliding();
        List<Vec3d> newDirections = new ArrayList<>();
        for (String name : preferDir.getAvailableModes()) {
            if (!preferDir.get(name)) continue;
            if (name.equals("Down") && !isFlying) continue;
            Vec3d dir = namedDirections.get(name);
            if (dir != null) newDirections.add(dir);
        }
        if (newDirections.isEmpty()) {
            for (Map.Entry<String, Vec3d> entry : namedDirections.entrySet()) {
                if (entry.getKey().equals("Down") && !isFlying) continue;
                newDirections.add(entry.getValue());
            }
        }

        int oldIndexInNew = -1;
        Vec3d oldDir = null;
        if (!airDirections.isEmpty() && directionIndex >= 0 && directionIndex < airDirections.size()) {
            oldDir = airDirections.get(directionIndex);
        }
        if (oldDir != null) {
            for (int i = 0; i < newDirections.size(); i++) {
                Vec3d v = newDirections.get(i);
                if (Double.compare(v.x, oldDir.x) == 0 && Double.compare(v.y, oldDir.y) == 0 && Double.compare(v.z, oldDir.z) == 0) {
                    oldIndexInNew = i;
                    break;
                }
            }
        }

        int size = newDirections.size();
        int candidateIndex = (oldIndexInNew == -1) ? 0 : (oldIndexInNew + 1) % size;
        int attempts = 0;
        int chosenIndex = candidateIndex;

        while (attempts < size) {
            Vec3d dir = newDirections.get(candidateIndex);
            if (resolver.get() && dir.y < 0 && !mc.player.isGliding()) {
                candidateIndex = (candidateIndex + 1) % size;
                attempts++;
                continue;
            }

            Vec3d candidatePos = target.getPos().add(dir.normalize().multiply(sila.get().floatValue()));
            Vec3d tryBetter = findClosestValidPosAround(candidatePos, 4.5);
            Vec3d checkPos = tryBetter != null ? tryBetter : new Vec3d(candidatePos.x, Math.max(candidatePos.y, mc.player.getY() + 6), candidatePos.z);

            if (!(avoidWalls.get() && isObstructed(mc.player.getEyePos(), checkPos)) && isValidFlyPosition(checkPos)) {
                chosenIndex = candidateIndex;
                break;
            }

            candidateIndex = (candidateIndex + 1) % size;
            attempts++;
        }

        airDirections = newDirections;
        directionIndex = chosenIndex;
    }

    private void renderFrontHitbox() {
        if (!frontMode.get() || !frontHitbox.get() || frontFeet == null) return;
        if (!mc.player.isGliding() || Manager.FUNCTION_MANAGER.attackAura.target == null) return;
        if (System.currentTimeMillis() - frontTime > 300L) return;

        double hw = frontWidth / 2.0;
        Box box = new Box(
                frontFeet.x - hw, frontFeet.y, frontFeet.z - hw,
                frontFeet.x + hw, frontFeet.y + frontHeight, frontFeet.z + hw
        );
        RenderUtil.render3D.drawHoleOutline(box, ColorUtil.rgba(255, 70, 70, 255), 2);
    }

    private boolean isFrontClear(Vec3d from, Vec3d to) {
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == HitResult.Type.MISS;
    }

    private static Vec3d lerpVec(Vec3d a, Vec3d b, double t) {
        return a.add(b.subtract(a).multiply(t));
    }

    /** Hedefin hareket/bakış yönünün önünde, ayarlanan mesafedeki hitbox merkezi (tick'ler arası yumuşatılmış). */
    private Vec3d computeFrontCenter(LivingEntity base) {
        Vec3d motion = new Vec3d(base.getX() - base.prevX, base.getY() - base.prevY, base.getZ() - base.prevZ);

        // Hedef değiştiyse yumuşatmayı sıfırla
        if (base != frontEntity) {
            frontEntity = base;
            smMotion = motion;
            smDir = null;
            smCenter = null;
            smMoving = false;
            smFast = false;
            prevRawDir = null;
            smTurn = 0.0;
        }

        // Hareketi yumuşat (paket titremesi noktayı zıplatmasın) + histerezis
        smMotion = lerpVec(smMotion, motion, motion.lengthSquared() > smMotion.lengthSquared() * 1.5 ? 0.6 : 0.35);
        double speedSq = smMotion.lengthSquared();
        if (smMoving) smMoving = speedSq > 0.01; else smMoving = speedSq > 0.03;
        // Hedef hareket ediyorsa (yürüyen/koşan/uçan) "önüne geç" mantığı, duruyorsa doğrudan hedefe uç
        smFast = smMoving;

        Vec3d dir;
        if (smMoving) {
            dir = smMotion.normalize();
        } else {
            Vec3d look = base.getRotationVecClient();
            Vec3d flat = new Vec3d(look.x, 0, look.z);
            dir = flat.lengthSquared() < 1.0E-4 ? Vec3d.fromPolar(0, base.getYaw()) : flat.normalize();
        }

        // Hedef ne kadar kıvrılıyor (etrafımızda dönüyor mu)? Düz giderken ~0, dönerken büyük
        if (prevRawDir != null) {
            double turn = 1.0 - prevRawDir.dotProduct(dir);
            smTurn = smTurn + (turn - smTurn) * 0.4;
        }
        prevRawDir = dir;

        // Yönü yumuşat
        if (smDir == null) {
            smDir = dir;
        } else {
            double follow = smDir.dotProduct(dir) > 0.7 ? 0.3 : 0.7; // ani yön değişiminde (kaçış) hızlı takip
            Vec3d blended = lerpVec(smDir, dir, follow);
            smDir = blended.lengthSquared() < 1.0E-6 ? dir : blended.normalize();
        }

        // Kesişme: hedefin bizim varacağımız zamandaki konumunu yinelemeli hesapla (kaçan hedef için tek adım yetmez)
        Vec3d lead = Vec3d.ZERO;
        if (smMoving && !mc.player.isTouchingWater()) {
            double mySpeed = Math.max(mc.player.getVelocity().length(), 1.0);
            Vec3d me = mc.player.getPos();
            double t = 0.0;
            for (int i = 0; i < 3; i++) {
                Vec3d future = base.getPos().add(smMotion.multiply(t));
                t = MathHelper.clamp(me.distanceTo(future) / mySpeed, 0.0, 12.0);
            }
            // Hedef dönüyorsa (etrafımızda dolanıyorsa) doğrusal tahmin noktayı çok dışarı atar: tahmini kapat
            double straight = MathHelper.clamp(1.0 - smTurn / 0.03, 0.0, 1.0);
            lead = smMotion.multiply(t * straight);
            if (lead.lengthSquared() > 36.0) lead = lead.normalize().multiply(6.0);
        }

        Vec3d half = new Vec3d(0, base.getHeight() / 2.0, 0);
        Vec3d origin = base.getPos().add(lead).add(half);

        // Nokta her zaman saldırı menzilinin içinde kalsın ki AttackAura vuruş yapabilsin
        double maxHitDist = Math.max(1.0, Manager.FUNCTION_MANAGER.attackAura.getAttackRadius() - 0.5);
        double dist = Math.min(frontDistance.get().doubleValue(), maxHitDist);
        Vec3d center = origin.add(smDir.multiply(dist));
        for (int i = 0; i < 3 && !isFrontClear(origin, center); i++) {
            dist *= 0.6;
            center = origin.add(smDir.multiply(dist));
        }
        if (dist < 1.0) {                       // engel çok yakınsa bile hedefin merkezine çökme, en az 1 blok önde kal
            dist = 1.0;
            center = origin.add(smDir.multiply(dist));
        }

        // Nokta blok/zemin içine düştüyse yukarı kaldır (içine uçulamaz, önüne geçmek imkansız olur)
        for (int i = 0; i < 4; i++) {
            net.minecraft.util.math.BlockPos bp = net.minecraft.util.math.BlockPos.ofFloored(center);
            if (mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) break;
            center = center.add(0, 1.0, 0);
        }

        // Noktanın kendisini de yumuşat (ani sıçrama olursa sıfırla)
        if (smCenter == null) {
            smCenter = center;
        } else {
            double dd = smCenter.distanceTo(center);
            if (dd > 6.0) {
                smCenter = center;                       // büyük sıçrama: sıfırla
            } else if (dd > 0.3) {
                smCenter = lerpVec(smCenter, center, 0.45); // küçük titremeyi yoksay, gerisini yumuşat
            }
        }

        frontFeet = smCenter.subtract(half);
        frontWidth = base.getWidth();
        frontHeight = base.getHeight();
        if (smFast) frontTime = System.currentTimeMillis();
        return smCenter;
    }

    /** Vuruş hazırsa ve hedef menzile yakınsa true: bu sırada bakış hedefe çevrilir ki vuruş hedefe dönükken yapılsın. */
    private boolean inHitWindow(LivingEntity base) {
        double radius = Manager.FUNCTION_MANAGER.attackAura.getAttackRadius();
        // Yaklaşma hızı yüksekse vuruştan 1-2 tick önce menzile girilmiş olur; bakışı erken hazırla
        double closing = mc.player.getVelocity().subtract(smMotion).length();
        double margin = MathHelper.clamp(1.0 + closing * 1.2, 1.0, 4.5);
        boolean inRange = AuraUtil.getDistance(base) <= radius + margin;
        if (hitboxAim.get()) return inRange;   // menzildeyken her zaman hitbox'a nişan al
        boolean ready = mc.player.getMainHandStack().isOf(Items.MACE)
                || mc.player.getAttackCooldownProgress(0.0f) >= 0.7f;
        return ready && inRange;
    }

    /** Verilen noktaya bakarsak bakış çizgisi hedefin gövdesinden geçer mi? */
    private boolean rayHitsTarget(LivingEntity base, Vec3d aim) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d dir = aim.subtract(eye);
        double lenSq = dir.lengthSquared();
        if (lenSq < 1.0E-6) return true;

        Vec3d tc = base.getPos().add(0, base.getHeight() * 0.5, 0);
        double t = tc.subtract(eye).dotProduct(dir) / lenSq;
        if (t < 0.0) return false;                       // hedef bakış yönünün arkasında

        Vec3d closest = eye.add(dir.multiply(t));
        Vec3d diff = closest.subtract(tc);
        double horizontal = Math.hypot(diff.x, diff.z);
        return horizontal <= base.getWidth() * 0.5 + 0.3 && Math.abs(diff.y) <= base.getHeight() * 0.5 + 0.2;
    }

    /**
     * Hedefin gövdesine nişan noktası. Paket bu tick'in sonunda, biz hareket ettikten sonra gittiği için
     * kendi hızımızı çıkarıp hedefin bir tick sonraki konumuna göre hesaplar.
     */
    private Vec3d hitAimPoint(LivingEntity base) {
        Vec3d center = base.getPos().add(0, base.getHeight() * 0.5, 0).add(smMotion);
        Vec3d eye = mc.player.getEyePos();
        Vec3d a = center.subtract(mc.player.getVelocity());
        Vec3d toA = a.subtract(eye);
        // Telafi edilen nokta çok yakınsa ya da arkamızda kalıyorsa düz hedefe bak
        if (toA.lengthSquared() < 0.25 || toA.dotProduct(center.subtract(eye)) <= 0.0) return center;
        return a;
    }

    /** Vuruş penceresinde hedefin kendisine, değilse hedefin önündeki noktaya nişan alır. */
    private Vec3d aimPointFor(LivingEntity base, Vec3d front) {
        Vec3d targetAim = hitAimPoint(base);
        if (!smFast) {
            frontTime = 0L;                       // hitbox'a değil hedefe uçuyoruz: kutuyu gizle
            return targetAim;
        }

        // Önüne geçmeye devam et. Sadece vuruş hazırsa ve hedef menzildeyken bu nişan hedefi kaçırıyorsa
        // (hedef yanımızdan/etrafımızdan dönüyorsa) hedefe dön ki vuruş hedefe dönükken yapılsın.
        Vec3d aim = stableAimPoint(front);
        if (inHitWindow(base) && (hitboxAim.get() || !rayHitsTarget(base, aim))) {
            frontTime = 0L;                       // vuruş için hedefe dönüyoruz: kutuyu gizle
            return targetAim;
        }
        return aim;
    }

    /**
     * Önündeki noktaya çok yaklaşınca yön ani dönüp takılma yapmasın diye, nişan noktanın
     * biraz ilerisine (hedefin gittiği yöne) kayar. Hedefe geri dönmez, hep önde kalırsın.
     */
    private Vec3d stableAimPoint(Vec3d front) {
        if (smDir == null) return front;
        double d = mc.player.getEyePos().distanceTo(front);
        if (d >= 3.0) return front;
        double w = MathHelper.clamp((d - 0.8) / 2.2, 0.0, 1.0);
        Vec3d beyond = front.add(smDir.multiply(4.0));
        return lerpVec(beyond, front, w);
    }

    private void aimAt(Vec3d p) {
        double dx = p.x - mc.player.getX();
        double dy = p.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = p.z - mc.player.getZ();

        x = (int) Math.floor(p.x);
        y = (int) Math.floor(p.y);
        z = (int) Math.floor(p.z);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));

        Manager.ROTATION.setSmooth(yaw, pitch, 1.15f, 140f, 40f, true);
    }

    public void overtakingElytra(LivingEntity base, boolean attack) {
        boolean leave = shouldLeave();
        boolean inEvasion = rotationPhase != 0 || leave;

        Vec3d targetPos = base.getPos().add(0, base.getHeight() / 2.0, 0);
        Vec3d modifiedPos;

        if (inEvasion) {
            Vec3d dir = airDirections.get(directionIndex);
            double strength = sila.get().floatValue();
            Vec3d candidatePos = targetPos.add(dir.normalize().multiply(strength));
            Vec3d tryBetter = findClosestValidPosAround(candidatePos, 10);
            if (tryBetter != null) {
                trueFireWork = true;
                candidatePos = tryBetter;
            } else {
                candidatePos = new Vec3d(candidatePos.x, Math.max(candidatePos.y, mc.player.getY() + 2), candidatePos.z);
            }
            modifiedPos = candidatePos;

            if (mc.player.getPos().distanceTo(modifiedPos) < 1) {
                rotationPhase = 0;
            }
        } else {
            double bps;
            try {
                bps = Double.parseDouble(ClientManager.getBps(base));
            } catch (Exception e) {
                bps = 0.0;
            }

            boolean canPredict = perelet.get() && bps >= 20.0 && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater();

            if (frontMode.get()) {
                modifiedPos = aimPointFor(base, computeFrontCenter(base));
            } else if (canPredict) {
                Vec3d motion = new Vec3d(base.getX() - base.prevX, base.getY() - base.prevY, base.getZ() - base.prevZ);
                double predictFactor = MathHelper.clamp(predict.get().floatValue(), 0.2F, 2.5F);
                Vec3d predicted = motion.multiply(predictFactor * 1.5);

                if (predicted.lengthSquared() < 1.0E-4) {
                    predicted = base.getRotationVecClient().normalize().multiply(predictFactor);
                }
                modifiedPos = base.getPos().add(predicted).add(0, base.getHeight() / 2.0, 0);
            } else {
                modifiedPos = base.getPos().add(0, base.getHeight() / 2.0, 0);
            }
        }

        double dx = modifiedPos.x - mc.player.getX();
        double dy = modifiedPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = modifiedPos.z - mc.player.getZ();

        x = (int) modifiedPos.x;
        y = (int) modifiedPos.y;
        z = (int) modifiedPos.z;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));

        Manager.ROTATION.setSmooth(
                targetYaw,
                targetPitch,
                1.15f,
                140f,
                40f,
                true
        );
    }

    public void targetDefault(LivingEntity base, boolean attack) {
        if (frontMode.get()) {
            aimAt(aimPointFor(base, computeFrontCenter(base)));
            return;
        }
        Vec3d targetPos = base.getPos();
        double bps;

        try {
            bps = Double.parseDouble(ClientManager.getBps(base));
        } catch (Exception e) {
            bps = 0.0;
        }

        boolean canPredict = perelet.get() && bps >= 20.0 && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater();

        if (canPredict) {
            Vec3d motion = new Vec3d(base.getX() - base.prevX, base.getY() - base.prevY, base.getZ() - base.prevZ);
            double predictFactor = MathHelper.clamp(predict.get().floatValue(), 0.2F, 2.5F);
            Vec3d predicted = motion.multiply(predictFactor * 1.5);

            if (predicted.lengthSquared() < 1.0E-4) {
                predicted = base.getRotationVecClient().normalize().multiply(predictFactor);
            }
            targetPos = targetPos.add(predicted);
        }

        double diffX = targetPos.x - mc.player.getX();
        double diffY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = targetPos.z - mc.player.getZ();

        x = (int) targetPos.x;
        y = (int) targetPos.y;
        z = (int) targetPos.z;

        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))));

        Manager.ROTATION.setSmooth(
                yaw,
                pitch,
                1.15f,
                140f,
                40f,
                true
        );
    }


    private Vec3d findClosestValidPosAround(Vec3d center, double radius) {
        List<Vec3d> offsets = new ArrayList<>();
        for (int y = 2; y >= -1; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && y == 0) continue;
                    offsets.add(new Vec3d(dx, y, dz));
                }
            }
        }

        Vec3d best = null;
        double bestDist = Double.MAX_VALUE;

        for (Vec3d offset : offsets) {
            Vec3d candidate = center;
            if (!isValidFlyPosition(candidate)) continue;
            if (isObstructed(mc.player.getEyePos(), candidate)) continue;
            double dist = candidate.squaredDistanceTo(center);
            if (dist < bestDist) {
                best = candidate;
                bestDist = dist;
            }
        }
        return best;
    }

    private boolean isObstructed(Vec3d from, Vec3d to) {
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() != HitResult.Type.MISS;
    }

    private boolean isValidFlyPosition(Vec3d pos) {
        Vec3d head = new Vec3d(pos.x, pos.y + mc.player.getHeight(), pos.z);
        Vec3d feet = new Vec3d(pos.x, pos.y, pos.z);
        boolean obstructedVertically = mc.world.raycast(new net.minecraft.world.RaycastContext(
                head, feet,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() != HitResult.Type.MISS;

        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        boolean isBlockSolid = mc.world.getBlockState(new BlockPos(blockX, blockY, blockZ)).isOpaqueFullCube();
        int groundY = mc.world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, new BlockPos(blockX, 0, blockZ)).getY();

        boolean belowGround = pos.y < groundY + 1.0;

        return !obstructedVertically && !isBlockSolid && !belowGround;
    }
}