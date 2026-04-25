package gg.glacier.command;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.ColorUtil;
import gg.glacier.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GlacierCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB = Arrays.asList("help", "alerts", "reload", "vl", "info", "top");

    private final Glacier plugin;

    public GlacierCommand(Glacier plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help":    help(sender); break;
            case "alerts":  alerts(sender); break;
            case "reload":  reload(sender); break;
            case "vl":      vl(sender, args); break;
            case "info":    info(sender); break;
            case "top":     top(sender); break;
            default:        help(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String sub : SUB) if (sub.startsWith(args[0].toLowerCase())) out.add(sub);
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("vl")) {
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            return out;
        }
        return new ArrayList<>();
    }

    private void help(CommandSender s) {
        send(s, "&8&m-----&r &bGlacier &8&m-----");
        send(s, "&b/glacier alerts       &7toggle staff alerts");
        send(s, "&b/glacier vl <player>  &7show a player's violations");
        send(s, "&b/glacier top          &7top ten current violators");
        send(s, "&b/glacier reload       &7reload config.yml");
        send(s, "&b/glacier info         &7plugin info");
    }

    private void alerts(CommandSender s) {
        if (!(s instanceof Player)) { send(s, "&cConsole already receives alerts."); return; }
        Player p = (Player) s;
        boolean on = plugin.alertManager().toggle(p);
        send(s, on ? "&aAlerts enabled." : "&cAlerts disabled.");
    }

    private void reload(CommandSender s) {
        if (!s.hasPermission("glacier.command")) { send(s, "&cNo permission."); return; }
        plugin.reload();
        send(s, "&aConfig reloaded.");
    }

    private void vl(CommandSender s, String[] args) {
        if (args.length < 2) { send(s, "&cUsage: /glacier vl <player>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { send(s, "&cPlayer not online."); return; }
        PlayerData d = plugin.dataManager().get(target);
        if (d == null) { send(s, "&cNo data."); return; }

        send(s, "&8&m-----&r &b" + target.getName() + " &8&m-----");
        for (Check c : d.checks()) {
            String col = c.vl() <= 0 ? "&7" : c.vl() >= c.maxVl() ? "&c" : "&e";
            send(s, col + c.id() + " &8(" + c.category() + ") &7vl=" + col + fmt(c.vl()) + "&7/" + c.maxVl());
        }
    }

    private void info(CommandSender s) {
        send(s, "&bGlacier &7v" + plugin.getDescription().getVersion()
                + " &8· &7" + plugin.checkManager().count() + " checks");
    }

    private void top(CommandSender s) {
        List<String[]> rows = new ArrayList<>();
        for (PlayerData d : plugin.dataManager().all()) {
            double sum = 0;
            String worst = "-";
            double worstVl = 0;
            for (Check c : d.checks()) {
                sum += c.vl();
                if (c.vl() > worstVl) { worstVl = c.vl(); worst = c.id(); }
            }
            if (sum <= 0 || d.player() == null) continue;
            rows.add(new String[]{ d.player().getName(), fmt(sum), worst });
        }
        rows.sort(Comparator.comparingDouble((String[] r) -> -Double.parseDouble(r[1])));

        send(s, "&8&m-----&r &bGlacier top &8&m-----");
        if (rows.isEmpty()) { send(s, "&7(no violators)"); return; }
        int n = Math.min(10, rows.size());
        for (int i = 0; i < n; i++) {
            String[] r = rows.get(i);
            send(s, "&8" + (i + 1) + ". &b" + r[0] + " &7sum=" + r[1] + " &8· worst=&7" + r[2]);
        }
    }

    private static void send(CommandSender s, String msg) { s.sendMessage(ColorUtil.color(msg)); }
    private static String fmt(double v) { return MathUtil.formatVl(v); }
}
