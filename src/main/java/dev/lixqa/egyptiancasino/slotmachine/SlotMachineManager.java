package dev.lixqa.egyptiancasino.slotmachine;

import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import dev.lixqa.egyptiancasino.tokens.TokenManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;

import java.util.*;

public class SlotMachineManager implements Listener, CommandExecutor, TabCompleter {

    private final EgyptianCasinoPlugin plugin;
    private final Map<UUID, SlotMachineInstance> machines = new HashMap<>();
    private final Map<String, UUID> machinesByBlock = new HashMap<>();
    private final List<Integer> reelSymbolModels = new ArrayList<>();
    private final Random random = new Random();

    public SlotMachineManager(EgyptianCasinoPlugin plugin) {
        this.plugin = plugin;
        // Default reel symbols reference custom model data entries. Replace with your own IDs.
        reelSymbolModels.addAll(EgyptSlots.createDefaultSymbolList());
    }

    public ItemStack createSlotMachineItem(int amount) {
        ItemStack itemStack = new ItemStack(Material.BLAZE_ROD, Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Slot Machine", NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(plugin.getSlotMachineItemKey(), PersistentDataType.INTEGER, 1);
        // Custom model data is optional but recommended for unique textures.
        meta.setCustomModelData(2100);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public boolean isSlotMachineItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer marker = data.get(plugin.getSlotMachineItemKey(), PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    private String blockKey(Location location) {
        BlockVector vector = new BlockVector(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return location.getWorld().getUID() + ":" + vector.getBlockX() + ":" + vector.getBlockY() + ":" + vector.getBlockZ();
    }

    private Optional<SlotMachineInstance> findByEntity(Entity entity) {
        if (!(entity instanceof Display display)) {
            return Optional.empty();
        }
        PersistentDataContainer data = display.getPersistentDataContainer();
        String id = data.get(plugin.getSlotMachineEntityKey(), PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        UUID uuid = UUID.fromString(id);
        return Optional.ofNullable(machines.get(uuid));
    }

    private Optional<SlotMachineInstance> findByBlock(Location baseLocation) {
        return Optional.ofNullable(machinesByBlock.get(blockKey(baseLocation)))
                .map(machines::get);
    }

    @EventHandler
    public void handleSlotPlacement(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!isSlotMachineItem(stack)) {
            return;
        }

        if (event.getClickedBlock() == null || event.getBlockFace() == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Location clicked = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
        if (!canPlace(clicked)) {
            plugin.sendMessage(player, Component.text("There is not enough room to place the Slot Machine.", NamedTextColor.RED));
            return;
        }

        if (findByBlock(clicked).isPresent()) {
            plugin.sendMessage(player, Component.text("A Slot Machine is already placed here.", NamedTextColor.RED));
            return;
        }

        SlotMachineInstance instance = new SlotMachineInstance(plugin, this, player.getUniqueId(), clicked.toBlockLocation(), resolveFacing(player));
        if (!instance.spawn()) {
            plugin.sendMessage(player, Component.text("Failed to create the Slot Machine. Check console for errors.", NamedTextColor.RED));
            return;
        }

        machines.put(instance.getMachineId(), instance);
        machinesByBlock.put(blockKey(clicked.toBlockLocation()), instance.getMachineId());

        stack.subtract(1);
        plugin.sendMessage(player, Component.text("Placed a Slot Machine. Only you can break it while sneaking.", NamedTextColor.GOLD));
    }

    @EventHandler(ignoreCancelled = true)
    public void handleMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        findByBlock(event.getClickedBlock().getLocation()).ifPresent(machine -> {
            event.setCancelled(true);
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            machine.handleUse(event.getPlayer());
        });
    }

    private BlockFace resolveFacing(Player player) {
        BlockFace face = player.getFacing();
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            face = BlockFace.SOUTH;
        }
        return face.getOppositeFace();
    }

    private boolean canPlace(Location base) {
        if (!base.getChunk().isLoaded()) {
            base.getChunk().load();
        }
        return base.getBlock().isEmpty() && base.clone().add(0, 1, 0).getBlock().isEmpty();
    }

    @EventHandler
    public void handleUse(PlayerInteractAtEntityEvent event) {
        findByEntity(event.getRightClicked()).ifPresent(machine -> {
            event.setCancelled(true);
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            machine.handleUse(event.getPlayer());
        });
    }

    @EventHandler
    public void handleHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        findByEntity(event.getEntity()).ifPresent(machine -> {
            event.setCancelled(true);
            if (!player.isSneaking()) {
                plugin.sendMessage(player, Component.text("Sneak while breaking your Slot Machine to collect it.", NamedTextColor.YELLOW));
                return;
            }
            if (!machine.getOwner().equals(player.getUniqueId())) {
                plugin.sendMessage(player, Component.text("Only the owner can break this Slot Machine.", NamedTextColor.RED));
                return;
            }
            removeMachine(machine, true);
            plugin.sendMessage(player, Component.text("Slot Machine returned to your inventory.", NamedTextColor.GREEN));
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void handleBlockBreak(BlockBreakEvent event) {
        findByBlock(event.getBlock().getLocation()).ifPresent(machine -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!player.isSneaking()) {
                plugin.sendMessage(player, Component.text("Sneak while breaking your Slot Machine to collect it.", NamedTextColor.YELLOW));
                return;
            }
            if (!machine.getOwner().equals(player.getUniqueId())) {
                plugin.sendMessage(player, Component.text("Only the owner can break this Slot Machine.", NamedTextColor.RED));
                return;
            }
            removeMachine(machine, true);
            plugin.sendMessage(player, Component.text("Slot Machine returned to your inventory.", NamedTextColor.GREEN));
        });
    }

    public void startSpin(SlotMachineInstance machine, Player player) {
        if (machine.isSpinning()) {
            plugin.sendMessage(player, Component.text("The reels are already spinning!", NamedTextColor.YELLOW));
            return;
        }

        TokenManager tokenManager = plugin.getTokenManager();
        long converted = plugin.depositTokenItems(player);
        if (converted > 0) {
            plugin.sendMessage(player, Component.text("Banked " + converted + " Egyptian Token" + (converted == 1 ? "" : "s") + " from your inventory.", NamedTextColor.GREEN));
        }
        if (!tokenManager.withdraw(player.getUniqueId(), 1)) {
            plugin.sendMessage(player, Component.text("You do not have enough Egyptian Tokens to play.", NamedTextColor.RED));
            return;
        }

        machine.spin(player, outcome -> handleOutcome(machine, player, outcome));
    }

    private void handleOutcome(SlotMachineInstance machine, Player player, SlotOutcome outcome) {
        long reward = EgyptSlots.rewardForMatches(outcome.matchCount());

        World world = machine.getBaseLocation().getWorld();
        Location displayLocation = machine.getVisualLocation();
        if (reward > 0) {
            plugin.getTokenManager().deposit(player.getUniqueId(), reward);
            if (outcome.matchCount() == 3) {
                world.playSound(displayLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.spawnParticle(Particle.GLOW, displayLocation, 40, 0.4, 0.6, 0.4, 0.02);
            } else {
                world.playSound(displayLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.3f);
                world.spawnParticle(Particle.FIREWORKS_SPARK, displayLocation, 18, 0.25, 0.4, 0.25, 0.03);
            }
            plugin.sendMessage(player, Component.text("You won " + reward + " Egyptian Token" + (reward == 1 ? "" : "s") + "!", NamedTextColor.GOLD));
        } else {
            world.playSound(displayLocation, Sound.BLOCK_SAND_FALL, SoundCategory.BLOCKS, 0.7f, 0.8f);
            world.spawnParticle(Particle.FALLING_DUST, displayLocation, 25, 0.35, 0.4, 0.35, 0.02, Material.SAND.createBlockData());
            plugin.sendMessage(player, Component.text("No matching symbols this time.", NamedTextColor.RED));
        }
    }

    public ItemStack createBodyItem() {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Pharaoh Slot Cabinet", NamedTextColor.GOLD));
        meta.setCustomModelData(3200);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemStack createReelItem(int modelData) {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Pharaoh Reel Symbol", NamedTextColor.AQUA));
        meta.setCustomModelData(modelData);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemStack createLeverItem() {
        ItemStack itemStack = new ItemStack(Material.STICK);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Pharaoh Lever", NamedTextColor.GOLD));
        meta.setCustomModelData(3100);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public int randomSymbolModel() {
        return reelSymbolModels.get(random.nextInt(reelSymbolModels.size()));
    }

    public List<Integer> getReelSymbolModels() {
        return Collections.unmodifiableList(reelSymbolModels);
    }

    public void removeMachine(SlotMachineInstance instance, boolean dropItem) {
        machines.remove(instance.getMachineId());
        machinesByBlock.remove(blockKey(instance.getBaseLocation()));
        instance.despawn();
        if (dropItem) {
            ItemStack item = createSlotMachineItem(1);
            instance.getBaseLocation().getWorld().dropItemNaturally(instance.getBaseLocation().add(0.5, 0.5, 0.5), item);
        }
    }

    public void shutdown() {
        machines.values().forEach(SlotMachineInstance::despawn);
        machines.clear();
        machinesByBlock.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can receive Slot Machine items.");
            return true;
        }

        if (!player.hasPermission("egyptiancasino.admin")) {
            plugin.sendMessage(player, Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length > 0) {
            try {
                amount = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException exception) {
                plugin.sendMessage(player, Component.text("Amount must be numeric.", NamedTextColor.RED));
                return true;
            }
        }

        ItemStack itemStack = createSlotMachineItem(amount);
        player.getInventory().addItem(itemStack);
        plugin.sendMessage(player, Component.text("Gave " + amount + " Slot Machine item" + (amount == 1 ? "" : "s") + ".", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("egyptiancasino.admin")) {
            return Collections.singletonList("1");
        }
        return Collections.emptyList();
    }
}
