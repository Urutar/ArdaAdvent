package de.ardania.urutar.ardaadvent.domain;


import de.ardania.urutar.ardaadvent.ArdaAdvent;
import de.ardania.urutar.ardaadvent.handler.CalendarManager;
import de.ardania.urutar.ardaadvent.handler.MessageHandler;
import de.ardania.urutar.ardaadvent.util.DateChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class PlayerCalendar {

    protected final CalendarManager man;
    protected final Player player;
    protected final Map<ItemStack, Integer> doors = new HashMap<>();
    protected final Inventory calendar;
    public int genDay;
    protected int open;
    protected final ArdaAdvent plugin;

    public PlayerCalendar(ArdaAdvent plugin, CalendarManager man, Player player, int openStatus) {
        this.plugin = plugin;
        this.man = man;
        this.player = player;
        this.open = openStatus;
        this.calendar = Bukkit.createInventory(player, 54, Messages.INV_TITLE);
        this.calendar.setMaxStackSize(1);
        generateCalendar();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PlayerCalendar)) {
            return false;
        }
        PlayerCalendar cal = (PlayerCalendar) obj;
        return this.player.getUniqueId().equals(cal.getPlayer().getUniqueId());
    }

    @Override
    public int hashCode() {
        return this.player.getUniqueId().hashCode();
    }

    protected boolean isUp2Date() {
        return this.genDay == DateChecker.getDayOfMonth();
    }

    protected void generateCalendar() {
        this.doors.clear();
        this.calendar.setStorageContents(FILL);
        this.genDay = DateChecker.getDayOfMonth();
        ItemStack door;
        ItemMeta meta;
        for (int d = 1; d <= 24; d++) {
            door = new ItemStack(isOpen(d)
                    ? DOOR_OPEN
                    : (d == 24)
                    ? DOOR_24
                    : DOOR_CLOSED);
            meta = door.getItemMeta();
            if (d > this.genDay) {
                String lore = String.format((d == 24)
                                ? Messages.ITEM_24
                                : Messages.ITEM_FUTURE,
                        d - this.genDay,
                        (d - this.genDay == 1) ? "" : "en");
                meta.setLore(Collections.singletonList(lore));
            } else if (d == 24) {
                meta.setLore(Collections.singletonList(Messages.ITEM_24_READY));
            }

            meta.setDisplayName(String.format(Messages.ITEM_NAME, d));
            door.setItemMeta(meta);

            this.doors.put(door, d);
            this.calendar.setItem(this.man.getDoor(d).getPosition(), door);
        }
    }

    public boolean isValidDoor(ItemStack i) {
        return isCalendarDoor(i)
                && i.hasItemMeta()
                && this.doors.containsKey(i);
    }

    public int getClickedDay(ItemStack ci) {
        return this.doors.get(ci);
    }

    public boolean isCalendar(Inventory i) {
        return this.calendar.equals(i);
    }

    public void open(ItemStack i, int day) {
        if (!this.man.getDoor(day).reward(player)) {
            MessageHandler.showMessage(player, Messages.MSG_MISSING_CONTENT);
        } else {
            this.setOpen(day);
            this.plugin.log(Level.INFO, player.getName() + " opened door " + day);
            this.doors.remove(i);
            i.setType(Material.CHEST);
            ItemMeta meta = i.getItemMeta();
            meta.setLore(Collections.singletonList(Messages.ITEM_OPENED));
            i.setItemMeta(meta);
            this.doors.put(i, day);
            this.player.playSound(this.player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        }
    }

    public boolean hasClosedDoors() {
        int day = 0, b = 0;
        while (canOpen(++day)) {
            if (!isOpen(day)) {
                return true;
            }
        }
        return false;
    }

    public void setOpen(int day) {
        this.open |= 0b1 << --day;
    }

    public void set(int day, boolean open) {
        // 0b000111
        // 0b010010
        // 0b010101
    }

    public boolean isOpen(int day) {
        return ((this.open >> --day) & 0b1) == 0b1;
    }

    public boolean isFullyOpened() {
        return 0b1111_1111_1111_1111_1111_1111 == this.open;
    }

    public boolean canOpen(int day) {
        return day <= this.genDay && day <= 24;
    }

    public Inventory getCalendar() {
        if (!isUp2Date()) {
            generateCalendar();
        }
        return this.calendar;
    }

    public Player getPlayer() {
        return player;
    }

    public int getOpenStatus() {
        return this.open;
    }

    @Override
    public String toString() {
        return String.format("Calendar[%s]=%b", this.player.getName(), this.open);
    }

    protected final static ItemStack SAPLING;
    protected final static ItemStack[] FILL;
    protected final static ItemStack DOOR_OPEN;
    protected final static ItemStack DOOR_CLOSED;
    protected final static ItemStack DOOR_24;

    static {
        SAPLING = new ItemStack(Material.SPRUCE_SAPLING, 1);
        ItemMeta meta = SAPLING.getItemMeta();
        meta.setDisplayName(" ");
        SAPLING.setItemMeta(meta);

        int dChestSize = 54;
        Inventory calendar = Bukkit.createInventory(null, dChestSize);
        for (int i = 0; i < dChestSize; i++) {
            calendar.setItem(i, SAPLING);
        }
        FILL = calendar.getStorageContents();

        DOOR_OPEN = new ItemStack(Material.CHEST);
        DOOR_CLOSED = new ItemStack(Material.SPRUCE_DOOR);
        DOOR_24 = new ItemStack(Material.DARK_OAK_DOOR);
        meta.setLore(Collections.singletonList(Messages.ITEM_OPENED));
        DOOR_OPEN.setItemMeta(meta);
        meta.setLore(Collections.singletonList(Messages.ITEM_CLOSED));
        DOOR_CLOSED.setItemMeta(meta);
    }

    public static boolean isAir(ItemStack i) {
        return i.getType().equals(Material.AIR) || i.getAmount() == 0;
    }

    public static boolean isCalendarDoor(ItemStack i) {
        return !isAir(i) && !PlayerCalendar.SAPLING.isSimilar(i);
    }
}
