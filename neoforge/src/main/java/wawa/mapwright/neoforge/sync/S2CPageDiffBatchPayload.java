package wawa.mapwright.neoforge.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wawa.mapwright.MapwrightClient;
import wawa.mapwright.data.sync.MapSyncOperation;

import java.util.List;

public record S2CPageDiffBatchPayload(List<MapSyncOperation> operations) implements CustomPacketPayload {
    public static final Type<S2CPageDiffBatchPayload> TYPE = new Type<>(MapwrightClient.id("s2c_page_diff_batch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPageDiffBatchPayload> STREAM_CODEC = StreamCodec.composite(
            MapSyncPayload.OP_CODEC.apply(ByteBufCodecs.list()), S2CPageDiffBatchPayload::operations, S2CPageDiffBatchPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
