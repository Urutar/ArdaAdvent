package de.ardania.urutar.ardaadvent.handler;

import de.ardania.urutar.ardaadvent.ArdaAdvent;
import de.ardania.urutar.ardaadvent.domain.Door;
import de.ardania.urutar.ardaadvent.domain.PlayerCalendar;
import de.ardania.urutar.ardaadvent.util.DateChecker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CalendarManager {

    public final CalendarEditor EDITOR;

    protected final ArdaAdvent plugin;
    protected final File cfgFile, saveFile;
    protected final Map<UUID, Integer> data = Collections.synchronizedMap(new HashMap<>(128));
    protected final Map<UUID, PlayerCalendar> activePlayers = new HashMap<>(32);

    protected FileConfiguration config;
    protected final Door[] doors;

    public CalendarManager(ArdaAdvent plugin, File cfgFile, File saveFile) {
        this.plugin = plugin;
        this.cfgFile = cfgFile;
        this.saveFile = saveFile;
        this.EDITOR = new CalendarEditor(plugin, this);

        this.doors = new Door[24];
        for (int day = 0; day < 24; day++) {
            this.doors[day] = new Door(this.plugin, day + 1);
        }
    }

    public Door getDoor(int day) {
        return this.doors[day - 1];
    }

    public void load() throws IOException {
        long t1 = System.currentTimeMillis();
        this.data.clear();
        this.config = YamlConfiguration.loadConfiguration(this.cfgFile);
        for (int i = 0; i < 24; i++) {
            this.doors[i].update(config);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(this.saveFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                int split = line.indexOf(':');
                if (line.isEmpty() || split == -1) {
                    continue;
                }
                this.data.put(
                        UUID.fromString(line.substring(0, split++)),
                        Integer.parseUnsignedInt(line.substring(split), 16)
                );
            }
        }
        this.plugin.log(Level.INFO, "Loaded (" + this.data.size() + " players) in " + (System.currentTimeMillis() - t1) + "ms");
        this.activePlayers.replaceAll((UUID uuid, PlayerCalendar cal) -> getNewCalendar(cal.getPlayer()));
        //in the case that there is a new player in the data, he will not be added to the active players - use rejoin
    }

    public void save() throws IOException {
        long t1 = System.currentTimeMillis();
        this.activePlayers.forEach((UUID pid, PlayerCalendar cal) -> this.data.put(pid, cal.getOpenStatus()));
        try (FileWriter fw = new FileWriter(this.saveFile, false)) {
            for (Map.Entry<UUID, Integer> entry : this.data.entrySet()) {
                fw.write(String.format("%s:%x\n",
                        entry.getKey().toString(),
                        entry.getValue()
                ));
            }
        }
        this.plugin.log(Level.INFO, "Saved (" + this.data.size() + " players) in " + (System.currentTimeMillis() - t1) + "ms");
    }

    protected void silentSave() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                this.save();
            } catch (IOException e) {
            }
        });
    }

    public boolean isActive(Player p) {
        return this.activePlayers.containsKey(p.getUniqueId());
    }

    public PlayerCalendar get(Player p) {
        return this.activePlayers.get(p.getUniqueId());
    }

    public PlayerCalendar playerJoin(Player p) {
        if (!DateChecker.isLateOpenTime()) {
            return null;
        }
        UUID pid = p.getUniqueId();
        if (this.activePlayers.containsKey(pid)) {
            return this.activePlayers.get(pid);
        } else if (!DateChecker.isNewCalendarTime() && !this.data.containsKey(pid)) {
            return null;
        } else {
            PlayerCalendar calendar = getNewCalendar(p);
            this.activePlayers.put(pid, calendar);
            return calendar;
        }
    }

    public PlayerCalendar getNewCalendar(Player p) {
        return new PlayerCalendar(this.plugin, this, p, this.data.getOrDefault(p.getUniqueId(), 0));
    }

    public void playerQuit(Player p) {
        UUID pid = p.getUniqueId();
        if (this.activePlayers.containsKey(pid)) {
            this.data.put(pid, this.activePlayers.remove(pid).getOpenStatus());
            this.silentSave();
        }
    }
}
