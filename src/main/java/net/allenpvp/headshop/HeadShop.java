package net.allenpvp.headshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class HeadShop extends JavaPlugin implements CommandExecutor {

    private Economy economy;
    private double defaultPrice;
    private int defaultTotal;

    @Override
    public void onEnable() {
        // 保存默認配置
        saveDefaultConfig();

        // 初始化配置
        loadConfig();

        // 設置Vault經濟系統
        if (!setupEconomy()) {
            getLogger().severe("Vault economy plugin not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 註冊指令
        this.getCommand("headshop").setExecutor(this);

        getLogger().info("HeadShop plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HeadShop plugin disabled!");
    }

    private void loadConfig() {
        this.defaultPrice = getConfig().getDouble("default.price", 200.0);
        this.defaultTotal = getConfig().getInt("default.total", 1);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "這個指令只能由玩家使用！");
            return true;
        }

        Player player = (Player) sender;

        // 檢查權限
        if (!player.hasPermission(getConfig().getString("permissions", "headshop.buy"))) {
            player.sendMessage(ChatColor.RED + "您沒有權限使用這個指令！");
            return true;
        }

        // 檢查參數
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "使用方法: /headshop <玩家名稱> [數量]");
            return true;
        }

        String targetPlayerName = args[0];
        int amount = defaultTotal;

        // 解析數量參數
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "數量必須大於0！");
                    return true;
                }
                if (amount > 64) {
                    player.sendMessage(ChatColor.RED + "數量不能超過64！");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "請輸入有效的數量！");
                return true;
            }
        }

        // 計算總價格
        double totalPrice = defaultPrice * amount;

        // 檢查玩家餘額
        if (!economy.has(player, totalPrice)) {
            player.sendMessage(ChatColor.RED + "您的餘額不足！需要 " + totalPrice + " 元，但您只有 " + economy.getBalance(player) + " 元。");
            return true;
        }

        // 檢查目標玩家是否存在
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            player.sendMessage(ChatColor.RED + "玩家 " + targetPlayerName + " 不存在或從未加入伺服器！");
            return true;
        }

        // 檢查背包空間
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "您的背包已滿！");
            return true;
        }

        // 扣除金錢
        economy.withdrawPlayer(player, totalPrice);

        // 創建頭顱物品
        ItemStack skull = createPlayerHead(targetPlayer, amount);

        // 給予玩家頭顱
        player.getInventory().addItem(skull);

        // 發送成功消息
        player.sendMessage(ChatColor.GREEN + "成功購買了 " + amount + " 個 " + targetPlayerName + " 的頭顱！");
        player.sendMessage(ChatColor.GREEN + "花費了 " + totalPrice + " 元，餘額：" + economy.getBalance(player) + " 元");

        return true;
    }

    private ItemStack createPlayerHead(OfflinePlayer targetPlayer, int amount) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, amount);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(targetPlayer);
            meta.setDisplayName(ChatColor.YELLOW + targetPlayer.getName() + " 的頭顱");
            skull.setItemMeta(meta);
        }

        return skull;
    }
}