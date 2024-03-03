package de.tudbut.mod.client.ttcp.ttcic;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import net.minecraft.util.math.Vec3d;

public class Account {
    public OptionalInt id;
    public UUID uuid;
    public String name;

    public Optional<Vec3d> location = Optional.ofNullable(null);

    public Account(Integer id, UUID uuid, String name) {
        this.id = id == null ? OptionalInt.empty() : OptionalInt.of(id);
        this.uuid = uuid;
        this.name = name;
    }

    public Account(Integer id, UUID uuid, String name, Vec3d location) {
        this(id, uuid, name);
        this.location = Optional.ofNullable(location);
    }
}