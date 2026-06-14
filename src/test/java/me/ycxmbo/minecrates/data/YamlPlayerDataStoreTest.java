package me.ycxmbo.minecrates.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlPlayerDataStoreTest {

    private static final Logger LOG = Logger.getLogger("MineCratesTest");

    @Test
    void roundTripPreservesAllFields(@TempDir Path tmp) {
        YamlPlayerDataStore store = new YamlPlayerDataStore(tmp.toFile(), LOG);
        UUID id = UUID.randomUUID();

        PlayerData data = new PlayerData(id);
        data.setOpened(42);
        data.setLastReward("legendary_sword");
        data.addVirtualKeys("mystic", 5);
        data.setCooldownUntil("mystic", 1_700_000_000_000L);
        data.setPityCounter("mystic", 12);

        store.save(data);

        PlayerData loaded = store.load(id);
        assertEquals(42, loaded.opened());
        assertEquals("legendary_sword", loaded.lastReward());
        assertEquals(5, loaded.virtualKeys("mystic"));
        assertEquals(1_700_000_000_000L, loaded.cooldownUntil("mystic"));
        assertEquals(12, loaded.pityCounter("mystic"));

        store.close();
    }

    @Test
    void loadingUnknownPlayerReturnsEmptyRecord(@TempDir Path tmp) {
        YamlPlayerDataStore store = new YamlPlayerDataStore(tmp.toFile(), LOG);
        UUID id = UUID.randomUUID();

        PlayerData loaded = store.load(id);
        assertEquals(0, loaded.opened());
        assertEquals("", loaded.lastReward());
        assertTrue(loaded.virtualKeys().isEmpty());
        assertEquals(0, loaded.virtualKeys("anything"));

        store.close();
    }

    @Test
    void negativeVirtualKeysAreClampedAndRemoved(@TempDir Path tmp) {
        YamlPlayerDataStore store = new YamlPlayerDataStore(tmp.toFile(), LOG);
        UUID id = UUID.randomUUID();

        PlayerData data = new PlayerData(id);
        data.addVirtualKeys("k", 3);
        data.addVirtualKeys("k", -3);
        assertTrue(data.virtualKeys().isEmpty());

        store.save(data);
        PlayerData loaded = store.load(id);
        assertEquals(0, loaded.virtualKeys("k"));

        File f = new File(tmp.toFile(), id + ".yml");
        assertTrue(f.exists());

        store.close();
    }
}
