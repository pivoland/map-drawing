package wawa.mapwright.neoforge.sync;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import wawa.mapwright.MapwrightClient;
import wawa.mapwright.data.PageIO;
import wawa.mapwright.data.sync.MapSyncBridge;
import wawa.mapwright.data.sync.MapSyncOperation;
import wawa.mapwright.data.sync.PinSyncBridge;
import wawa.mapwright.data.sync.PlayerIconSyncBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = MapwrightClient.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MapSyncNetworking {
    private static final ServerMapState SERVER_STATE = new ServerMapState();
    private static final int CHUNK_SIZE = 1024;
    private static final int CLIENT_BATCH = 768;
    private static final int DEBOUNCE_MS = 0;
    private static boolean serverBootstrapped;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("3");
        registrar.playBidirectional(MapSyncPayload.TYPE, MapSyncPayload.STREAM_CODEC, MapSyncNetworking::handleMapSync);
        registrar.playBidirectional(PinSyncPayload.TYPE, PinSyncPayload.STREAM_CODEC, MapSyncNetworking::handlePinSync);
        registrar.playToServer(C2SRequestFullStatePayload.TYPE, C2SRequestFullStatePayload.STREAM_CODEC, MapSyncNetworking::handleFullRequest);
        registrar.playBidirectional(PlayerIconPayload.TYPE, PlayerIconPayload.STREAM_CODEC, MapSyncNetworking::handlePlayerIcon);
        registrar.playToClient(S2CFullStateChunkedPayload.TYPE, S2CFullStateChunkedPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations())));
        registrar.playToClient(S2CPageDiffBatchPayload.TYPE, S2CPageDiffBatchPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations())));
    }

    private static void bootstrapServerState() {
        if (serverBootstrapped) return;
        serverBootstrapped = true;
        final PageIO pageIO = MapwrightClient.PAGE_MANAGER.pageIO;
        if (pageIO == null) return;
        final Map<Integer, NativeImage> images = pageIO.tryLoadAllPages();
        final List<MapSyncOperation> preload = new ArrayList<>();
        for (var e : images.entrySet()) {
            final int rx = e.getKey() >> 16;
            final int ry = (short) (e.getKey() & 0xFFFF);
            final NativeImage image = e.getValue();
            for (int x = 0; x < MapwrightClient.CHUNK_SIZE; x++) for (int y = 0; y < MapwrightClient.CHUNK_SIZE; y++) {
                final int rgba = image.getPixelRGBA(x, y);
                if (rgba != 0) preload.add(new MapSyncOperation(rx * MapwrightClient.CHUNK_SIZE + x, ry * MapwrightClient.CHUNK_SIZE + y, rgba, 0, "", -1L));
            }
            image.close();
        }
        SERVER_STATE.apply(preload);
        SERVER_STATE.drainDiffs();
    }

    private static void handleMapSync(final MapSyncPayload payload, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp) {
            bootstrapServerState();
            SERVER_STATE.apply(payload.operations());
            if (!payload.operations().isEmpty()) PacketDistributor.sendToAllPlayers(new S2CPageDiffBatchPayload(payload.operations()));
            flushServerDiffs(sp);
            return;
        }
        context.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations()));
    }

    private static void handlePinSync(final PinSyncPayload payload, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp) {
            PacketDistributor.sendToAllPlayers(payload);
            return;
        }
        context.enqueueWork(() -> PinSyncBridge.applyRemote(payload.operations()));
    }


    private static void handlePlayerIcon(final PlayerIconPayload payload, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer) {
            PacketDistributor.sendToAllPlayers(payload);
            return;
        }
        context.enqueueWork(() -> PlayerIconSyncBridge.update(payload.playerId(), payload.x(), payload.z(), payload.yaw()));
    }

    private static void handleFullRequest(final C2SRequestFullStatePayload payload, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer sp)) return;
        bootstrapServerState();
        final List<MapSyncOperation> out = new ArrayList<>(CHUNK_SIZE);
        for (var e: SERVER_STATE.pixels().entrySet()) {
            out.add(new MapSyncOperation((int)(e.getKey() >> 32), (int)(long)e.getKey(), e.getValue(), 0, "", -1L));
            if (out.size() >= CHUNK_SIZE) { PacketDistributor.sendToPlayer(sp, new S2CFullStateChunkedPayload(List.copyOf(out))); out.clear(); }
        }
        if (!out.isEmpty()) PacketDistributor.sendToPlayer(sp, new S2CFullStateChunkedPayload(List.copyOf(out)));
    }

    public static void flushClientPending() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        for(int i=0;i<4;i++) {
            final var ops = MapSyncBridge.drainPending(CLIENT_BATCH);
            if (ops.isEmpty()) break;
            PacketDistributor.sendToServer(new MapSyncPayload(ops));
        }
        final var pinOps = PinSyncBridge.drainPending();
        if (!pinOps.isEmpty()) PacketDistributor.sendToServer(new PinSyncPayload(pinOps));
    }


    public static void sendLocalPlayerIcon() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        final var p = mc.player;
        PacketDistributor.sendToServer(new PlayerIconPayload(p.getUUID(), p.getX(), p.getZ(), p.getYRot()));
    }

    public static void requestSnapshot() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) PacketDistributor.sendToServer(new C2SRequestFullStatePayload());
    }

    public static void flushServerDiffs(final ServerPlayer source) {
        final long now = System.currentTimeMillis();
        if (!SERVER_STATE.shouldFlush(now, CHUNK_SIZE, DEBOUNCE_MS)) return;
        final var diffs = SERVER_STATE.drainDiffs();
        SERVER_STATE.markFlushed(now);
        if (!diffs.isEmpty()) PacketDistributor.sendToAllPlayers(new S2CPageDiffBatchPayload(diffs));
    }
}
