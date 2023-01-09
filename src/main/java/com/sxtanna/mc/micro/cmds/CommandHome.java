package com.sxtanna.mc.micro.cmds;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.sxtanna.mc.micro.MicroHomesPlugin;
import com.sxtanna.mc.micro.data.Home;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;

import java.text.NumberFormat;
import java.util.Locale;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@CommandAlias("homes|home|h")
public final class CommandHome extends BaseCommand {

    @Default
    @CommandCompletion("@homes")
    public void home(@NotNull final Player sender, @Default("home") @Nullable final Home home) {
        if (home == null) {

            sender.sendMessage(text("Could not find home to teleport to!")
                                       .color(RED));

            return;
        }

        final var location = home.toBukkitLocation().orElse(null);
        if (location == null) {

            sender.sendMessage(text("Could not resolve location of home named ")
                                       .color(RED)
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(home.name())
                                                       .color(YELLOW))
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text("!")
                                                       .color(RED)));

            return;
        }

        sender.teleportAsync(location, PlayerTeleportEvent.TeleportCause.COMMAND)
              .whenComplete((state, error) -> {
                  if (state == null || !state) {
                      var message = text("Could not teleport to home ")
                              .color(RED)
                              .append(text("'")
                                              .color(GRAY))
                              .append(text(home.name())
                                              .color(YELLOW))
                              .append(text("'")
                                              .color(GRAY))
                              .append(text("!")
                                              .color(RED));

                      if (error != null) {
                          message = message.append(newline())
                                           .append(text(error.getMessage() != null ?
                                                        error.getMessage() :
                                                        "Unknown Reason")
                                                           .color(RED));
                      }

                      sender.sendMessage(message);

                      return;
                  }

                  sender.sendMessage(text("Teleported to home ")
                                             .color(GREEN)
                                             .append(text("'")
                                                             .color(GRAY))
                                             .append(text(home.name())
                                                             .color(YELLOW))
                                             .append(text("'")
                                                             .color(GRAY)));
              });
    }


    @Subcommand("set")
    @CommandAlias("sethome|shome|sh")
    public void set_home(@NotNull final Player sender, @Optional @Nullable final String name) {
        final var location = sender.getLocation();

        final var home = new Home(name != null ? name : "home",
                                  location.getWorld().getUID(),

                                  location.x(),
                                  location.y(),
                                  location.z(),

                                  location.getYaw(),
                                  location.getPitch());


        final var result = MicroHomesPlugin.getInstance()
                                           .insertHome(sender.getUniqueId(), home);

        if (!result) {
            sender.sendMessage(text("You already have a home named ")
                                       .color(RED)
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(home.name())
                                                       .color(WHITE))
                                       .append(text("'")
                                                       .color(GRAY)));
        } else {
            sender.sendMessage(text("Home ")
                                       .color(GREEN)
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(home.name())
                                                       .color(WHITE))
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(" has been created at ")
                                                       .color(GREEN))
                                       .append(homeLocationAsHumanFriendly(sender.locale(), home)));
        }
    }


    @Subcommand("del")
    @CommandAlias("delhome|dhome|dh")
    @CommandCompletion("@homes")
    public void del_home(@NotNull final Player sender, @Nullable final Home home) {
        if (home == null) {

            sender.sendMessage(text("Could not find home to remove!")
                                       .color(RED));

            return;
        }

        final var result = MicroHomesPlugin.getInstance()
                                           .removeHome(sender.getUniqueId(), home);

        if (result.isEmpty()) {
            sender.sendMessage(text("You don't have a home named ")
                                       .color(RED)
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(home.name())
                                                       .color(WHITE))
                                       .append(text("'")
                                                       .color(GRAY)));
        } else {
            sender.sendMessage(text("Home ")
                                       .color(GREEN)
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(home.name())
                                                       .color(WHITE))
                                       .append(text("'")
                                                       .color(GRAY))
                                       .append(text(" at ")
                                                       .color(GREEN))
                                       .append(homeLocationAsHumanFriendly(sender.locale(), home))
                                       .append(text(" has been removed")
                                                       .color(GREEN)));
        }
    }


    private static @NotNull Component homeLocationAsHumanFriendly(@NotNull final Locale locale, @NotNull final Home home) {
        final var format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(true);
        format.setMaximumFractionDigits(2);

        return text("[")
                .color(DARK_GRAY)

                .append(text("x: ")
                                .color(GRAY))
                .append(text(format.format(home.x()))
                                .color(GREEN))

                .append(text(", ")
                                .color(DARK_GRAY))

                .append(text("y: ")
                                .color(GRAY))
                .append(text(format.format(home.y()))
                                .color(GREEN))

                .append(text(", ")
                                .color(DARK_GRAY))

                .append(text("z: ")
                                .color(GRAY))
                .append(text(format.format(home.z()))
                                .color(GREEN))

                .append(text("]")
                                .color(DARK_GRAY));
    }

}
