package com.sxtanna.mc.micro.data;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public record Home(@NotNull String name, @NotNull UUID world, double x, double y, double z, float yaw, float pitch) {

    public static @NotNull String referenceableName(@NotNull final String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }


    public @NotNull String nameAsReference() {
        return referenceableName(this.name());
    }

    public @NotNull Optional<Location> toBukkitLocation() {
        return Optional.of(this.world())
                       .map(Bukkit::getWorld)
                       .map(world -> new Location(world,
                                                  this.x(),
                                                  this.y(),
                                                  this.z(),
                                                  Location.normalizeYaw(this.yaw()),
                                                  Location.normalizePitch(this.pitch())));
    }

}
