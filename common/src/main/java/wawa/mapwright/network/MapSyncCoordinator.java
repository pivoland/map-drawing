package wawa.mapwright.network;

import net.minecraft.client.Minecraft;
import wawa.mapwright.MapwrightClient;

import java.util.ArrayList;
import java.util.List;

public class MapSyncCoordinator {
    private static final int FLUSH_INTERVAL_MS = 150;
    private static final int MAX_BATCH_SIZE = 1024;

    private final List<S2CPageDiffBatch.PageDiff> stagedDiffs = new ArrayList<>();
    private long lastFlushMs;
    private boolean localOnlyMode = true;

    public void onClientJoin(final Minecraft client) {
        this.lastFlushMs = System.currentTimeMillis();
        this.localOnlyMode = !MapwrightClient.MAP_SYNC_TRANSPORT.isServerMapwrightAvailable();
        if (!this.localOnlyMode) {
            MapwrightClient.MAP_SYNC_TRANSPORT.sendToServer(new C2SRequestFullState());
        }
    }

    public void stageLocalPixelDiff(final int x, final int y, final int argb) {
        if (this.localOnlyMode) return;
        final int rx = Math.floorDiv(x, MapwrightClient.CHUNK_SIZE);
        final int ry = Math.floorDiv(y, MapwrightClient.CHUNK_SIZE);
        final int localX = x - rx * MapwrightClient.CHUNK_SIZE;
        final int localY = y - ry * MapwrightClient.CHUNK_SIZE;
        this.stagedDiffs.add(new S2CPageDiffBatch.PageDiff(rx, ry, localX, localY, argb));
    }

    public void tick() {
        if (this.localOnlyMode || this.stagedDiffs.isEmpty()) return;
        final long now = System.currentTimeMillis();
        if (this.stagedDiffs.size() >= MAX_BATCH_SIZE || now - this.lastFlushMs >= FLUSH_INTERVAL_MS) {
            MapwrightClient.MAP_SYNC_TRANSPORT.sendToServer(new S2CPageDiffBatch(new ArrayList<>(this.stagedDiffs)));
            this.stagedDiffs.clear();
            this.lastFlushMs = now;
        }
    }

    public void applyFullStateChunk(final S2CFullStateChunked packet) {
        for (final S2CFullStateChunked.PagePayload page : packet.pages()) {
            for (int x = 0; x < MapwrightClient.CHUNK_SIZE; x++) {
                for (int y = 0; y < MapwrightClient.CHUNK_SIZE; y++) {
                    final int pixel = page.argbPixels()[y * MapwrightClient.CHUNK_SIZE + x];
                    MapwrightClient.PAGE_MANAGER.putPixel(page.rx() * MapwrightClient.CHUNK_SIZE + x, page.ry() * MapwrightClient.CHUNK_SIZE + y, pixel);
                }
            }
        }
    }

    public void applyDiffBatch(final S2CPageDiffBatch packet) {
        for (final S2CPageDiffBatch.PageDiff diff : packet.diffs()) {
            MapwrightClient.PAGE_MANAGER.putPixel(diff.rx() * MapwrightClient.CHUNK_SIZE + diff.localX(), diff.ry() * MapwrightClient.CHUNK_SIZE + diff.localY(), diff.argb());
        }
    }
}
