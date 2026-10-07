package com.killtospawn.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class KillToSpawnClient implements ClientModInitializer {
    private static boolean enabled = true;
    private static long nextAllowed = 0L;

    private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
        new KeyBinding(
            "key.killtospawn.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.killtospawn"
        )
    );

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!enabled || client.player == null) return;

            String msg = message.getString();
            String me = client.player.getName().getString();
            if (isKillMessage(msg, me)) {
                long now = System.currentTimeMillis();
                if (now >= nextAllowed) {
                    nextAllowed = now + 750;
                    client.player.networkHandler.sendChatCommand("spawn");
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("Kill To Spawn: " + (enabled ? "§aACTIVADO" : "§cDESACTIVADO")),
                        true
                    );
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("killspawn")
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    enabled = true;
                    ctx.getSource().sendFeedback(Text.literal("Kill To Spawn: §aACTIVADO"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    enabled = false;
                    ctx.getSource().sendFeedback(Text.literal("Kill To Spawn: §cDESACTIVADO"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    enabled = !enabled;
                    ctx.getSource().sendFeedback(Text.literal(
                        "Kill To Spawn: " + (enabled ? "§aACTIVADO" : "§cDESACTIVADO")
                    ));
                    return 1;
                }))
            );
        });
    }

    private static boolean isKillMessage(String message, String username) {
        String s = message.toLowerCase(Locale.ROOT);
        String u = username.toLowerCase(Locale.ROOT);

        boolean mentionsMeAsKiller =
            s.contains(" by " + u) ||
            s.contains(" por " + u) ||
            s.endsWith(" " + u);

        if (!mentionsMeAsKiller) return false;

        String[] deathWords = {
            "slain", "killed", "shot", "fireballed", "blown up", "fell",
            "drowned", "burned", "pummeled", "impaled", "stung", "frozen",
            "magic", "asesinado", "matado", "mato", "mató", "murió",
            "eliminado", "eliminó", "derrotado", "derrotó", "kill"
        };

        for (String word : deathWords) {
            if (s.contains(word)) return true;
        }
        return false;
    }
}
