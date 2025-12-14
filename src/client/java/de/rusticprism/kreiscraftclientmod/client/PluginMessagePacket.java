package de.rusticprism.kreiscraftclientmod.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Objects;

public record PluginMessagePacket(byte[] data) implements CustomPayload {

    public PluginMessagePacket(ByteBuf buf) {
        this(getWrittenBytes(buf));
    }

    public static Id<PluginMessagePacket> ID = new Id<>(Identifier.of("kreiscraft", "mod_checker"));

    public static PacketCodec<ByteBuf, PluginMessagePacket> CODEC = PacketCodec.ofStatic(
            (buf, value) -> writeBytes(buf, value.data),
            PluginMessagePacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return new Id<>(Identifier.of("kreiscraft", "mod_checker"));
    }

    private static void writeBytes(ByteBuf buf, byte[] v) {
        buf.writeBytes(v);
    }

    private static byte[] getWrittenBytes(ByteBuf buf) {
        byte[] bs = new byte[buf.readableBytes()];
        buf.readBytes(bs);
        return bs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PluginMessagePacket that = (PluginMessagePacket) o;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
