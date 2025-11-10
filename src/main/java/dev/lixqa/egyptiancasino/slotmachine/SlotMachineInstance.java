package dev.lixqa.egyptiancasino.slotmachine;

import dev.lixqa.egyptiancasino.EgyptianCasinoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SlotMachineInstance {

    private final EgyptianCasinoPlugin plugin;
    private final SlotMachineManager manager;
    private final UUID owner;
    private final Location baseLocation;
    private final BlockFace facing;
    private final float facingYaw;
    private final UUID machineId = UUID.randomUUID();
    private ItemDisplay bodyDisplay;
    private ItemDisplay[] reelDisplays;
    private ItemDisplay leverDisplay;
    private boolean spinning;
    private BukkitTask spinTask;
    private BukkitTask leverResetTask;
    private Transformation leverRestTransformation;
    private Transformation leverPulledTransformation;

    public SlotMachineInstance(EgyptianCasinoPlugin plugin, SlotMachineManager manager, UUID owner, Location baseLocation, BlockFace facing) {
        this.plugin = plugin;
        this.manager = manager;
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.facing = facing;
        this.facingYaw = yawForFacing(facing);
    }

    public UUID getMachineId() {
        return machineId;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getBaseLocation() {
        return baseLocation.clone();
    }

    public Location getVisualLocation() {
        return toWorldPosition(0.0, 1.35, 0.35);
    }

    public boolean spawn() {
        try {
            World world = baseLocation.getWorld();
            if (world == null) {
                return false;
            }
            baseLocation.getBlock().setType(Material.SMOOTH_SANDSTONE);
            baseLocation.clone().add(0, 1, 0).getBlock().setType(Material.AIR);

            Location bodyLocation = toWorldPosition(0.0, 1.0, 0.0);
            bodyDisplay = world.spawn(bodyLocation, ItemDisplay.class, display -> {
                display.setItemStack(manager.createBodyItem());
                display.setBillboard(Display.Billboard.FIXED);
                display.setInterpolationDuration(5);
                display.setRotation(facingYaw, 0f);
                display.setViewRange(1.6f);
                display.setItemDisplayTransform(ItemDisplayTransform.FIXED);
                markDisplay(display);
            });

            reelDisplays = new ItemDisplay[3];
            double[][] reelOffsets = {
                    {-0.32, 1.35, 0.36},
                    {0.0, 1.35, 0.36},
                    {0.32, 1.35, 0.36}
            };
            for (int i = 0; i < reelDisplays.length; i++) {
                double[] offsets = reelOffsets[i];
                Location reelLocation = toWorldPosition(offsets[0], offsets[1], offsets[2]);
                int symbolModel = manager.randomSymbolModel();
                reelDisplays[i] = world.spawn(reelLocation, ItemDisplay.class, display -> {
                    display.setItemStack(manager.createReelItem(symbolModel));
                    display.setBillboard(Display.Billboard.FIXED);
                    display.setInterpolationDuration(2);
                    display.setRotation(facingYaw, 0f);
                    display.setViewRange(1.3f);
                    display.setItemDisplayTransform(ItemDisplayTransform.FIXED);
                    markDisplay(display);
                });
            }

            Location leverLocation = toWorldPosition(0.58, 1.2, 0.18);
            leverDisplay = world.spawn(leverLocation, ItemDisplay.class, display -> {
                display.setItemStack(manager.createLeverItem());
                display.setBillboard(Display.Billboard.FIXED);
                display.setInterpolationDuration(5);
                display.setRotation(facingYaw, 0f);
                display.setViewRange(1.3f);
                display.setItemDisplayTransform(ItemDisplayTransform.FIXED);
                markDisplay(display);
            });

            leverRestTransformation = leverDisplay.getTransformation();
            leverPulledTransformation = translate(leverRestTransformation, new Vector3f(0.0f, -0.25f, 0.05f));
            leverDisplay.setTransformation(leverRestTransformation);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to spawn Slot Machine: " + exception.getMessage());
            baseLocation.getBlock().setType(Material.AIR);
            baseLocation.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
            return false;
        }
    }

    private void markDisplay(Display display) {
        display.getPersistentDataContainer().set(plugin.getSlotMachineEntityKey(), PersistentDataType.STRING, machineId.toString());
        display.getPersistentDataContainer().set(plugin.getSlotMachineOwnerKey(), PersistentDataType.STRING, owner.toString());
    }

    public void handleUse(Player player) {
        if (player.isSneaking() && player.getUniqueId().equals(owner)) {
            plugin.sendMessage(player, Component.text("Sneak-left click to break the machine.", NamedTextColor.YELLOW));
            return;
        }
        manager.startSpin(this, player);
    }

    public boolean isSpinning() {
        return spinning;
    }

    public void spin(Player player, Consumer<SlotOutcome> callback) {
        spinning = true;
        World world = baseLocation.getWorld();
        if (world == null) {
            spinning = false;
            return;
        }
        Location visual = getVisualLocation();
        world.playSound(visual, Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8f, 0.9f);
        pullLever();

        List<Integer> symbols = manager.getReelSymbolModels();
        int[] indices = new int[reelDisplays.length];
        Arrays.setAll(indices, i -> ThreadLocalRandom.current().nextInt(symbols.size()));
        int[] finalModels = new int[reelDisplays.length];

        spinTask = new BukkitRunnable() {
            int tick = 0;
            boolean firstStopped = false;
            boolean secondStopped = false;
            boolean thirdStopped = false;

            @Override
            public void run() {
                tick++;

                if (tick % 4 == 0) {
                    for (int i = 0; i < reelDisplays.length; i++) {
                        if ((i == 0 && firstStopped) || (i == 1 && secondStopped) || (i == 2 && thirdStopped)) {
                            continue;
                        }
                        indices[i] = (indices[i] + 1) % symbols.size();
                        reelDisplays[i].setItemStack(manager.createReelItem(symbols.get(indices[i])));
                    }
                }

                if (tick == 60 && !firstStopped) {
                    firstStopped = true;
                    finalModels[0] = symbols.get(indices[0]);
                    world.playSound(visual, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 0.6f, 1.2f);
                }
                if (tick == 80 && !secondStopped) {
                    secondStopped = true;
                    finalModels[1] = symbols.get(indices[1]);
                    world.playSound(visual, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 0.6f, 1.4f);
                }
                if (tick == 100 && !thirdStopped) {
                    thirdStopped = true;
                    finalModels[2] = symbols.get(indices[2]);
                    world.playSound(visual, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 0.6f, 1.6f);
                    complete();
                }
            }

            private void complete() {
                cancel();
                finishSpin(player, finalModels, callback);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void pullLever() {
        leverDisplay.setTransformation(leverPulledTransformation);
        if (leverResetTask != null) {
            leverResetTask.cancel();
        }
        leverResetTask = new BukkitRunnable() {
            @Override
            public void run() {
                leverDisplay.setTransformation(leverRestTransformation);
            }
        }.runTaskLater(plugin, 15L);
    }

    private void finishSpin(Player player, int[] finalModels, Consumer<SlotOutcome> callback) {
        spinning = false;
        spinTask = null;
        List<Integer> results = new ArrayList<>();
        for (int model : finalModels) {
            results.add(model);
        }
        int matches = calculateMatches(results);
        callback.accept(new SlotOutcome(results, matches));
    }

    private int calculateMatches(List<Integer> results) {
        if (results.get(0).equals(results.get(1)) && results.get(1).equals(results.get(2))) {
            return 3;
        }
        if (results.get(0).equals(results.get(1)) || results.get(0).equals(results.get(2)) || results.get(1).equals(results.get(2))) {
            return 2;
        }
        return 0;
    }

    public void despawn() {
        if (spinTask != null) {
            spinTask.cancel();
        }
        if (leverResetTask != null) {
            leverResetTask.cancel();
        }
        if (bodyDisplay != null) {
            bodyDisplay.remove();
        }
        if (reelDisplays != null) {
            for (ItemDisplay display : reelDisplays) {
                if (display != null) {
                    display.remove();
                }
            }
        }
        if (leverDisplay != null) {
            leverDisplay.remove();
        }
        baseLocation.getBlock().setType(Material.AIR);
        baseLocation.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
    }

    private Location toWorldPosition(double offsetX, double offsetY, double offsetZ) {
        double[] rotated = rotate(offsetX, offsetZ);
        return baseLocation.clone().add(0.5 + rotated[0], offsetY, 0.5 + rotated[1]);
    }

    private double[] rotate(double offsetX, double offsetZ) {
        double radians = Math.toRadians(facingYaw);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double worldX = offsetX * cos - offsetZ * sin;
        double worldZ = offsetX * sin + offsetZ * cos;
        return new double[]{worldX, worldZ};
    }

    private static float yawForFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> 180f;
            case EAST -> -90f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    private Transformation translate(Transformation base, Vector3f delta) {
        Vector3f translation = new Vector3f(base.getTranslation());
        translation.add(delta);
        Quaternionf leftRotation = new Quaternionf(base.getLeftRotation());
        Vector3f scale = new Vector3f(base.getScale());
        Quaternionf rightRotation = new Quaternionf(base.getRightRotation());
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }
}
