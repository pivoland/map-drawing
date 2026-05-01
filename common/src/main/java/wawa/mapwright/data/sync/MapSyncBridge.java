package wawa.mapwright.data.sync;

import wawa.mapwright.MapwrightClient;

import java.util.ArrayList;
import java.util.List;

public final class MapSyncBridge {
    private static final int MAX_BATCH_SIZE = 8192;
    private static final List<MapSyncOperation> pending = new ArrayList<>();
    private static boolean applyingRemote = false;

    private MapSyncBridge() {}

    public static synchronized void queueLocalOperation(final int x, final int y, final int rgba, final int previousRgba, final String authorId, final long strokeId) {
        if (applyingRemote) return;
        if (pending.size() >= MAX_BATCH_SIZE) pending.remove(0);
        pending.add(new MapSyncOperation(x, y, rgba, previousRgba, authorId, strokeId));
    }

    public static synchronized List<MapSyncOperation> drainPending(final int maxCount) {
        if (pending.isEmpty() || maxCount <= 0) return List.of();
        final int count = Math.min(maxCount, pending.size());
        final List<MapSyncOperation> copy = new ArrayList<>(count);
        for (int i = 0; i < count; i++) copy.add(pending.remove(0));
        return copy;
    }

    public static synchronized boolean isApplyingRemote() { return applyingRemote; }

    public static synchronized void applyRemoteOperations(final List<MapSyncOperation> operations) {
        if (operations.isEmpty()) return;
        applyingRemote = true;
        try {
            for (final MapSyncOperation op : operations) {
                MapwrightClient.PAGE_MANAGER.putPixel(op.x(), op.y(), op.rgba(), op.authorId(), op.strokeId());
            }
        } finally { applyingRemote = false; }
    }

    public static synchronized void clear() {
        pending.clear();
        applyingRemote = false;
    }
}
