package wawa.mapwright.data.sync;

public record MapSyncOperation(int x, int y, int rgba, String authorId, long strokeId) {
    public MapSyncOperation(final int x, final int y, final int rgba) {
        this(x, y, rgba, "", -1L);
    }
}
