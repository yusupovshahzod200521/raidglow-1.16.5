package uz.shahzod.raidglow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents {

    public static final KeyBinding TOGGLE_KEY = new KeyBinding(
            "key.raidglow.toggle", GLFW.GLFW_KEY_F10, "key.categories.raidglow");

    // 4 chank = 64 blok (o'zgartirib bo'lmaydi)
    private static final double RANGE = 64.0D;

    private static boolean enabled = false;
    private static final Set<AbstractRaiderEntity> marked =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Mod.EventBusSubscriber(modid = RaidGlowMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.registerKeyBinding(TOGGLE_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = RaidGlowMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                marked.clear();
                enabled = false;
                return;
            }
            while (TOGGLE_KEY.consumeClick()) {
                enabled = !enabled;
                if (!enabled) {
                    clearAll();
                }
                mc.player.displayClientMessage(
                        new StringTextComponent("Raid Glow: " + (enabled ? "ON" : "OFF")), true);
            }
            if (enabled) {
                update(mc);
            }
        }
    }

    private static void update(Minecraft mc) {
        double maxDist = RANGE * RANGE;
        List<AbstractRaiderEntity> near = mc.level.getEntitiesOfClass(
                AbstractRaiderEntity.class, mc.player.getBoundingBox().inflate(RANGE));

        Set<AbstractRaiderEntity> current = new HashSet<>();
        for (AbstractRaiderEntity raider : near) {
            if (raider.isAlive() && mc.player.distanceToSqr(raider) <= maxDist) {
                raider.setGlowing(true);
                marked.add(raider);
                current.add(raider);
            }
        }

        Iterator<AbstractRaiderEntity> it = marked.iterator();
        while (it.hasNext()) {
            AbstractRaiderEntity e = it.next();
            if (!current.contains(e)) {
                e.setGlowing(false);
                it.remove();
            }
        }
    }

    private static void clearAll() {
        for (AbstractRaiderEntity e : marked) {
            e.setGlowing(false);
        }
        marked.clear();
    }
}
