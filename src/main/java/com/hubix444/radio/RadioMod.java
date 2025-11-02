package com.hubix444.radio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public static abstract class AbstractRadioBlockEntity extends BlockEntity {
    public int channel = 101;
    public boolean isTransmitter = false;
    public long lastTxTick = 0;

    public AbstractRadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}

public class RadioMod implements ModInitializer {
    public static final String MODID = "radiomod";

    public static Block RADIO_TRANSMITTER;
    public static Block RADIO_RECEIVER;

    public static Item CASSETTE_AUDIO;
    public static Item CASSETTE_VIDEO;
    // =============================
// === BLOKI CYFROWE ===
// =============================
    public static Block DIGITAL_TRANSMITTER;
    public static Block DIGITAL_RECEIVER;

    // =============================
// === ITEMY CYFROWE ===
// =============================
    public static Item DIGITAL_TRANSMITTER_ITEM;
    public static Item DIGITAL_RECEIVER_ITEM;

    // =============================
// === BLOCK ENTITY CYFROWE ===
// =============================
    public static BlockEntityType<DigitalBlockEntity> DIGITAL_BE_TYPE;

    // =============================
// === SCREEN HANDLERY CYFROWE ===
// =============================
    public static ScreenHandlerType<GenericScreenHandler> DIGITAL_SCREEN_TYPE;


    public static BlockEntityType<RadioBlockEntity> RADIO_BE_TYPE;
    public static ScreenHandlerType<GenericScreenHandler> RADIO_SCREEN_TYPE;

    public static final Map<Integer, List<WeakReference<RadioBlockEntity>>> ETHER = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        // === BLOKI ===
        // Nadajnik z GUI
        RADIO_TRANSMITTER = Registry.register(Registries.BLOCK, id("radio_transmitter"),
                new RadioTransmitterBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(2.0f)
                        .sounds(BlockSoundGroup.ANVIL)));


        // odbiornik ma GUI
        RADIO_RECEIVER = Registry.register(Registries.BLOCK, id("radio_receiver"),
                new RadioReceiverBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.0f).sounds(BlockSoundGroup.ANVIL)));

        // === ITEMY ===
        CASSETTE_AUDIO = Registry.register(Registries.ITEM, id("cassette_audio"), new Item(new FabricItemSettings()));
        CASSETTE_VIDEO = Registry.register(Registries.ITEM, id("cassette_video"), new Item(new FabricItemSettings()));

        Registry.register(Registries.ITEM, id("radio_transmitter"), new BlockItem(RADIO_TRANSMITTER, new FabricItemSettings()));
        Registry.register(Registries.ITEM, id("radio_receiver"), new BlockItem(RADIO_RECEIVER, new FabricItemSettings()));

        // === BLOCK ENTITY ===
        RADIO_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("radio_be"),
                FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, RADIO_TRANSMITTER, RADIO_RECEIVER).build(null));

        DIGITAL_BE_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("digital_be"),
                FabricBlockEntityTypeBuilder.create(DigitalBlockEntity::new, DIGITAL_TRANSMITTER, DIGITAL_RECEIVER).build(null));


        RADIO_SCREEN_TYPE = ScreenHandlerRegistry.registerSimple(id("generic_radio_screen"),
                (syncId, inv) -> new GenericScreenHandler(syncId));

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            ETHER.values().forEach(list -> list.removeIf(ref -> ref.get() == null));
        });
        // =============================
        // === BLOKI CYFROWE ===
        // =============================
        DIGITAL_TRANSMITTER = Registry.register(Registries.BLOCK, id("digital_transmitter"),
                new DigitalTransmitterBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(2.0f)
                        .sounds(BlockSoundGroup.METAL)));

        DIGITAL_RECEIVER = Registry.register(Registries.BLOCK, id("digital_receiver"),
                new DigitalReceiverBlock(AbstractBlock.Settings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .strength(2.0f)
                        .sounds(BlockSoundGroup.METAL)));

        // =============================
        // === ITEMY BLOKÓW CYFROWYCH ===
        // =============================
        Registry.register(Registries.ITEM, id("digital_transmitter"),
                new BlockItem(DIGITAL_TRANSMITTER, new FabricItemSettings()));

        Registry.register(Registries.ITEM, id("digital_receiver"),
                new BlockItem(DIGITAL_RECEIVER, new FabricItemSettings()));

        // =============================
        // === BLOCK ENTITY CYFROWE ===
        // =============================
        // Uwaga: możesz użyć tego samego typu BE co RADIO_BE_TYPE albo stworzyć osobny typ, jeśli chcesz

        // =============================
        // === SCREEN HANDLERY CYFROWE ===
        // =============================
        // Rejestracja GUI dla nadajnika
        RADIO_SCREEN_TYPE = ScreenHandlerRegistry.registerSimple(id("digital_transmitter_screen"),
                (syncId, inv) -> new GenericScreenHandler(syncId));

        System.out.println("[RadioMod] initialized");
    }

    private static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    // =============================
    // === BLOCK ENTITY RADIA ===
    // =============================
    public static class RadioBlockEntity extends BlockEntity {
        public int channel = 101;
        public boolean isTransmitter = false;
        public long lastTxTick = 0;

        public RadioBlockEntity(BlockPos pos, BlockState state) {
            super(RADIO_BE_TYPE, pos, state);
        }

        @Override
        public void readNbt(NbtCompound tag) {
            super.readNbt(tag);
            this.channel = tag.getInt("channel");
            this.isTransmitter = tag.getBoolean("tx");
        }

        @Override
        public void writeNbt(NbtCompound tag) {
            tag.putInt("channel", channel);
            tag.putBoolean("tx", isTransmitter);
            super.writeNbt(tag);
        }

            public void onLoad() {
            if (!world.isClient && isTransmitter) {
                ETHER.computeIfAbsent(channel, k -> new ArrayList<>())
                        .add(new WeakReference<>(this));
            }
        }

        @Override
        public void markRemoved() {
            super.markRemoved();
            if (!world.isClient && isTransmitter) {
                List<WeakReference<RadioBlockEntity>> list = ETHER.get(channel);
                if (list != null) {
                    list.removeIf(ref -> ref.get() == this);
                }
            }
        }

    }
    // =============================
