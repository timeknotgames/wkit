package fun.eightxm.wkit.commands;

import fun.eightxm.wkit.WKitPlugin;
import fun.eightxm.wkit.arnis.ArnisCLI;
import fun.eightxm.wkit.sector.GeoProjection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WKitCommand implements CommandExecutor {

    private final WKitPlugin plugin;

    public WKitCommand(WKitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /wkit <status|coords|measure|reload>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status" -> handleStatus(sender);
            case "coords" -> handleCoords(sender);
            case "autogen" -> handleAutoGen(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
        }
        return true;
    }

    private void handleStatus(CommandSender sender) {
        ArnisCLI arnis = new ArnisCLI(plugin.getArnisBinaryPath(), plugin.getLogger());
        GeoProjection proj = plugin.getGeoProjection();

        sender.sendMessage(Component.text("=== WKit Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Arnis binary: " + plugin.getArnisBinaryPath()
            + (arnis.isAvailable() ? " (OK)" : " (NOT FOUND)"),
            arnis.isAvailable() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Origin: Mount Tahoma (" + proj.getOriginLat() + ", " + proj.getOriginLng() + ")", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Scale: " + proj.getScale(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Sectors: " + plugin.getSectorRegistry().getAll().size(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("World: " + plugin.getWorldName(), NamedTextColor.WHITE));
        var pg = plugin.getProceduralGenerator();
        sender.sendMessage(Component.text("Procedural gen: " + (pg.isEnabled() ? "ON" : "OFF")
            + " (" + pg.getPendingCount() + " regions pending)",
            pg.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
    }

    private void handleCoords(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return;
        }

        Location loc = player.getLocation();
        GeoProjection proj = plugin.getGeoProjection();
        double[] latLng = proj.toLatlng(loc.getBlockX(), loc.getBlockZ());

        sender.sendMessage(Component.text("Position: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Geographic: " + String.format("%.6f", latLng[0]) + "°N, "
            + String.format("%.6f", latLng[1]) + "°W", NamedTextColor.GREEN));
    }

    private void handleAutoGen(CommandSender sender) {
        if (!sender.hasPermission("wkit.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        var pg = plugin.getProceduralGenerator();
        pg.setEnabled(!pg.isEnabled());
        sender.sendMessage(Component.text("Procedural generation: " + (pg.isEnabled() ? "ON" : "OFF"),
            pg.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("wkit.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.reloadConfig();
        plugin.getSectorRegistry().load();
        sender.sendMessage(Component.text("WKit config and sector registry reloaded.", NamedTextColor.GREEN));
    }
}
