package wawa.mapwright.data.sync;

public record MapSyncOperation(int x, int y, int rgba, int previousRgba, String authorId, long strokeId) {
    public MapSyncOperation(final int x, final int y, final int rgba) {
        this(x, y, rgba, 0, "", -1L);
    }
}
