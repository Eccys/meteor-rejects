package anticope.rejects;

import anticope.rejects.arguments.ClientPosArgumentType;
import anticope.rejects.arguments.EnumArgumentType;
import anticope.rejects.arguments.EnumStringArgumentType;
import anticope.rejects.utils.RejectsUtils;
import anticope.rejects.utils.WorldUtils;
import anticope.rejects.utils.portscanner.PScanRunner;
import anticope.rejects.utils.portscanner.PortScannerManager;
import anticope.rejects.utils.seeds.Seed;
import anticope.rejects.utils.server.IPAddress;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seedfinding.mccore.version.MCVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RejectsCoreTest {

    enum ComplexEnum {
        ALPHA {
            @Override
            public String label() {
                return "a";
            }
        },
        BETA {
            @Override
            public String label() {
                return "b";
            }
        };

        public abstract String label();
    }

    @Test
    void testIPAddressParsingValidAndEdgeCases() {
        IPAddress defaultPort = IPAddress.fromText("192.168.1.1");
        assertNotNull(defaultPort);
        assertEquals("192.168.1.1:25565", defaultPort.toString());

        IPAddress customPort = IPAddress.fromText(" 10.0.0.255 : 25575 ");
        assertNotNull(customPort);
        assertEquals("10.0.0.255:25575", customPort.toString());
        assertEquals(new IPAddress(10, 0, 0, 255, 25575), customPort);
        assertEquals(new IPAddress(10, 0, 0, 255, 25575).hashCode(), customPort.hashCode());

        // Boundary ports and octets
        assertNotNull(IPAddress.fromText("0.0.0.0:1"));
        assertNotNull(IPAddress.fromText("255.255.255.255:65535"));

        // Invalid inputs
        assertNull(IPAddress.fromText(null));
        assertNull(IPAddress.fromText(""));
        assertNull(IPAddress.fromText("   "));
        assertNull(IPAddress.fromText("127.0.0.1:"));
        assertNull(IPAddress.fromText("127.0.0.1:0"));
        assertNull(IPAddress.fromText("127.0.0.1:-1"));
        assertNull(IPAddress.fromText("127.0.0.1:65536"));
        assertNull(IPAddress.fromText("127.0.0.1:25565:1"));
        assertNull(IPAddress.fromText("127.0.0.256:25565"));
        assertNull(IPAddress.fromText("-1.0.0.1:25565"));
        assertNull(IPAddress.fromText("127.0.0.1.:25565"));
        assertNull(IPAddress.fromText("127.0.1:25565"));
        assertNull(IPAddress.fromText("localhost:25565"));
    }

    @Test
    void testWorldUtilsSphereSymmetryAndDistance() {
        BlockPos origin = new BlockPos(10, 64, -20);
        assertEquals(0.0, WorldUtils.distanceBetween(origin, origin), 1e-6);
        assertEquals(5.0, WorldUtils.distanceBetween(origin, origin.offset(3, 4, 0)), 1e-5);

        int radius = 3;
        int height = 2;
        List<BlockPos> sphere = WorldUtils.getSphere(origin, radius, height);

        // No duplicate positions
        Set<BlockPos> unique = new HashSet<>(sphere);
        assertEquals(unique.size(), sphere.size());

        // Positive and negative boundary points must be symmetrically included
        assertTrue(unique.contains(origin.offset(radius, 0, 0)), "+X boundary missing");
        assertTrue(unique.contains(origin.offset(-radius, 0, 0)), "-X boundary missing");
        assertTrue(unique.contains(origin.offset(0, 0, radius)), "+Z boundary missing");
        assertTrue(unique.contains(origin.offset(0, 0, -radius)), "-Z boundary missing");
        assertTrue(unique.contains(origin.offset(0, height, 0)), "+Y boundary missing");
        assertTrue(unique.contains(origin.offset(0, -height, 0)), "-Y boundary missing");

        for (BlockPos pos : sphere) {
            assertTrue(WorldUtils.distanceBetween(origin, pos) <= radius + 1e-5);
            int dx = pos.getX() - origin.getX();
            int dy = pos.getY() - origin.getY();
            int dz = pos.getZ() - origin.getZ();
            BlockPos mirrored = origin.offset(-dx, -dy, -dz);
            assertTrue(unique.contains(mirrored), "Mirrored point missing for " + pos);
        }
    }

    @Test
    void testEnumArgumentTypesWithAnonymousConstantSubclasses() throws CommandSyntaxException {
        EnumArgumentType<ComplexEnum> argType = EnumArgumentType.enumArgument(ComplexEnum.ALPHA);
        assertEquals(ComplexEnum.ALPHA, argType.parse(new StringReader("ALPHA")));
        assertEquals(ComplexEnum.BETA, argType.parse(new StringReader("BETA")));
        assertThrows(CommandSyntaxException.class, () -> argType.parse(new StringReader("GAMMA")));

        EnumArgumentType<MCVersion> mcVersionArg = EnumArgumentType.enumArgument(MCVersion.v1_20);
        assertEquals(MCVersion.v1_19, mcVersionArg.parse(new StringReader("1.19")));

        EnumStringArgumentType stringArg = new EnumStringArgumentType(List.of("foo", "bar"));
        assertEquals("foo", stringArg.parse(new StringReader("foo")));
        assertThrows(CommandSyntaxException.class, () -> stringArg.parse(new StringReader("baz")));
    }

    @Test
    void testClientPosArgumentTypeAbsoluteRelativeAndIncomplete() throws CommandSyntaxException {
        ClientPosArgumentType posArg = ClientPosArgumentType.pos();

        Vec3 abs = posArg.parse(new StringReader("10 64 -20"));
        assertEquals(10.0, abs.x, 1e-6);
        assertEquals(64.0, abs.y, 1e-6);
        assertEquals(-20.0, abs.z, 1e-6);

        Vec3 rel = posArg.parse(new StringReader("~5 ~-2 ~"));
        assertEquals(5.0, rel.x, 1e-6);
        assertEquals(-2.0, rel.y, 1e-6);
        assertEquals(0.0, rel.z, 1e-6);

        Vec3 frac = posArg.parse(new StringReader("~-1.5 100 ~2.25"));
        assertEquals(-1.5, frac.x, 1e-6);
        assertEquals(100.0, frac.y, 1e-6);
        assertEquals(2.25, frac.z, 1e-6);

        StringReader incompleteOne = new StringReader("10");
        assertThrows(CommandSyntaxException.class, () -> posArg.parse(incompleteOne));
        assertEquals(0, incompleteOne.getCursor());

        StringReader incompleteTwo = new StringReader("10 20");
        assertThrows(CommandSyntaxException.class, () -> posArg.parse(incompleteTwo));
        assertEquals(0, incompleteTwo.getCursor());

        assertThrows(CommandSyntaxException.class, () -> posArg.parse(new StringReader("abc 20 30")));
    }

    @Test
    void testPortScannerKillAllScansAndZeroRadiusSphere() {
        List<BlockPos> singleBlockSphere = WorldUtils.getSphere(new BlockPos(5, 10, -5), 0, 0);
        assertEquals(1, singleBlockSphere.size());
        assertEquals(new BlockPos(5, 10, -5), singleBlockSphere.getFirst());

        PortScannerManager.ScanResult open = new PortScannerManager.ScanResult(25565, true);
        assertEquals(25565, open.getPort());
        assertTrue(open.isOpen());

        PortScannerManager.killAllScans();
        assertTrue(PortScannerManager.scans.isEmpty());
    }

    @Test
    void testSeedNbtSerializationRoundTrip() {
        Seed seed = new Seed(123456789012345L, MCVersion.v1_20);
        CompoundTag tag = seed.toTag();
        Seed loaded = Seed.fromTag(tag);
        assertEquals(123456789012345L, loaded.seed);
        assertEquals(MCVersion.v1_20, loaded.version);
        assertTrue(loaded.toText().getString().contains("123456789012345"));

        Seed defaultVersionSeed = new Seed(-999L, null);
        assertEquals(MCVersion.latest(), defaultVersionSeed.version);

        Seed nullSeed = new Seed(null, null);
        assertEquals(0L, nullSeed.seed);
        assertEquals(MCVersion.latest(), nullSeed.version);
        assertNotNull(nullSeed.toTag());
        assertNotNull(nullSeed.toText());

        Seed fromNullTag = Seed.fromTag(null);
        assertEquals(0L, fromNullTag.seed);
        assertEquals(MCVersion.latest(), fromNullTag.version);

        assertEquals(0L, anticope.rejects.utils.seeds.Seeds.toSeed(null));
        assertEquals(0L, anticope.rejects.utils.seeds.Seeds.toSeed("   "));
        assertEquals(42L, anticope.rejects.utils.seeds.Seeds.toSeed("  42  "));
        assertEquals("abc".hashCode(), anticope.rejects.utils.seeds.Seeds.toSeed("  abc  "));
    }

    @Test
    void testRandomPasswordAndPortScanner() throws Exception {
        String pwd = RejectsUtils.getRandomPassword(24);
        assertNotNull(pwd);
        assertEquals(24, pwd.length());
        assertTrue(pwd.matches("[a-zA-Z0-9_]{24}"));
        assertEquals("", RejectsUtils.getRandomPassword(0));
        assertEquals("", RejectsUtils.getRandomPassword(-5));
        assertEquals("", RejectsUtils.getModuleName(null));

        try (ServerSocket server = new ServerSocket(0, 5, InetAddress.getLoopbackAddress())) {
            int openPort = server.getLocalPort();
            int closedPort;
            try (ServerSocket temp = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                closedPort = temp.getLocalPort();
            }

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<List<PortScannerManager.ScanResult>> resultsRef = new AtomicReference<>();

            PScanRunner runner = new PScanRunner(
                    InetAddress.getLoopbackAddress(),
                    2,
                    1,
                    150,
                    List.of(openPort, closedPort),
                    results -> {
                        resultsRef.set(results);
                        latch.countDown();
                    }
            );

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Port scan did not complete in time");
            List<PortScannerManager.ScanResult> results = resultsRef.get();
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(2, runner.portsScanned);

            PortScannerManager.ScanResult openResult = results.stream()
                    .filter(r -> r.getPort() == openPort)
                    .findFirst()
                    .orElseThrow();
            assertTrue(openResult.isOpen());

            PortScannerManager.ScanResult closedResult = results.stream()
                    .filter(r -> r.getPort() == closedPort)
                    .findFirst()
                    .orElseThrow();
            assertFalse(closedResult.isOpen());
            assertFalse(runner.running, "PScanRunner.running should be false after completion");
        }

        // Verify immediate cancellation via PortScannerManager.killAllScans() stops runner and suppresses callback
        CountDownLatch cancelledCallbackLatch = new CountDownLatch(1);
        PScanRunner cancelledRunner = new PScanRunner(
                InetAddress.getLoopbackAddress(),
                1,
                500,
                500,
                List.of(1, 2, 3, 4, 5),
                results -> cancelledCallbackLatch.countDown()
        );
        PortScannerManager.scans.add(cancelledRunner);
        PortScannerManager.killAllScans();
        assertFalse(cancelledRunner.running);
        assertTrue(PortScannerManager.scans.isEmpty());
        assertFalse(cancelledCallbackLatch.await(150, TimeUnit.MILLISECONDS), "Cancelled scan should not invoke completion callback");
    }
}
