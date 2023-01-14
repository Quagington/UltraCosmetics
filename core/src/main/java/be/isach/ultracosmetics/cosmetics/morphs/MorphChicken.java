package be.isach.ultracosmetics.cosmetics.morphs;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.Updatable;
import be.isach.ultracosmetics.cosmetics.type.MorphType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.ItemFactory;
import be.isach.ultracosmetics.util.Particles;
import be.isach.ultracosmetics.util.PetPathfinder;
import be.isach.ultracosmetics.version.VersionManager;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;

import java.util.ArrayList;
import java.util.List;

import me.gamercoder215.mobchip.EntityBrain;
import me.gamercoder215.mobchip.bukkit.BukkitBrain;

/**
 * Represents an instance of a chicken morph summoned by a player.
 *
 * @author iSach
 * @since 08-27-2015
 */
public class MorphChicken extends Morph implements Updatable {

    private List<Item> items = new ArrayList<>();
    private List<Chicken> chickens = new ArrayList<>();
    private boolean cooldown;

    public MorphChicken(UltraPlayer owner, MorphType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.getPlayer() == getPlayer() && getOwner().getCurrentMorph() == this && !cooldown) {
            items = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                items.add(ItemFactory.createUnpickableItemVariance(XMaterial.EGG, getPlayer().getLocation(), RANDOM, 0.5));
                XSound.ENTITY_CHICKEN_EGG.play(getPlayer(), 0.5f, 1.5f);
            }
            Bukkit.getScheduler().runTaskLater(getUltraCosmetics(), new Runnable() {
                BukkitRunnable followRunnable;

                @Override
                public void run() {
                    chickens = new ArrayList<>();
                    for (Item i : items) {
                        if (VersionManager.IS_VERSION_1_13) {
                            i.getWorld().spawnParticle(Particle.BLOCK_CRACK, i.getLocation(), 0, 0, 0, 0, 0, XMaterial.WHITE_TERRACOTTA.parseMaterial().createBlockData());
                        } else {
                            Particles.BLOCK_CRACK.display(new Particles.BlockData(XMaterial.WHITE_TERRACOTTA.parseMaterial(), (byte) 0), 0, 0, 0, 0.3f, 50, i.getLocation(), 128);
                        }
                        XSound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR.play(i.getLocation(), 0.05f, 1f);
                        final Chicken chicken = (Chicken) i.getWorld().spawnEntity(i.getLocation(), EntityType.CHICKEN);
                        chicken.setAgeLock(true);
                        chicken.setBaby();
                        chicken.setNoDamageTicks(Integer.MAX_VALUE);
                        chicken.setVelocity(new Vector(0, 0.5f, 0));
                        EntityBrain brain = BukkitBrain.getBrain(chicken);
                        brain.getGoalAI().clear();
                        brain.getTargetAI().clear();
                        brain.getGoalAI().put(new PetPathfinder(chicken, getPlayer()), 0);
                        i.remove();
                        chickens.add(chicken);
                    }
                    Bukkit.getScheduler().runTaskLater(getUltraCosmetics(), () -> {
                        for (Chicken chicken : chickens) {
                            Particles.LAVA.display(chicken.getLocation(), 10);
                            chicken.remove();
                        }
                        chickens.clear();
                        if (followRunnable != null) {
                            followRunnable.cancel();
                        }
                    }, 200);
                }
            }, 50);
            cooldown = true;
            Bukkit.getScheduler().runTaskLaterAsynchronously(getUltraCosmetics(), () -> cooldown = false, 30 * 20);
        }
    }

    /**
     * Cancel eggs from merging
     *
     */
    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        if (items.contains(event.getEntity()) || items.contains(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @Override
    protected void onClear() {
        for (Chicken chicken : chickens) {
            Particles.LAVA.display(chicken.getLocation(), 10);
            chicken.remove();
        }
        chickens.clear();
    }

    @Override
    public void onUpdate() {
        Player player = getPlayer();
        @SuppressWarnings("deprecation")
        boolean onGround = player.isOnGround();
        if (onGround || player.getVelocity().getY() >= 0) return;
        player.setVelocity(player.getVelocity().multiply(0.85));
    }
}
