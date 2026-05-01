package wawa.mapwright.neoforge.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import wawa.mapwright.MapwrightClient;
import wawa.mapwright.data.sync.MapSyncBridge;
import wawa.mapwright.data.sync.MapSyncOperation;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = MapwrightClient.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MapSyncNetworking {
    private static final ServerMapState SERVER_STATE = new ServerMapState();
    private static final int CHUNK_SIZE = 1024;
    private static final int CLIENT_BATCH = 512;
    private static final int DEBOUNCE_MS = 70;

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("2");
        registrar.playBidirectional(MapSyncPayload.TYPE, MapSyncPayload.STREAM_CODEC, MapSyncNetworking::handleMapSync);
        registrar.playToServer(C2SRequestFullStatePayload.TYPE, C2SRequestFullStatePayload.STREAM_CODEC, MapSyncNetworking::handleFullRequest);
        registrar.playToClient(S2CFullStateChunkedPayload.TYPE, S2CFullStateChunkedPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations())));
        registrar.playToClient(S2CPageDiffBatchPayload.TYPE, S2CPageDiffBatchPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations())));
    }

    private static void handleMapSync(final MapSyncPayload payload, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp) {
            SERVER_STATE.apply(payload.operations());
            flushServerDiffs(sp);
            return;
        }
        context.enqueueWork(() -> MapSyncBridge.applyRemoteOperations(payload.operations()));
    }

    private static void handleFullRequest(final C2SRequestFullStatePayload payload, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer sp)) return;
        final List<MapSyncOperation> out = new ArrayList<>(CHUNK_SIZE);
        for (var e: SERVER_STATE.pixels().entrySet()) {
            out.add(new MapSyncOperation((int)(e.getKey() >> 32), (int)(long)e.getKey(), e.getValue(), "", -1L));
            if (out.size() >= CHUNK_SIZE) { PacketDistributor.sendToPlayer(sp, new S2CFullStateChunkedPayload(List.copyOf(out))); out.clear(); }
        }
        if (!out.isEmpty()) PacketDistributor.sendToPlayer(sp, new S2CFullStateChunkedPayload(List.copyOf(out)));
    }

    public static void flushClientPending() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        final var ops = MapSyncBridge.drainPending(CLIENT_BATCH);
        if (!ops.isEmpty()) PacketDistributor.sendToServer(new MapSyncPayload(ops));
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
        if (!diffs.isEmpty()) PacketDistributor.sendToPlayersTrackingEntityAndSelf(source, new S2CPageDiffBatchPayload(diffs));
    }
}
