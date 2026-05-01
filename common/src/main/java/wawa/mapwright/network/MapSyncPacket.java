package wawa.mapwright.network;

public sealed interface MapSyncPacket permits C2SRequestFullState, S2CFullStateChunked, S2CPageDiffBatch {
}
