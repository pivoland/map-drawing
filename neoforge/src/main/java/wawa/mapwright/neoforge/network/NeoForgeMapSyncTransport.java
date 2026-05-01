package wawa.mapwright.neoforge.network;

import wawa.mapwright.network.IMapSyncTransport;
import wawa.mapwright.network.MapSyncPacket;

public class NeoForgeMapSyncTransport implements IMapSyncTransport {
    @Override
    public void register() {
        // Packet registration is loader-specific and intentionally isolated here.
    }

    @Override
    public boolean isServerMapwrightAvailable() {
        return false;
    }

    @Override
    public void sendToServer(final MapSyncPacket packet) {
        // Serialization and send are intentionally isolated for NeoForge implementation.
    }
}
