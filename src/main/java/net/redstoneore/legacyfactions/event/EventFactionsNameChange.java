package net.redstoneore.legacyfactions.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import net.redstoneore.legacyfactions.entity.FPlayer;

public class EventFactionsNameChange extends AbstractFactionsPlayerEvent implements Cancellable {

    private boolean cancelled = false;
    private String tag;

    public EventFactionsNameChange(FPlayer sender, String newTag) {
        super(sender.getFaction(), sender);
        tag = newTag;
    }

    /**
     * Get the player involved in the event.
     *
     * @return Player involved in the event.
     *
     * @deprecated use getfPlayer().getPlayer() instead.
     */
    @Deprecated
    public Player getPlayer() {
        return getfPlayer().getPlayer();
    }

    /**
     * Get the faction tag before it was renamed.
     *
     * @return old faction tag.
     *
     * @deprecated use getFaction().getTag() instead.
     */
    @Deprecated
    public String getOldFactionTag() {
        return getFaction().getTag();
    }

    /**
     * Get the new faction tag.
     *
     * @return new faction tag as String.
     */
    public String getFactionTag() {
        return tag;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        this.cancelled = c;
    }
}
