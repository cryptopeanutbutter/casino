package dev.lixqa.egyptiancasino.integration;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class AmethystControlHook {

    private static final List<HookTarget> TARGETS = List.of(
            new HookTarget(
                    "CrystalMath",
                    "dev.lixqa.crystalmath.CrystalMath",
                    "dev.lixqa.crystalmath.MintLedger",
                    "dev.lixqa.crystalmath.util.MintedCrystalUtil"
            ),
            new HookTarget(
                    "AmethystControl",
                    "dev.lixqa.amethystControl.AmethystControl",
                    "dev.lixqa.amethystControl.MintLedger",
                    "dev.lixqa.amethystControl.util.MintedCrystalUtil"
            )
    );

    private final Logger logger;

    private JavaPlugin provider;
    private HookTarget activeTarget;
    private NamespacedKey mintedCrystalKey;
    private Object mintLedger;
    private Method markRedeemedMethod;
    private Method readLedgerIdMethod;

    public AmethystControlHook(Logger logger) {
        this.logger = logger;
    }

    public boolean initialize() {
        for (HookTarget target : TARGETS) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(target.pluginName());
            if (plugin instanceof JavaPlugin javaPlugin) {
                provider = javaPlugin;
                activeTarget = target;
                break;
            }
        }

        if (provider == null || activeTarget == null) {
            logger.warning("Neither CrystalMath nor AmethystControl were found. Minted crystal conversion will stay disabled.");
            return false;
        }

        try {
            Class<?> controllerClass = Class.forName(activeTarget.controllerClass());
            Class<?> ledgerClass = Class.forName(activeTarget.ledgerClass());
            Class<?> utilClass = Class.forName(activeTarget.crystalUtilClass());

            Method getMintedCrystalKey = controllerClass.getMethod("getMintedCrystalKey");
            Method getLedger = controllerClass.getMethod("getLedger");
            markRedeemedMethod = ledgerClass.getMethod("markRedeemed", UUID.class);
            readLedgerIdMethod = utilClass.getMethod("readLedgerId", ItemStack.class, NamespacedKey.class);

            mintedCrystalKey = (NamespacedKey) getMintedCrystalKey.invoke(provider);
            mintLedger = getLedger.invoke(provider);

            if (mintedCrystalKey == null || mintLedger == null) {
                logger.severe(activeTarget.pluginName() + " did not return the minted crystal key or ledger reference.");
                return false;
            }
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            logger.severe(activeTarget.pluginName() + " API is missing expected classes or methods: " + exception.getMessage());
        } catch (IllegalAccessException | InvocationTargetException exception) {
            logger.severe("Unable to query " + activeTarget.pluginName() + " API: " + exception.getMessage());
        }
        return false;
    }

    public String getProviderName() {
        return activeTarget != null ? activeTarget.pluginName() : "";
    }

    public NamespacedKey getMintedCrystalKey() {
        return mintedCrystalKey;
    }

    public Optional<UUID> readLedgerId(ItemStack stack) {
        if (stack == null || readLedgerIdMethod == null || mintedCrystalKey == null) {
            return Optional.empty();
        }
        try {
            Object result = readLedgerIdMethod.invoke(null, stack, mintedCrystalKey);
            if (result instanceof Optional<?> optional) {
                Object value = optional.orElse(null);
                if (value instanceof UUID uuid) {
                    return Optional.of(uuid);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            logger.warning("Failed to read minted crystal metadata via " + getProviderName() + ": " + exception.getMessage());
        }
        return Optional.empty();
    }

    public boolean markRedeemed(UUID ledgerId) {
        if (ledgerId == null || markRedeemedMethod == null || mintLedger == null) {
            return false;
        }
        try {
            Object result = markRedeemedMethod.invoke(mintLedger, ledgerId);
            return result instanceof Boolean && (Boolean) result;
        } catch (IllegalAccessException | InvocationTargetException exception) {
            logger.warning("Failed to mark minted crystal as redeemed via " + getProviderName() + ": " + exception.getMessage());
            return false;
        }
    }

    private record HookTarget(String pluginName, String controllerClass, String ledgerClass, String crystalUtilClass) {
    }
}