// === GUI NADAJNIKA ===
// =============================
    @Environment(EnvType.CLIENT)
    public static class TransmitterScreen extends Screen {
        private final BlockPos pos;
        private int channel = 101;
        private ButtonWidget plusButton;
        private ButtonWidget minusButton;

        protected TransmitterScreen(BlockPos pos) {
            super(Text.of("Radio Transmitter"));
            this.pos = pos;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int bottomY = this.height - 40;
            this.plusButton = ButtonWidget.builder(Text.of("+"), b -> changeChannel(1))
                    .dimensions(centerX + 40, bottomY, 20, 20).build();
            this.minusButton = ButtonWidget.builder(Text.of("−"), b -> changeChannel(-1))
                    .dimensions(centerX - 60, bottomY, 20, 20).build();
            this.addDrawableChild(plusButton);
            this.addDrawableChild(minusButton);
        }

        private void changeChannel(int delta) {
            channel += delta;
            if (channel < 1) channel = 1;
            if (channel > 999) channel = 999;

            // wyślij zmianę do BE
            if (MinecraftClient.getInstance().world != null) {
                var world = MinecraftClient.getInstance().world;
                if (world.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                    be.channel = channel;

                    // aktualizuj ETHER (rejestracja na nowym kanale)
                    ETHER.computeIfAbsent(channel, k -> new ArrayList<>())
                            .add(new WeakReference<>(be));
                }
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            context.drawTextWithShadow(this.textRenderer, "Radio Transmitter", 10, 10, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "Channel: " + channel, 10, 25, 0x00FF00);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    // =============================
    // === BLOK ODBIORNIKA ===
    // =============================
    public static class RadioReceiverBlock extends Block implements BlockEntityProvider {
        public RadioReceiverBlock(Settings settings) {
            super(settings);
        }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return new RadioBlockEntity(pos, state);
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(new SdrScreen(pos));
            }
            return ActionResult.SUCCESS;
        }
    }

    // =============================
    // === SCREEN HANDLER ===
    // =============================
    public static class GenericScreenHandler extends ScreenHandler {
        public GenericScreenHandler(int syncId) {
            super(RADIO_SCREEN_TYPE, syncId);
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }
    }
    // =============================
// === BLOK NADAJNIKA ===
// =============================
    public static class RadioTransmitterBlock extends Block implements BlockEntityProvider {
        public RadioTransmitterBlock(Settings settings) {
            super(settings);
        }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            var be = new RadioBlockEntity(pos, state);
            be.isTransmitter = true;
            return be;
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(new TransmitterScreen(pos));
            }
            return ActionResult.SUCCESS;
        }
    }
