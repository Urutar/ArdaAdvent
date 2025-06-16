package de.ardania.urutar.ardaadvent;

import de.ardania.urutar.ardaadvent.domain.Messages;
import de.ardania.urutar.ardaadvent.domain.PlayerCalendar;
import de.ardania.urutar.ardaadvent.handler.CalendarManager;
import de.ardania.urutar.ardaadvent.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

public class ArdaAdvent extends JavaPlugin implements Listener, CommandExecutor {

    /* fix: give item only in gm 0 */
    //protected final String PERM_admin_peek = "calendar.admin.showAny";
    protected final String PERM_ownAndShow = "calendar.ownAndShow";
    protected final String PERM_openDoors = "calendar.openDoors";

    protected CalendarManager man;

    public void log(Level level, String log) {
        this.getLogger().log(level, log);
    }

    @Override
    public void onEnable() {
        File pluginFolder = this.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        man = new CalendarManager(this,
                new File(pluginFolder, "calendar.cfg"),
                new File(pluginFolder, "players.data")
        );

        try {
            man.load();
        } catch (IOException e) {
            this.log(Level.WARNING, "IO error with save-file while loading: " + e.getLocalizedMessage());
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("adventskalender").setExecutor(this);
    }

    @Override
    public void onDisable() {
        try {
            man.save();
        } catch (IOException e) {
            this.log(Level.WARNING, "IO error with save-file while saving: " + e.getLocalizedMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        Player p = ev.getPlayer();
        if (p.hasPermission(PERM_ownAndShow)) {
            PlayerCalendar cal = man.playerJoin(p);
            if (cal != null && cal.hasClosedDoors()) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    MessageHandler.showMessage(p, Messages.MSG_COMMAND_INFO);
                    if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                        Bukkit.dispatchCommand(p, "advent");
                    }
                }, 20);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        man.playerQuit(ev.getPlayer());
    }

    protected boolean opCommand(Player p, String[] args) {
        if (args[0].equalsIgnoreCase("edit")) {
            if (args.length == 1) {
                Bukkit.getScheduler().runTask(this, () -> this.man.EDITOR.startEditing(p));
            } else {
                try {
                    int day = Integer.parseInt(args[1]);
                    if (day < 1 || day > 24) {
                        throw new NumberFormatException("not a day from the calendar");
                    }
                    switch (args[2].toLowerCase()) {
                        case "clear":
                            this.man.EDITOR.clearCommands(day);
                            break;
                        case "add":
                            this.man.EDITOR.addCommand(day, String.join(" ", Arrays.asList(args).subList(3, args.length)));
                            break;
                        case "test":
                            this.man.getDoor(day).reward(p);
                            break;
                        default:
                            throw new NumberFormatException("wrong usage");
                    }
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    MessageHandler.showMessage(p, "Error: " + e.getMessage());
                    MessageHandler.showMessage(p, "use \"edit [day] clear/add/test [command]\"");
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            try {
                man.load();
            } catch (IOException e) {
                this.log(Level.WARNING, "IO error with save-file while loading: " + e.getLocalizedMessage());
            }
            return true;
        }
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length > 0 && p.isOp()) {
                if (this.opCommand(p, args)) {
                    return true;
                }
            }

            if (p.hasPermission(PERM_ownAndShow)) {
                if (!man.isActive(p)) {
                    MessageHandler.showMessage(p, Messages.MSG_WRONG_TIME);
                } else if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                    Bukkit.getScheduler().runTask(this, () -> p.openInventory(man.get(p).getCalendar()));
                } else {
                    MessageHandler.showMessage(p, Messages.MSG_WRONG_GAMEMODE);
                }
            } else {
                MessageHandler.showMessage(p, Messages.MSG_MISSING_PERM);
            }
            return true;
        } else {
            sender.sendMessage("not supported");
        }
        return false;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent ev) {
        Player p = (Player) ev.getPlayer();
        Inventory ei = ev.getInventory();
        if (!ei.getType().equals(InventoryType.CHEST)) {
            return;
        }
        if (this.man.EDITOR.isEditing(p)) {
            this.man.EDITOR.stopEditing();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ev) {
        Player p = (Player) ev.getWhoClicked();
        Inventory ei = ev.getClickedInventory();
        if (ei == null || !ei.getType().equals(InventoryType.CHEST)) { // || ei.getHolder() == null || !ei.getHolder().equals(p)
            return;
        }
        if (p.isOp() && this.man.EDITOR.isEditing(p)) {
            this.man.EDITOR.edit(ev);
            return;
        }

        PlayerCalendar pc = man.get(p);
        if (pc == null || !pc.isCalendar(ev.getInventory())) {
            return;
        }

        ev.setCancelled(true);
        if (!p.hasPermission(PERM_openDoors)) {
            MessageHandler.showMessage(p, Messages.MSG_MISSING_PERM);
            return;
        }

        ItemStack ci = ev.getCurrentItem();
        if (pc.isValidDoor(ci)) {
            int day = pc.getClickedDay(ci);

            Bukkit.getScheduler().runTask(this, () -> {
                boolean isOpen = pc.isOpen(day),
                        canOpen = pc.canOpen(day);
                this.log(Level.INFO, p.getName() + " tried opening door " + day + " [canOpen=" + canOpen + ", isOpen=" + isOpen + ", calendarGenerationDay=" + pc.genDay + "]");
                if (isOpen) {
                    MessageHandler.showMessage(p, String.format(Messages.MSG_OPEN, day));
                } else if (!canOpen) {
                    MessageHandler.showMessage(p, String.format(Messages.MSG_CLOSED, day));
                } else {
                    pc.open(ci, day);
                }
            });
        }
    }
}