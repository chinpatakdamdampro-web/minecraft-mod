package com.pvpbot;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registers all PvP bot commands.
 */
public class PvpBotCommands {

    private static final List<PvpBotEntity> activeBots = new ArrayList<>();

    public static void register() {
        PvpBotEntities.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<ServerCommandSource> pvpbotNode = dispatcher.register(
                    CommandManager.literal("pvpbot")
                            .then(CommandManager.literal("spawn")
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .executes(ctx -> safeExecute(ctx,
                                                    () -> spawnBot(ctx,
                                                            StringArgumentType.getString(ctx, "name"))))))
                            .then(CommandManager.literal("attack")
                                    .then(CommandManager.argument("botName", StringArgumentType.word())
                                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                                    .executes(ctx -> safeExecute(ctx,
                                                            () -> attackPlayer(ctx,
                                                                    StringArgumentType.getString(ctx, "botName"),
                                                                    EntityArgumentType.getPlayer(ctx, "player")))))))
                            .then(CommandManager.literal("stop")
                                    .then(CommandManager.argument("botName", StringArgumentType.word())
                                            .executes(ctx -> safeExecute(ctx,
                                                    () -> stopBot(ctx,
                                                            StringArgumentType.getString(ctx, "botName"))))))
                            .then(CommandManager.literal("remove")
                                    .then(CommandManager.argument("botName", StringArgumentType.word())
                                            .executes(ctx -> safeExecute(ctx,
                                                    () -> removeBot(ctx,
                                                            StringArgumentType.getString(ctx, "botName"))))))
                            .then(CommandManager.literal("list")
                                    .executes(ctx -> safeExecute(ctx, () -> listBots(ctx))))
            );

            dispatcher.register(CommandManager.literal("spawnbot67")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> safeExecute(ctx,
                                    () -> spawnBot(ctx, StringArgumentType.getString(ctx, "name")))))
                    .executes(ctx -> safeExecute(ctx, () -> spawnBot(ctx, "bot67"))));

            dispatcher.register(CommandManager.literal("botattack67")
                    .then(CommandManager.argument("botName", StringArgumentType.word())
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .executes(ctx -> safeExecute(ctx,
                                            () -> attackPlayer(ctx,
                                                    StringArgumentType.getString(ctx, "botName"),
                                                    EntityArgumentType.getPlayer(ctx, "player")))))));

            dispatcher.register(CommandManager.literal("botstop67")
                    .then(CommandManager.argument("botName", StringArgumentType.word())
                            .executes(ctx -> safeExecute(ctx,
                                    () -> stopBot(ctx, StringArgumentType.getString(ctx, "botName"))))));

            dispatcher.register(CommandManager.literal("removebot67")
                    .then(CommandManager.argument("botName", StringArgumentType.word())
                            .executes(ctx -> safeExecute(ctx,
                                    () -> removeBot(ctx, StringArgumentType.getString(ctx, "botName"))))));

            dispatcher.register(CommandManager.literal("listbots67")
                    .executes(ctx -> safeExecute(ctx, () -> listBots(ctx))));

            dispatcher.register(CommandManager.literal("pvpbot67").redirect(pvpbotNode));
        });
    }

    private static int spawnBot(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity caller = source.getPlayer();
        if (caller == null) {
            source.sendError(Text.literal("§cYou must be a player to spawn a bot."));
            return 0;
        }

        ServerWorld world = caller.getWorld();
        PvpBotEntity bot = PvpBotEntities.PVP_BOT.create(world, SpawnReason.COMMAND);
        if (bot == null) {
            source.sendError(Text.literal("§cFailed to create bot entity."));
            return 0;
        }

        bot.setBotLabel(name);
        bot.refreshPositionAndAngles(caller.getX(), caller.getY(), caller.getZ(), caller.getYaw(), 0.0f);

        if (!world.spawnEntity(bot)) {
            source.sendError(Text.literal("§cFailed to spawn bot in world."));
            return 0;
        }

        activeBots.add(bot);
        source.sendFeedback(() -> Text.literal("§aSpawned PvP bot §e" + name + " §aat your location!"), true);
        return 1;
    }

    private static int attackPlayer(CommandContext<ServerCommandSource> ctx,
                                    String botName,
                                    ServerPlayerEntity targetPlayer) {
        ServerCommandSource source = ctx.getSource();

        PvpBotEntity bot = findBotByName(botName);
        if (bot == null) {
            source.sendError(Text.literal("§cNo bot named §e" + botName + " §cwas found. Use §f/pvpbot list§c."));
            return 0;
        }

        if (targetPlayer.isRemoved()) {
            source.sendError(Text.literal("§cTarget player is not available."));
            return 0;
        }

        bot.setTargetPlayer(targetPlayer.getUuid());
        source.sendFeedback(() -> Text.literal("§e" + botName + " §ais now targeting §c"
                + targetPlayer.getName().getString() + "§a!"), true);
        return 1;
    }

    private static int stopBot(CommandContext<ServerCommandSource> ctx, String botName) {
        ServerCommandSource source = ctx.getSource();
        PvpBotEntity bot = findBotByName(botName);

        if (bot == null) {
            source.sendError(Text.literal("§cNo bot named §e" + botName + " §cwas found."));
            return 0;
        }

        bot.setTargetPlayer(null);
        source.sendFeedback(() -> Text.literal("§aBot §e" + botName + " §ais now passive."), true);
        return 1;
    }

    private static int removeBot(CommandContext<ServerCommandSource> ctx, String botName) {
        ServerCommandSource source = ctx.getSource();
        PvpBotEntity bot = findBotByName(botName);

        if (bot == null) {
            source.sendError(Text.literal("§cNo bot named §e" + botName + " §cwas found."));
            return 0;
        }

        bot.discard();
        activeBots.remove(bot);
        source.sendFeedback(() -> Text.literal("§aRemoved bot §e" + botName + "§a."), true);
        return 1;
    }

    private static int listBots(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        cleanupBotList();

        if (activeBots.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo active PvP bots. Use §f/pvpbot spawn <name>§e."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("§6Active PvP Bots:\n");
        for (PvpBotEntity bot : activeBots) {
            UUID targetUUID = bot.getTargetPlayerUUID();
            String status = "§7[Passive]";

            if (targetUUID != null && bot.getEntityWorld() instanceof ServerWorld world) {
                ServerPlayerEntity target = world.getPlayerByUuid(targetUUID);
                status = "§cAttacking: " + (target != null ? target.getName().getString() : "§7[offline player]");
            }

            sb.append("  §e").append(bot.getBotLabel()).append(" §f- ").append(status).append("\n");
        }

        source.sendFeedback(() -> Text.literal(sb.toString().trim()), false);
        return activeBots.size();
    }

    private static void cleanupBotList() {
        activeBots.removeIf(bot -> bot == null || bot.isRemoved());
    }

    private static PvpBotEntity findBotByName(String name) {
        cleanupBotList();
        for (PvpBotEntity bot : activeBots) {
            if (bot.getBotLabel().equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    private static int safeExecute(CommandContext<ServerCommandSource> ctx, CommandAction action) {
        try {
            return action.run();
        } catch (Throwable t) {
            ctx.getSource().sendError(Text.literal("§cPvPBot command failed safely: " + t.getClass().getSimpleName()));
            PvpBotMod.LOGGER.error("PvPBot command execution error", t);
            return 0;
        }
    }

    @FunctionalInterface
    private interface CommandAction {
        int run();
    }
}
