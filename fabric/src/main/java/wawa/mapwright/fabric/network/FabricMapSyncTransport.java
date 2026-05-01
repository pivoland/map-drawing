package wawa.mapwright.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import wawa.mapwright.network.IMapSyncTransport;
import wawa.mapwright.network.MapSyncPacket;

public class FabricMapSyncTransport implements IMapSyncTransport {
    @Override
    public void register() {
        // Packet registration is loader-specific and intentionally isolated here.
    }

    @Override
    public boolean isServerMapwrightAvailable() {
        return ClientPlayNetworking.canSend(wawa.mapwright.MapwrightClient.id("request_full_state"));
    }

    @Override
    public void sendToServer(final MapSyncPacket packet) {
        // Serialization and send are intentionally isolated for Fabric implementation.
    }
}
