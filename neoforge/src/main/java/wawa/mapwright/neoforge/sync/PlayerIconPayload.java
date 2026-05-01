package wawa.mapwright.neoforge.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import wawa.mapwright.MapwrightClient;

import java.util.UUID;

public record PlayerIconPayload(UUID playerId, double x, double z, float yaw) implements CustomPacketPayload {
    public static final Type<PlayerIconPayload> TYPE = new Type<>(MapwrightClient.id("player_icon_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerIconPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtilCodec.CODEC, PlayerIconPayload::playerId,
            ByteBufCodecs.DOUBLE, PlayerIconPayload::x,
            ByteBufCodecs.DOUBLE, PlayerIconPayload::z,
            ByteBufCodecs.FLOAT, PlayerIconPayload::yaw,
            PlayerIconPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    private static final class UUIDUtilCodec { private static final StreamCodec<RegistryFriendlyByteBuf, UUID> CODEC = ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString); }
}