// =============================
// === BLOCK ENTITY CYFROWE ===
// =============================

    public static class DigitalBlockEntity extends BlockEntity {
        public int channel = 101;
        public boolean isTransmitter = false;
        public String messageToSend = "";
        public long lastTxTick = 0;
        public String lastReceived = "";

        public DigitalBlockEntity(BlockPos pos, BlockState state) {
            super(RADIO_BE_TYPE, pos, state);
        }

        @Override
        public void readNbt(NbtCompound tag) {
            super.readNbt(tag);
            this.channel = tag.getInt("channel");
            this.isTransmitter = tag.getBoolean("tx");
            this.messageToSend = tag.getString("msg");
            this.lastReceived = tag.getString("received");
        }

        @Override
        public void writeNbt(NbtCompound tag) {
            tag.putInt("channel", channel);
            tag.putBoolean("tx", isTransmitter);
            tag.putString("msg", messageToSend);
            tag.putString("received", lastReceived);
            super.writeNbt(tag);
        }

        public void onLoad() {
            if (!world.isClient && isTransmitter) {
                ETHER.computeIfAbsent(channel, k -> new ArrayList<>())
                        .add(new WeakReference<>(this));
            }
        }

        @Override
        public void markRemoved() {
            super.markRemoved();
            if (!world.isClient && isTransmitter) {
                List<WeakReference<RadioBlockEntity>> list = ETHER.get(channel);
                if (list != null) {
                    list.removeIf(ref -> ref.get() == this);
                }
            }
        }

        public void sendMessage() {
            if (!isTransmitter || world.isClient || messageToSend.isEmpty()) return;

            List<WeakReference<RadioBlockEntity>> list = ETHER.get(channel);
            if (list == null) return;

            for (WeakReference<RadioBlockEntity> ref : list) {
                var rx = ref.get();
                if (rx instanceof DigitalBlockEntity digitalRx) {
                    digitalRx.lastReceived = messageToSend;
                }
            }

            lastTxTick = world.getTime();
        }
    }

    // =============================
// === BLOK NADAJNIKA CYFROWEGO ===
// =============================
    public static class DigitalTransmitterBlock extends Block implements BlockEntityProvider {
        public DigitalTransmitterBlock(Settings settings) { super(settings); }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            var be = new DigitalBlockEntity(pos, state);
            be.isTransmitter = true;
            return be;
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(new DigitalTransmitterScreen(pos));
            }
            return ActionResult.SUCCESS;
        }
    }

    // =============================
// === BLOK ODBIORNIKA CYFROWEGO ===
// =============================
    public static class DigitalReceiverBlock extends Block implements BlockEntityProvider {
        public DigitalReceiverBlock(Settings settings) { super(settings); }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return new DigitalBlockEntity(pos, state);
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(new DigitalReceiverScreen(pos));
            }
            return ActionResult.SUCCESS;
        }
    }

    // =============================
