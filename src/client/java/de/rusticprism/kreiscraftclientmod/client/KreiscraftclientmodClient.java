package de.rusticprism.kreiscraftclientmod.client;

import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class KreiscraftclientmodClient implements ClientModInitializer {

    private static final Logger log = LogManager.getLogger(KreiscraftclientmodClient.class);
    private boolean freecam = false;
    private boolean freecaminstalled = false;
    private String cheats = "false";
    private final List<String> cheatsList = new ArrayList<>();
    public static ServerAddress address;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(PluginMessagePacket.ID, PluginMessagePacket.CODEC);
        checkForFreecam();
        log.info("Successfully initialized KreiscraftClientMod");
        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
            readCheats();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                ModMetadata meta = mod.getMetadata();
                for (String str : cheatsList) {
                    if (meta.getId().equalsIgnoreCase(str)) {
                        cheats = meta.getId();
                    }
                    if (meta.getId().replaceAll("-", "").equalsIgnoreCase(str)) {
                        cheats = str;
                    }
                }
                if (meta.getId().contains("freecam")) {
                    freecaminstalled = true;
                    File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "freecam.json5");
                    try {
                        freecam = JsonParser.parseReader(new FileReader(file)).getAsJsonObject()
                                .getAsJsonObject("collision").getAsJsonPrimitive("ignoreAll").getAsBoolean();
                    } catch (FileNotFoundException e) {
                        log.error(e.fillInStackTrace());
                        return;
                    }
                }
            }
            PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeString("cheats:" + cheats);
            ClientPlayNetworking.send(new PluginMessagePacket(buf));
            buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeString("freecam:" + freecam);
            ClientPlayNetworking.send(new PluginMessagePacket(buf));
        }));
    }

    public void readCheats() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("list/cheats.txt");
        if (inputStream == null) {
            log.error("File not found!");
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        reader.lines().forEach(cheatsList::add);
    }

    public void checkForFreecam() {
        AtomicInteger i = new AtomicInteger();
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.getNetworkHandler() == null) {
                return;
            }
            log.atDebug().log(i);
            if (i.get() >= 200) {
                checkForXray();
                if (!freecaminstalled) {
                    return;
                }
                File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "freecam.json5");
                try {
                    if (JsonParser.parseReader(new FileReader(file)).getAsJsonObject().getAsJsonObject("collision")
                            .getAsJsonPrimitive("ignoreAll").getAsBoolean()) {
                        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
                        buf.writeString("freecam:true");
                        ClientPlayNetworking.send(new PluginMessagePacket(buf));
                    }
                } catch (FileNotFoundException e) {
                    log.error(e.fillInStackTrace());
                    return;
                }
                i.set(0);
            }
            i.getAndIncrement();
        });
    }

    public void checkForXray() {
        MinecraftClient.getInstance().getResourcePackManager().getEnabledProfiles().forEach(s -> {
            log.atDebug().log(s);
            if (s.getId().toLowerCase().contains("xray") || s.getId().toLowerCase().contains("x-ray")) {
                PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
                buf.writeString("xray:" + s.getId().replaceAll("file/", ""));
                ClientPlayNetworking.send(new PluginMessagePacket(buf));
                return;
            }
            if (s.getDescription().getLiteralString() != null
                    && (s.getDescription().getLiteralString().toLowerCase().contains("xray")
                            || s.getDescription().getLiteralString().toLowerCase().contains("x-ray"))) {
                PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
                buf.writeString("xray:" + s.getId().replaceAll("file/", ""));
                ClientPlayNetworking.send(new PluginMessagePacket(buf));
            }
        });
    }
}
