package wawa.mapwright.neoforge.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wawa.mapwright.MapwrightClient;

public record C2SRequestFullStatePayload() implements CustomPacketPayload {
    public static final Type<C2SRequestFullStatePayload> TYPE = new Type<>(MapwrightClient.id("c2s_request_full_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestFullStatePayload> STREAM_CODEC = StreamCodec.unit(new C2SRequestFullStatePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
