package Sergey_Dertan.SRegionProtector.Event;

import Sergey_Dertan.SRegionProtector.Messenger.Messenger;
import Sergey_Dertan.SRegionProtector.Region.Selector.RegionSelector;
import Sergey_Dertan.SRegionProtector.Region.Selector.SelectorSession;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;

@SuppressWarnings("unused")
public final class SelectorEventsHandler implements Listener {

    private final RegionSelector regionSelector;
    private final Item wandItem;

    public SelectorEventsHandler(RegionSelector selector, Item wandItem) {
        this.regionSelector = selector;
        this.wandItem = wandItem;
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        this.regionSelector.removeSession(e.getPlayer());
        this.regionSelector.removeBorders(e.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerInteract(PlayerInteractEvent e) {
        if (this.selectPosition(e.getPlayer(), e.getBlock(), e.getItem())) e.setCancelled();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockBreak(BlockBreakEvent e) {
        if (this.selectPosition(e.getPlayer(), e.getBlock(), e.getItem())) e.setCancelled();
    }

    private boolean selectPosition(Player player, Block pos, Item item) {
        if (item == null || pos instanceof BlockAir || !item.equals(this.wandItem, true, false)) return false;
        if (!player.hasPermission("sregionprotector.wand")) return false;
        SelectorSession session = this.regionSelector.getSession(player);
        if (!session.setNextPos(Position.fromObject(pos, pos.level))) return false;
        if (session.nextPos) {
            Messenger.getInstance().sendMessage(player, "region.selection.pos2");
        } else {
            Messenger.getInstance().sendMessage(player, "region.selection.pos1");
        }
        return true;
    }
}
