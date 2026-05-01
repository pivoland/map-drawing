package wawa.mapwright.data.sync;

import org.joml.Vector2d;

import java.util.*;

public final class PlayerIconSyncBridge {
    public record RemotePlayerIcon(UUID playerId, Vector2d pos, float yaw) {}
    private static final Map<UUID, RemotePlayerIcon> remote = new HashMap<>();

    private PlayerIconSyncBridge() {}

    public static synchronized void update(final UUID id, final double x, final double z, final float yaw) {
        remote.put(id, new RemotePlayerIcon(id, new Vector2d(x, z), yaw));
    }

    public static synchronized Collection<RemotePlayerIcon> getAll() { return List.copyOf(remote.values()); }
    public static synchronized void clear() { remote.clear(); }
}
