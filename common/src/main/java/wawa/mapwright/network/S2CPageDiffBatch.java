package wawa.mapwright.network;

import java.util.List;

public record S2CPageDiffBatch(List<PageDiff> diffs) implements MapSyncPacket {
    public record PageDiff(int rx, int ry, int localX, int localY, int argb) {
    }
}
