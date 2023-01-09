package com.sxtanna.mc.micro;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.sxtanna.mc.micro.cmds.CommandHome;
import com.sxtanna.mc.micro.data.Home;

import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.util.*;

import static java.util.Optional.ofNullable;

public final class MicroHomesPlugin extends JavaPlugin implements Listener {

    private static MicroHomesPlugin instance;

    public static @NotNull MicroHomesPlugin getInstance() {
        return Objects.requireNonNull(instance, "plugin is not initialized");
    }

    public static @NotNull Logger logger() {
        return getInstance().getSLF4JLogger();
    }


    private static final TypeToken<List<Home>> HOMES_TYPE_TOKEN = new TypeToken<>() {
    };

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
                                                      .enableComplexMapKeySerialization()
                                                      .serializeSpecialFloatingPointValues()
                                                      .create();


    @NotNull
    private final Map<UUID, Map<String, Home>> cache = new HashMap<>();
    @NotNull
    private final NamespacedKey                space = new NamespacedKey(this, "homes");


    private BukkitTask          autoSavingTask;
    private PaperCommandManager commandManager;


    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.commandManager = new PaperCommandManager(this);

        this.initializeCommandManager(this.commandManager);

        this.commandManager.registerCommand(new CommandHome());


        getServer().getPluginManager().registerEvents(this, this);


        getServer().getOnlinePlayers()
                   .forEach(this::loadHomesFromPlayer);

        this.autoSavingTask = getServer().getScheduler()
                                         .runTaskTimer(this,
                                                       () -> getServer().getOnlinePlayers()
                                                                        .forEach(player -> this.saveHomesIntoPlayer(player, false)),
                                                       20L * 10L,
                                                       20L * (60L * 5L));
    }

    @Override
    public void onDisable() {
        this.commandManager.unregisterCommands();
        this.commandManager = null;

        this.autoSavingTask.cancel();
        this.autoSavingTask = null;

        HandlerList.unregisterAll((Plugin) this);


        getServer().getOnlinePlayers()
                   .forEach(this::saveHomesIntoPlayer);

        this.cache.clear();


        instance = null;
    }


    public @NotNull Optional<Home> findHomeByName(@NotNull final UUID user, @NotNull final String name) {
        final var homes = this.cache.get(user);

        return homes == null || homes.isEmpty() ?
               Optional.empty() :
               ofNullable(homes.get(Home.referenceableName(name)));
    }

    public @NotNull @UnmodifiableView Collection<Home> findHomesByUser(@NotNull final UUID user) {
        final var homes = this.cache.get(user);

        return homes == null || homes.isEmpty() ?
               Collections.emptyList() :
               Collections.unmodifiableCollection(homes.values());
    }


    public boolean insertHome(@NotNull final UUID user, @NotNull final Home home) {
        final var homes = this.cache.computeIfAbsent(user, $ -> new HashMap<>());

        if (homes.containsKey(home.nameAsReference())) {
            return false;
        }

        homes.put(home.nameAsReference(), home);

        return true;
    }

    public @NotNull Optional<Home> removeHome(@NotNull final UUID user, @NotNull final Home home) {
        final var homes = this.cache.get(user);

        return homes == null || !homes.remove(home.nameAsReference(), home) ?
               Optional.empty() :
               Optional.of(home);
    }

    public @NotNull Optional<Home> removeHome(@NotNull final UUID user, @NotNull final String name) {
        final var homes = this.cache.get(user);

        return homes == null ?
               Optional.empty() :
               ofNullable(homes.remove(Home.referenceableName(name)));
    }


    @EventHandler
    private void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        loadHomesFromPlayer(event.getPlayer());
    }

    @EventHandler
    private void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        saveHomesIntoPlayer(event.getPlayer());
    }


    private void loadHomesFromPlayer(@NotNull final Player player) {
        final var json = player.getPersistentDataContainer()
                               .get(this.space, PersistentDataType.STRING);
        if (json == null) {
            return;
        }

        final List<Home> decoded;

        try {
            decoded = GSON.fromJson(json, HOMES_TYPE_TOKEN);
        } catch (final Throwable ex) {
            logger().error("could not decode homes for player {}", player.getUniqueId(), ex);
            return;
        }

        final var homes = this.cache.computeIfAbsent(player.getUniqueId(), $ -> new HashMap<>());

        for (final var home : decoded) {
            homes.put(home.nameAsReference(), home);
        }
    }

    private void saveHomesIntoPlayer(@NotNull final Player player) {
        saveHomesIntoPlayer(player, true);
    }

    private void saveHomesIntoPlayer(@NotNull final Player player, final boolean remove) {
        final var homes = !remove ? this.cache.get(player.getUniqueId()) : this.cache.remove(player.getUniqueId());
        if (homes == null || homes.isEmpty()) {
            return;
        }

        final String encoded;

        try {
            encoded = GSON.toJson(List.copyOf(homes.values()), HOMES_TYPE_TOKEN.getType());
        } catch (final Throwable ex) {
            logger().error("could not encode homes for player {}", player.getUniqueId(), ex);
            return;
        }

        player.getPersistentDataContainer()
              .set(this.space, PersistentDataType.STRING, encoded);
    }


    private void initializeCommandManager(@NotNull final PaperCommandManager commandManager) {
        // commandManager.enableUnstableAPI("help");
        // commandManager.enableUnstableAPI("brigadier");
        // commandManager.usePerIssuerLocale(true, true);

        commandManager.getCommandCompletions().registerCompletion("homes", ctx -> {
            final var player = ctx.getPlayer();

            return player == null ?
                   Collections.emptyList() :
                   ofNullable(this.cache.get(player.getUniqueId()))
                           .map(Map::keySet)
                           .map(Collections::unmodifiableSet)
                           .orElse(Collections.emptySet());
        });

        commandManager.getCommandContexts().registerContext(Home.class, ctx -> {
            final var player = ctx.getPlayer();
            if (player == null) {
                return null;
            }

            final var homes = this.cache.get(player.getUniqueId());
            if (homes == null || homes.isEmpty()) {
                return null;
            }

            final var arg = ctx.popFirstArg();
            if (arg != null && !"home".equalsIgnoreCase(arg)) {
                return homes.get(Home.referenceableName(arg));
            }

            final var home = homes.get("home");
            if (home != null) {
                return home;
            }

            if (homes.size() == 1) {
                return homes.values().iterator().next();
            }

            return null;
        });
    }

}
