package wawa.mapwright.neoforge.sync;

import wawa.mapwright.data.sync.MapSyncOperation;

import java.util.*;

public final class ServerMapState {
    private final Map<Long, Integer> pixels = new HashMap<>();
    private final List<MapSyncOperation> diffBuffer = new ArrayList<>();
    private long lastFlushMs = System.currentTimeMillis();

    public void apply(final List<MapSyncOperation> ops) {
        for (final MapSyncOperation op : ops) {
            pixels.put(pack(op.x(), op.y()), op.rgba());
            diffBuffer.add(op);
        }
    }

    public Map<Long,Integer> pixels(){ return pixels; }
    public List<MapSyncOperation> drainDiffs() { final List<MapSyncOperation> out = List.copyOf(diffBuffer); diffBuffer.clear(); return out; }
    public boolean shouldFlush(final long now, final int maxPayload, final long debounceMs) { return !diffBuffer.isEmpty() && (diffBuffer.size() >= maxPayload || now - lastFlushMs >= debounceMs); }
    public void markFlushed(long now){ lastFlushMs = now; }

    private static long pack(final int x, final int y){ return (((long)x)<<32) | (y & 0xffffffffL); }
}
