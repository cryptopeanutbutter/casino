package dev.lixqa.egyptiancasino;

import dev.lixqa.egyptiancasino.commands.EgyptianCasinoCommand;
import dev.lixqa.egyptiancasino.exchange.CrystalExchangeListener;
import dev.lixqa.egyptiancasino.exchange.PriestOfRaManager;
import dev.lixqa.egyptiancasino.game.BlackjackGame;
import dev.lixqa.egyptiancasino.game.CasinoGame;
import dev.lixqa.egyptiancasino.game.DiceGame;
import dev.lixqa.egyptiancasino.game.GameRegistry;
import dev.lixqa.egyptiancasino.game.SlotsGame;
import dev.lixqa.egyptiancasino.integration.AmethystControlHook;
import dev.lixqa.egyptiancasino.slotmachine.SlotMachineManager;
import dev.lixqa.egyptiancasino.tokens.TokenManager;
import dev.lixqa.egyptiancasino.tokens.storage.YamlTokenStorage;
import dev.lixqa.egyptiancasino.tokens.storage.TokenStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class EgyptianCasinoPlugin extends JavaPlugin {

    private AmethystControlHook amethystHook;
    private NamespacedKey mintedCrystalKey;
    private TokenManager tokenManager;
    private PriestOfRaManager priestManager;
    private GameRegistry gameRegistry;
    private CasinoConfig casinoConfig;
    private NumberFormat numberFormat;
    private SlotMachineManager slotMachineManager;
    private NamespacedKey slotMachineItemKey;
    private NamespacedKey slotMachineEntityKey;
    private NamespacedKey slotMachineOwnerKey;
    private NamespacedKey egyptianTokenKey;

    @Override
    public void onEnable() {
        ensureAmethystControl();

        saveDefaultConfig();
        loadConfiguration();

        if (!setupTokenManager()) {
            return;
        }
        setupGames();
        priestManager = new PriestOfRaManager(this);
        slotMachineItemKey = new NamespacedKey(this, "slot_machine_item");
        slotMachineEntityKey = new NamespacedKey(this, "slot_machine_id");
        slotMachineOwnerKey = new NamespacedKey(this, "slot_machine_owner");
        egyptianTokenKey = new NamespacedKey(this, "egyptian_token");
        slotMachineManager = new SlotMachineManager(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CrystalExchangeListener(this, priestManager), this);
        pluginManager.registerEvents(slotMachineManager, this);

        EgyptianCasinoCommand command = new EgyptianCasinoCommand(this);
        if (getCommand("egyptiancasino") != null) {
            getCommand("egyptiancasino").setExecutor(command);
            getCommand("egyptiancasino").setTabCompleter(command);
        }

        if (getCommand("giveslot") != null) {
            getCommand("giveslot").setExecutor(slotMachineManager);
            getCommand("giveslot").setTabCompleter(slotMachineManager);
        }
    }

    @Override
    public void onDisable() {
        if (tokenManager != null) {
            tokenManager.shutdown();
        }
        if (slotMachineManager != null) {
            slotMachineManager.shutdown();
        }
    }

    private void ensureAmethystControl() {
        amethystHook = null;
        mintedCrystalKey = null;

        AmethystControlHook hook = new AmethystControlHook(getLogger());
        if (!hook.initialize()) {
            getLogger().warning("CrystalMath or AmethystControl were not detected. Minted crystal conversion is disabled.");
            return;
        }

        NamespacedKey crystalKey = hook.getMintedCrystalKey();
        if (crystalKey == null) {
            getLogger().warning(hook.getProviderName() + " did not expose the minted crystal key. Minted crystal conversion is disabled.");
            return;
        }

        amethystHook = hook;
        mintedCrystalKey = crystalKey;
        String providerName = hook.getProviderName().isEmpty() ? "Mint ledger provider" : hook.getProviderName();
        getLogger().info(providerName + " integration enabled. Minted crystals can now be converted.");
    }

    private boolean setupTokenManager() {
        try {
            TokenStorage storage = new YamlTokenStorage(new File(getDataFolder(), "tokens.yml"));
            tokenManager = new TokenManager(storage);
            tokenManager.initialize();
            return true;
        } catch (SQLException exception) {
            getLogger().severe("Failed to initialize the token storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void setupGames() {
        gameRegistry = new GameRegistry();
        registerGame(new SlotsGame());
        registerGame(new BlackjackGame());
        registerGame(new DiceGame());
    }

    private void registerGame(CasinoGame game) {
        gameRegistry.register(game);
    }

    private void loadConfiguration() {
        reloadConfig();
        FileConfiguration config = getConfig();
        casinoConfig = new CasinoConfig(config);
        numberFormat = NumberFormat.getIntegerInstance(Locale.US);
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public PriestOfRaManager getPriestManager() {
        return priestManager;
    }

    public GameRegistry getGameRegistry() {
        return gameRegistry;
    }

    public CasinoConfig getCasinoConfig() {
        return casinoConfig;
    }

    public NamespacedKey getMintedCrystalKey() {
        return mintedCrystalKey;
    }

    public boolean isAmethystControlReady() {
        return amethystHook != null && mintedCrystalKey != null;
    }

    public NamespacedKey getSlotMachineItemKey() {
        return slotMachineItemKey;
    }

    public NamespacedKey getSlotMachineEntityKey() {
        return slotMachineEntityKey;
    }

    public NamespacedKey getSlotMachineOwnerKey() {
        return slotMachineOwnerKey;
    }

    public NamespacedKey getEgyptianTokenKey() {
        return egyptianTokenKey;
    }

    public SlotMachineManager getSlotMachineManager() {
        return slotMachineManager;
    }

    public Component formatTokens(long amount) {
        return Component.text(numberFormat.format(amount) + " Egyptian Tokens").color(NamedTextColor.GOLD);
    }

    public long convertCrystals(Player player) {
        if (!isAmethystControlReady()) {
            return -1;
        }

        Inventory inventory = player.getInventory();
        long totalCrystals = 0;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getAmount() <= 0) {
                continue;
            }

            Optional<UUID> ledgerId = amethystHook.readLedgerId(stack);
            if (ledgerId.isEmpty()) {
                continue;
            }

            UUID crystalId = ledgerId.get();
            boolean redeemed = amethystHook.markRedeemed(crystalId);
            if (!redeemed) {
                continue;
            }

            totalCrystals += stack.getAmount();
            inventory.clear(slot);
        }

        long tokensPerCrystal = casinoConfig.tokensPerCrystal();
        long tokensAwarded = totalCrystals * tokensPerCrystal;
        if (tokensAwarded > 0) {
            tokenManager.deposit(player.getUniqueId(), tokensAwarded);
        }

        return tokensAwarded;
    }

    public void sendMessage(Player player, Component message) {
        player.sendMessage(Component.text("[EgyptianCasino] ", NamedTextColor.GOLD).append(message));
    }

    public ItemStack createEgyptianTokenItem(int amount) {
        ItemStack itemStack = new ItemStack(org.bukkit.Material.GOLD_NUGGET, Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Egyptian Token", NamedTextColor.GOLD));
        meta.setCustomModelData(1010);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(getEgyptianTokenKey(), PersistentDataType.INTEGER, 1);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public boolean isEgyptianToken(ItemStack stack) {
        if (stack == null || stack.getType() != org.bukkit.Material.GOLD_NUGGET) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 1010) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer marker = container.get(getEgyptianTokenKey(), PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    public long depositTokenItems(Player player) {
        Inventory inventory = player.getInventory();
        long collected = 0;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isEgyptianToken(stack)) {
                continue;
            }

            collected += stack.getAmount();
            inventory.clear(slot);
        }

        if (collected > 0) {
            tokenManager.deposit(player.getUniqueId(), collected);
        }

        return collected;
    }
}
