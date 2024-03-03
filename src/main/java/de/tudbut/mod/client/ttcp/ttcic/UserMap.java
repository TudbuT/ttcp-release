package de.tudbut.mod.client.ttcp.ttcic;

import java.util.UUID;
import java.util.HashMap;
import java.util.OptionalInt;

public class UserMap {
    private HashMap<Integer, Account> byID = new HashMap<>();
    private HashMap<String, Account> byName = new HashMap<>();
    private HashMap<UUID, Account> byUUID = new HashMap<>();

    public Account getByID(int id) {
        return byID.get(id);
    }

    public Account getByName(String name) {
        return byName.get(name);
    }

    public Account getByUUID(String uuid) {
        return byUUID.get(UUID.fromString(uuid));
    }

    public Account getByUUID(UUID uuid) {
        return byUUID.get(uuid);
    }

    public Account put(Account account) {
        if (account.id.isPresent())
            byID.put(account.id.getAsInt(), account);
        byName.put(account.name, account);
        byUUID.put(account.uuid, account);
        return account;
    }

    public Account getOrMake(Integer id, UUID uuid, String name) {
        if (byID.containsKey(id)) {
            Account account = getByID(id);
            account.id = id != null ? OptionalInt.of(id) : OptionalInt.empty();
            account.uuid = uuid;
            account.name = name;
        }
        if (byUUID.containsKey(uuid)) {
            Account account = getByUUID(uuid);
            account.id = id != null ? OptionalInt.of(id) : OptionalInt.empty();
            account.uuid = uuid;
            account.name = name;
        }
        if (byName.containsKey(name)) {
            Account account = getByName(name);
            account.id = id != null ? OptionalInt.of(id) : OptionalInt.empty();
            account.uuid = uuid;
            account.name = name;
        }
        return put(new Account(id, uuid, name));
    }

    public void removeID(int id) {
        getByID(id).id = OptionalInt.empty();
    }
}