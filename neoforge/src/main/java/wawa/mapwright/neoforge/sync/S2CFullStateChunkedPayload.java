package wawa.mapwright.neoforge.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wawa.mapwright.MapwrightClient;
import wawa.mapwright.data.sync.MapSyncOperation;

import java.util.List;

public record S2CFullStateChunkedPayload(List<MapSyncOperation> operations) implements CustomPacketPayload {
    public static final Type<S2CFullStateChunkedPayload> TYPE = new Type<>(MapwrightClient.id("s2c_full_state_chunked"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CFullStateChunkedPayload> STREAM_CODEC = StreamCodec.composite(
            MapSyncPayload.OP_CODEC.apply(ByteBufCodecs.list()), S2CFullStateChunkedPayload::operations, S2CFullStateChunkedPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
