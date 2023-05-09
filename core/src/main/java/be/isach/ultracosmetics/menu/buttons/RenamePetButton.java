package be.isach.ultracosmetics.menu.buttons;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.menu.Button;
import be.isach.ultracosmetics.menu.ClickData;
import be.isach.ultracosmetics.menu.Menu;
import be.isach.ultracosmetics.menu.PurchaseData;
import be.isach.ultracosmetics.menu.menus.MenuPurchase;
import be.isach.ultracosmetics.mysql.MySqlConnectionManager;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.ItemFactory;
import com.cryptomorin.xseries.XMaterial;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

public class RenamePetButton implements Button {
    private final String name = MessageManager.getMessage("Menu.Rename-Pet.Button.Name");
    private final String activePetNeeded = MessageManager.getMessage("Active-Pet-Needed");
    private final ItemStack stack = ItemFactory.getItemStackFromConfig("Categories.Rename-Pet-Item");
    private final UltraCosmetics ultraCosmetics;

    public RenamePetButton(UltraCosmetics ultraCosmetics) {
        this.ultraCosmetics = ultraCosmetics;
    }

    @Override
    public ItemStack getDisplayItem(UltraPlayer ultraPlayer) {
        if (ultraPlayer.getCurrentPet() == null) {
            return ItemFactory.rename(this.stack.clone(), activePetNeeded);
        }
        return ItemFactory.rename(this.stack.clone(), name.replace("%petname%", ultraPlayer.getCurrentPet().getType().getName()));
    }

    @Override
    public void onClick(ClickData clickData) {
        UltraPlayer player = clickData.getClicker();
        if (player.getCurrentPet() == null) {
            player.getBukkitPlayer().sendMessage(activePetNeeded);
            player.getBukkitPlayer().closeInventory();
            return;
        }
        renamePet(ultraCosmetics, player, clickData.getMenu());
    }

    public static void renamePet(UltraCosmetics ultraCosmetics, final UltraPlayer ultraPlayer, Menu returnMenu) {
        new AnvilGUI.Builder().plugin(ultraCosmetics)
                .itemLeft(XMaterial.PAPER.parseItem())
                .text(MessageManager.getMessage("Menu.Rename-Pet.Placeholder"))
                .title(MessageManager.getMessage("Menu.Rename-Pet.Title"))
                .onComplete(completion -> {
                    String text = completion.getText();
                    if (text.length() > MySqlConnectionManager.MAX_NAME_SIZE) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(MessageManager.getMessage("Too-Long")));
                    }
                    if (!text.isEmpty() && ultraCosmetics.getEconomyHandler().isUsingEconomy()
                            && SettingsManager.getConfig().getBoolean("Pets-Rename.Requires-Money.Enabled")) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.openInventory(buyRenamePet(ultraPlayer, text, returnMenu)));
                    } else {
                        ultraPlayer.setPetName(ultraPlayer.getCurrentPet().getType(), text);
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }
                }).open(ultraPlayer.getBukkitPlayer());
    }

    public static Inventory buyRenamePet(UltraPlayer ultraPlayer, final String name, Menu returnMenu) {
        final String formattedName = UltraPlayer.colorizePetName(name);
        int price = SettingsManager.getConfig().getInt("Pets-Rename.Requires-Money.Price");
        ItemStack showcaseItem = ItemFactory.create(XMaterial.NAME_TAG, MessageManager.getMessage("Menu.Purchase-Rename.Button.Showcase")
                .replace("%price%", String.valueOf(price)).replace("%name%", formattedName));

        PurchaseData purchaseData = new PurchaseData();
        purchaseData.setPrice(price);
        purchaseData.setShowcaseItem(showcaseItem);
        purchaseData.setOnPurchase(() -> {
            ultraPlayer.setPetName(ultraPlayer.getCurrentPet().getType(), name);
            if (returnMenu != null) {
                returnMenu.open(ultraPlayer);
            }
        });
        if (returnMenu != null) {
            purchaseData.setOnCancel(() -> returnMenu.open(ultraPlayer));
        }

        MenuPurchase menu = new MenuPurchase(UltraCosmeticsData.get().getPlugin(), MessageManager.getMessage("Menu.Purchase-Rename.Title"), purchaseData);
        return menu.getInventory(ultraPlayer);
    }
}