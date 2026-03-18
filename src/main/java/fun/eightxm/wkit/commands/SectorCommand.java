package fun.eightxm.wkit.commands;

import fun.eightxm.wkit.WKitPlugin;
import fun.eightxm.wkit.arnis.ArnisCLI;
import fun.eightxm.wkit.arnis.GenerationTask;
import fun.eightxm.wkit.sector.GeoProjection;
import fun.eightxm.wkit.sector.Sector;
import fun.eightxm.wkit.sector.SectorRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SectorCommand implements CommandExecutor {

    private final WKitPlugin plugin;

    public SectorCommand(WKitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /sector <generate|list|tp|info|here>", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "generate" -> handleGenerate(sender, args);
            case "list" -> handleList(sender);
            case "tp" -> handleTeleport(sender, args);
            case "info" -> handleInfo(sender, args);
            case "here" -> handleHere(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand: " + sub, NamedTextColor.RED));
        }
        return true;
    }

    private void handleGenerate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wkit.sector.generate")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        // Parse: /sector generate <bbox> [--name=X] [--terrain] [--scale=N]
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                "Usage: /sector generate <minLat,minLng,maxLat,maxLng> [--name=X] [--terrain] [--no-terrain] [--scale=N]",
                NamedTextColor.YELLOW));
            return;
        }

        String bboxStr = args[1];
        String[] bboxParts = bboxStr.split(",");
        if (bboxParts.length != 4) {
            sender.sendMessage(Component.text("Invalid bbox. Format: minLat,minLng,maxLat,maxLng", NamedTextColor.RED));
            return;
        }

        double minLat, minLng, maxLat, maxLng;
        try {
            minLat = Double.parseDouble(bboxParts[0]);
            minLng = Double.parseDouble(bboxParts[1]);
            maxLat = Double.parseDouble(bboxParts[2]);
            maxLng = Double.parseDouble(bboxParts[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid coordinates in bbox.", NamedTextColor.RED));
            return;
        }

        // Parse optional flags
        String name = "Sector " + System.currentTimeMillis();
        boolean terrain = plugin.getDefaultTerrain();
        double scale = plugin.getDefaultScale();

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--name=")) {
                name = arg.substring(7);
            } else if (arg.equals("--terrain")) {
                terrain = true;
            } else if (arg.equals("--no-terrain")) {
                terrain = false;
            } else if (arg.startsWith("--scale=")) {
                try {
                    scale = Double.parseDouble(arg.substring(8));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid scale value.", NamedTextColor.RED));
                    return;
                }
            }
        }

        // Check Arnis binary
        ArnisCLI arnis = new ArnisCLI(plugin.getArnisBinaryPath(), plugin.getLogger());
        if (!arnis.isAvailable()) {
            sender.sendMessage(Component.text("Arnis binary not found at " + plugin.getArnisBinaryPath(), NamedTextColor.RED));
            return;
        }

        // Preview size
        GeoProjection proj = plugin.getGeoProjection();
        int[] size = proj.bboxSize(minLat, minLng, maxLat, maxLng);
        sender.sendMessage(Component.text("Estimated sector size: " + size[0] + "x" + size[1] + " blocks", NamedTextColor.AQUA));

        // Create sector and start generation
        Sector sector = new Sector(name, minLat, minLng, maxLat, maxLng);
        sector.setScale(scale);
        sector.setGroundLevel(plugin.getDefaultGroundLevel());
        sector.setTerrain(terrain);

        sender.sendMessage(Component.text("Starting generation for '" + name + "'...", NamedTextColor.GREEN));

        // Run async
        GenerationTask task = new GenerationTask(plugin, sector,
            sender instanceof Player p ? p.getUniqueId() : null);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private void handleList(CommandSender sender) {
        Collection<Sector> sectors = plugin.getSectorRegistry().getAll();
        if (sectors.isEmpty()) {
            sender.sendMessage(Component.text("No sectors generated yet.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("=== Sectors (" + sectors.size() + ") ===", NamedTextColor.GOLD));
        for (Sector s : sectors) {
            sender.sendMessage(Component.text(
                " " + s.getName() + " [" + s.getId() + "] - " + s.getStatus()
                + " (" + s.getMcMinX() + "," + s.getMcMinZ() + " → " + s.getMcMaxX() + "," + s.getMcMaxZ() + ")",
                s.getStatus() == Sector.Status.LIVE ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sector tp <name>", NamedTextColor.YELLOW));
            return;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Sector sector = plugin.getSectorRegistry().findByNameOrId(query);
        if (sector == null) {
            sender.sendMessage(Component.text("Sector not found: " + query, NamedTextColor.RED));
            return;
        }

        if (sector.getStatus() != Sector.Status.LIVE) {
            sender.sendMessage(Component.text("Sector is not live yet: " + sector.getStatus(), NamedTextColor.YELLOW));
            return;
        }

        World world = Bukkit.getWorld(plugin.getWorldName());
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + plugin.getWorldName(), NamedTextColor.RED));
            return;
        }

        int[] center = sector.getMcCenter();
        int y = world.getHighestBlockYAt(center[0], center[1]) + 2;
        Location loc = new Location(world, center[0] + 0.5, y, center[1] + 0.5);
        player.teleport(loc);
        sender.sendMessage(Component.text("Teleported to " + sector.getName(), NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sector info <name>", NamedTextColor.YELLOW));
            return;
        }

        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Sector sector = plugin.getSectorRegistry().findByNameOrId(query);
        if (sector == null) {
            sender.sendMessage(Component.text("Sector not found: " + query, NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== " + sector.getName() + " [" + sector.getId() + "] ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Status: " + sector.getStatus(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Geo: " + sector.getMinLat() + "," + sector.getMinLng()
            + " → " + sector.getMaxLat() + "," + sector.getMaxLng(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("MC: " + sector.getMcMinX() + "," + sector.getMcMinZ()
            + " → " + sector.getMcMaxX() + "," + sector.getMcMaxZ(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Scale: " + sector.getScale() + " | Terrain: " + sector.isTerrain(), NamedTextColor.WHITE));

        if (sector.getGeneratedAt() > 0) {
            sender.sendMessage(Component.text("Generated: " + new java.util.Date(sector.getGeneratedAt()), NamedTextColor.GRAY));
        }
    }

    private void handleHere(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return;
        }

        Location loc = player.getLocation();
        Sector sector = plugin.getSectorRegistry().findByCoords(loc.getBlockX(), loc.getBlockZ());
        if (sector == null) {
            // Show coords anyway
            GeoProjection proj = plugin.getGeoProjection();
            double[] latLng = proj.toLatlng(loc.getBlockX(), loc.getBlockZ());
            sender.sendMessage(Component.text("Not in any sector. Geo: " + String.format("%.6f", latLng[0]) + ", " + String.format("%.6f", latLng[1]), NamedTextColor.YELLOW));
            return;
        }

        GeoProjection proj = plugin.getGeoProjection();
        double[] latLng = proj.toLatlng(loc.getBlockX(), loc.getBlockZ());
        sender.sendMessage(Component.text("Sector: " + sector.getName() + " | Geo: "
            + String.format("%.6f", latLng[0]) + ", " + String.format("%.6f", latLng[1]), NamedTextColor.GREEN));
    }
}