// === GUI DIGITAL TRANSMITTER ===
// =============================
    @Environment(EnvType.CLIENT)
    public static class DigitalTransmitterScreen extends Screen {
        private final BlockPos pos;
        private String message = "";

        protected DigitalTransmitterScreen(BlockPos pos) {
            super(Text.of("Digital Transmitter"));
            this.pos = pos;
        }

        @Override
        protected void init() {
            addDrawableChild(ButtonWidget.builder(Text.of("Send"), b -> sendMessage())
                    .dimensions(width/2 - 40, height - 40, 80, 20).build());
        }

        private void sendMessage() {
            if (MinecraftClient.getInstance().world != null) {
                var be = MinecraftClient.getInstance().world.getBlockEntity(pos);
                if (be instanceof DigitalBlockEntity digitalBe) {
                    digitalBe.messageToSend = message;
                    digitalBe.sendMessage();
                }
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            context.drawTextWithShadow(this.textRenderer, "Digital Transmitter", 10, 10, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "Message: " + message, 10, 25, 0x00FF00);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }

    // =============================
// === GUI DIGITAL RECEIVER ===
// =============================
    @Environment(EnvType.CLIENT)
    public static class DigitalReceiverScreen extends Screen {
        private final BlockPos pos;

        protected DigitalReceiverScreen(BlockPos pos) {
            super(Text.of("Digital Receiver"));
            this.pos = pos;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            if (MinecraftClient.getInstance().world != null) {
                var be = MinecraftClient.getInstance().world.getBlockEntity(pos);
                if (be instanceof DigitalBlockEntity digitalBe) {
                    context.drawTextWithShadow(this.textRenderer, "Received: " + digitalBe.lastReceived, 10, 10, 0x00FFFF);
                }
            }
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }

    // =============================
    // === GUI SDR ODBIORNIKA ===
    // =============================
    @Environment(EnvType.CLIENT)
    public static class SdrScreen extends Screen {
        private final BlockPos pos;
        private final Deque<float[]> waterfall = new ArrayDeque<>();
        private final int bins = 128;
        private int channel = 101;
        private ButtonWidget plusButton;
        private ButtonWidget minusButton;

        protected SdrScreen(BlockPos pos) {
            super(Text.of("SDR Receiver"));
            this.pos = pos;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int bottomY = this.height - 40;
            this.plusButton = ButtonWidget.builder(Text.of("+"), b -> changeChannel(1))
                    .dimensions(centerX + 40, bottomY, 20, 20).build();
            this.minusButton = ButtonWidget.builder(Text.of("−"), b -> changeChannel(-1))
                    .dimensions(centerX - 60, bottomY, 20, 20).build();
            this.addDrawableChild(plusButton);
            this.addDrawableChild(minusButton);
        }

        private void changeChannel(int delta) {
            channel += delta;
            if (channel < 1) channel = 1;
            if (channel > 999) channel = 999;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            int w = this.width;
            int h = this.height;

            // Tytuł i częstotliwość
            context.drawTextWithShadow(this.textRenderer, "SDR Receiver", 10, 10, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, "Channel: " + channel, 10, 25, 0x00FF00);

            // Wygeneruj sygnał z "pikami"
            float[] spectrum = generateSpikySpectrum();

            int specH = h / 2 - 10;
            for (int i = 0; i < bins; i++) {
                int x = 10 + i * (w - 20) / bins;
                int y = 50 + specH - (int) (spectrum[i] * specH);
                context.fill(x, 50 + specH, x + (w - 20) / bins - 1, 50 + specH + 2, 0xFFAAAAAA);
                context.fill(x, y, x + (w - 20) / bins - 1, 50 + specH, 0xFF00FF00);
            }

            pushWaterfallRow(spectrum);
            int wfTop = 60 + specH;
            int rowH = Math.max(1, (h - wfTop - 40) / 64);
            int rowIndex = 0;
            for (float[] row : waterfall) {
                int y = wfTop + rowIndex * rowH;
                for (int i = 0; i < bins; i++) {
                    int x = 10 + i * (w - 20) / bins;
                    int val = clampColor((int) (row[i] * 255));
                    int color = (0xFF << 24) | (val << 16);
                    context.fill(x, y, x + (w - 20) / bins - 1, y + rowH, color);
                }
                rowIndex++;
                if (rowIndex > 64) break;
            }

            super.render(context, mouseX, mouseY, delta);
        }

        private void pushWaterfallRow(float[] row) {
            waterfall.addFirst(Arrays.copyOf(row, row.length));
            while (waterfall.size() > 128) waterfall.removeLast();
        }

        private float[] generateSpikySpectrum() {
            float[] s = new float[bins];
            Arrays.fill(s, 0.05f); // szum tła

            if (MinecraftClient.getInstance().world == null) return s;
            var receiverWorld = MinecraftClient.getInstance().world;
            var receiverPos = pos;

            double time = System.currentTimeMillis() / 1000.0;

            // Pasmo obejmuje np. kanały od -2 do +2 względem aktualnego
            for (int offset = -2; offset <= 2; offset++) {
                int nearbyChannel = channel + offset;
                if (nearbyChannel < 1 || nearbyChannel > 999) continue;

                List<WeakReference<RadioBlockEntity>> list = ETHER.get(nearbyChannel);
                if (list == null) continue;

                for (WeakReference<RadioBlockEntity> ref : list) {
                    RadioBlockEntity tx = ref.get();
                    if (tx == null || tx.getWorld() == null || tx.getWorld() != receiverWorld) continue;

                    // Odległość i zasięg
                    double dx = tx.getPos().getX() - receiverPos.getX();
                    double dy = tx.getPos().getY() - receiverPos.getY();
                    double dz = tx.getPos().getZ() - receiverPos.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double range = 128.0;
                    if (dist > range) continue;

                    float strength = (float) (1.0 - (dist / range));
                    if (strength < 0) strength = 0;

                    // Każdy kanał przesunięty o określoną pozycję w spektrum
                    int baseIndex = bins / 2 + offset * (bins / 8);
                    baseIndex = Math.max(0, Math.min(bins - 1, baseIndex));

                    float peak = strength * (0.8f + 0.2f * (float) Math.sin(time * 2.0));
                    for (int i = Math.max(0, baseIndex - 2); i < Math.min(bins, baseIndex + 3); i++) {
                        float distToCenter = Math.abs(i - baseIndex) / 3f;
                        s[i] += peak * (1f - distToCenter);
                    }
                }
            }

            for (int i = 0; i < bins; i++) {
                if (s[i] > 1f) s[i] = 1f;
            }

            return s;
        }




        private int clampColor(int v) {
            return Math.max(0, Math.min(255, v));
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
