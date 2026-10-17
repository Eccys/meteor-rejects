package anticope.rejects.utils;

import anticope.rejects.MeteorRejectsAddon;
import anticope.rejects.utils.seeds.Seeds;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RejectsUtils {
    @PostInit
    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("saving seeds...");
            RejectsConfig.get().save(MeteorClient.FOLDER);
            Seeds.get().save(MeteorClient.FOLDER);
        }));
    }

    public static String getModuleName(String name) {
        if (name == null) return "";
        Modules modules = Modules.get();
        if (modules == null) {
            MeteorRejectsAddon.LOG.warn("Module instantiation before Modules initialized.");
            return name;
        }
        String candidate = name;
        while (true) {
            boolean found = false;
            for (Module module : modules.getAll()) {
                if (candidate.equals(module.name)) {
                    found = true;
                    break;
                }
            }
            if (!found) return candidate;
            candidate = candidate + "*";
        }
    }

    public static String getRandomPassword(int num) {
        if (num <= 0) return "";
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(num);
        for (int i = 0; i < num; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static boolean isValidProfileName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c <= 32 || c >= 127) return false;
        }
        return true;
    }

    public static String formatHostPort(String hostAddress, int port) {
        if (hostAddress == null || hostAddress.isBlank()) {
            return "127.0.0.1:" + port;
        }
        String cleanHost = hostAddress;
        int scopeIdx = cleanHost.indexOf('%');
        if (scopeIdx > 0) {
            cleanHost = cleanHost.substring(0, scopeIdx);
        }
        if (cleanHost.contains(":") && !cleanHost.startsWith("[")) {
            return String.format("[%s]:%d", cleanHost, port);
        }
        return String.format("%s:%d", cleanHost, port);
    }

    public static java.net.URI buildServiceUri(String scheme, String hostAddress, int port) {
        String normalizedScheme = (scheme == null || scheme.isBlank())
                ? "http"
                : scheme.trim().toLowerCase(java.util.Locale.ROOT);
        return java.net.URI.create(String.format("%s://%s", normalizedScheme, formatHostPort(hostAddress, port)));
    }

    public static String formatRelativeCoord(int x, int y, int z, int distance) {
        return String.format("%d, %d, %d\n", x + distance, y + distance, z + distance);
    }

    public static boolean inFov(Entity entity, double fov) {
        if (fov >= 360) return true;
        if (entity == null || mc == null || mc.player == null) return false;

        Vec3 eyePos = mc.player.getEyePosition();
        AABB box = entity.getBoundingBox();

        // 1. If player is inside or overlapping the target entity's bounding box
        if (box.contains(eyePos) || box.intersects(mc.player.getBoundingBox())) {
            return true;
        }

        // 2. Direct ray collision check: if looking anywhere at the target's bounding box
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 endVec = eyePos.add(lookVec.x * 100, lookVec.y * 100, lookVec.z * 100);
        if (box.clip(eyePos, endVec).isPresent()) {
            return true;
        }

        // Test key points on the target entity (head/eyes, center, top, feet)
        Vec3[] testPoints = new Vec3[] {
            entity.getEyePosition(),
            box.getCenter(),
            new Vec3(entity.getX(), box.maxY, entity.getZ()),
            new Vec3(entity.getX(), box.minY, entity.getZ())
        };

        double minAngleDist = Double.MAX_VALUE;
        for (Vec3 point : testPoints) {
            float[] angle = PlayerUtils.calculateAngle(point);
            double xDist = Mth.degreesDifferenceAbs(angle[0], mc.player.getYRot());
            double yDist = Mth.degreesDifferenceAbs(angle[1], mc.player.getXRot());
            double dist = Math.hypot(xDist, yDist);
            if (dist < minAngleDist) {
                minAngleDist = dist;
            }
        }

        return minAngleDist <= fov / 2.0;
    }

    public static float fullFlightMove(PlayerMoveEvent event, double speed, boolean verticalSpeedMatch) {
        if (PlayerUtils.isMoving()) {
            double dir = getDir();

            double xDir = Math.cos(Math.toRadians(dir + 90));
            double zDir = Math.sin(Math.toRadians(dir + 90));

            ((IVec3) event.movement).meteor$setXZ(xDir * speed, zDir * speed);
        } else {
            ((IVec3) event.movement).meteor$setXZ(0, 0);
        }

        float ySpeed = 0;

        if (mc.options.keyJump.isDown())
            ySpeed += speed;
        if (mc.options.keyShift.isDown())
            ySpeed -= speed;
        ((IVec3) event.movement).meteor$setY(verticalSpeedMatch ? ySpeed : ySpeed / 2);

        return ySpeed;
    }

    private static double getDir() {
        double dir = 0;

        if (Utils.canUpdate()) {
            dir = mc.player.getYRot() + ((mc.player.zza < 0) ? 180 : 0);

            if (mc.player.xxa > 0) {
                dir += -90F * ((mc.player.zza < 0) ? -0.5F : ((mc.player.zza > 0) ? 0.5F : 1F));
            } else if (mc.player.xxa < 0) {
                dir += 90F * ((mc.player.zza < 0) ? -0.5F : ((mc.player.zza > 0) ? 0.5F : 1F));
            }
        }
        return dir;
    }
}

