package wawa.mapwright.network;

import java.util.List;

public record S2CFullStateChunked(int chunkIndex, int chunkCount, List<PagePayload> pages) implements MapSyncPacket {
    public record PagePayload(int rx, int ry, int[] argbPixels) {
    }
}
