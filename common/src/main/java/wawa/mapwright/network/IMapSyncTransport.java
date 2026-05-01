package wawa.mapwright.network;

public interface IMapSyncTransport {
    void register();
    boolean isServerMapwrightAvailable();
    void sendToServer(MapSyncPacket packet);
}
