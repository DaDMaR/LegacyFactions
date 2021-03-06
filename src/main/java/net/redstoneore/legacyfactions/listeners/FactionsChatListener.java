package net.redstoneore.legacyfactions.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.FPlayerColl;
import net.redstoneore.legacyfactions.entity.Faction;

import java.util.UnknownFormatConversionException;
import java.util.logging.Level;


public class FactionsChatListener implements Listener {

	// -------------------------------------------------- //
	// INSTANCE
	// -------------------------------------------------- //
	
	private static FactionsChatListener i = new FactionsChatListener();
	public static FactionsChatListener get() { return i; }
	
	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //
	
	// this is for handling slashless command usage and faction/alliance chat, set at lowest priority so Factions gets to them first
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerEarlyChat(AsyncPlayerChatEvent event) {
		FPlayer me = FPlayerColl.get(event.getPlayer());
		ChatMode chat = me.getChatMode();
		
		if (chat == ChatMode.PUBLIC) return;

		String msg = event.getMessage();
		Faction myFaction = me.getFaction();

		// Is it a faction chat message?
		if (chat == ChatMode.FACTION) {

			String message = String.format(Conf.factionChatFormat, me.describeTo(myFaction), msg);
			myFaction.msg(message);

			Factions.get().log("[FactionChat " + myFaction.getTag() + "] " + ChatColor.stripColor(message));

			// Send to any players who are spying chat
			FPlayerColl.all(fplayer -> {
				if (fplayer.isSpyingChat() && fplayer.getFactionId() != myFaction.getId() && me != fplayer) {
					fplayer.msg("[FCspy] " + myFaction.getTag() + ": " + message);
				}
			});
			
			event.setCancelled(true);
			return;
		}
		
		if (chat == ChatMode.ALLIANCE) {
			String message = String.format(Conf.allianceChatFormat, ChatColor.stripColor(me.getNameAndTag()), msg);

			//Send message to our own faction
			myFaction.msg(message);

			//Send to all our allies
			FPlayerColl.all(true, fplayer -> {
				if (myFaction.getRelationTo(fplayer) == Relation.ALLY && !fplayer.isIgnoreAllianceChat()) {
					fplayer.msg(message);
				} else if (fplayer.isSpyingChat() && me != fplayer) {
					fplayer.msg("[ACspy]: " + message);
				}
			});
			
			Factions.get().log("[AllianceChat] " + ChatColor.stripColor(message));
			
			event.setCancelled(true);
			return;
		}
		
		if (chat == ChatMode.TRUCE) {
			String message = String.format(Conf.truceChatFormat, ChatColor.stripColor(me.getNameAndTag()), msg);

			// Send message to our own faction
			myFaction.msg(message);
			
			// Send to all our truces
			FPlayerColl.all(true, fplayer -> {
				if (myFaction.getRelationTo(fplayer) == Relation.TRUCE) {
					fplayer.msg(message);
				} else if (fplayer.isSpyingChat() && fplayer != me) {
					fplayer.msg("[TCspy]: " + message);
				}
			});
			
			Factions.get().log("[TruceChat] " + ChatColor.stripColor(message));

			event.setCancelled(true);
			return;
		}
	}

	// this is for handling insertion of the player's faction tag, set at highest priority to give other plugins a chance to modify chat first
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		// Are we to insert the Faction tag into the format?
		// If we are not to insert it - we are done.
		if (!Conf.chatTagEnabled || Conf.chatTagHandledByAnotherPlugin) {
			return;
		}

		Player talkingPlayer = event.getPlayer();
		String msg = event.getMessage();
		String eventFormat = event.getFormat();
		FPlayer me = FPlayerColl.get(talkingPlayer);
		int InsertIndex;

		if (!Conf.chatTagReplaceString.isEmpty() && eventFormat.contains(Conf.chatTagReplaceString)) {
			// we're using the "replace" method of inserting the faction tags
			if (eventFormat.contains("[FACTION_TITLE]")) {
				eventFormat = eventFormat.replace("[FACTION_TITLE]", me.getTitle());
			}
			InsertIndex = eventFormat.indexOf(Conf.chatTagReplaceString);
			eventFormat = eventFormat.replace(Conf.chatTagReplaceString, "");
			Conf.chatTagPadAfter = false;
			Conf.chatTagPadBefore = false;
		} else if (!Conf.chatTagInsertAfterString.isEmpty() && eventFormat.contains(Conf.chatTagInsertAfterString)) {
			// we're using the "insert after string" method
			InsertIndex = eventFormat.indexOf(Conf.chatTagInsertAfterString) + Conf.chatTagInsertAfterString.length();
		} else if (!Conf.chatTagInsertBeforeString.isEmpty() && eventFormat.contains(Conf.chatTagInsertBeforeString)) {
			// we're using the "insert before string" method
			InsertIndex = eventFormat.indexOf(Conf.chatTagInsertBeforeString);
		} else {
			// we'll fall back to using the index place method
			InsertIndex = Conf.chatTagInsertIndex;
			if (InsertIndex > eventFormat.length()) {
				return;
			}
		}

		String formatStart = eventFormat.substring(0, InsertIndex) + ((Conf.chatTagPadBefore && !me.getChatTag().isEmpty()) ? " " : "");
		String formatEnd = ((Conf.chatTagPadAfter && !me.getChatTag().isEmpty()) ? " " : "") + eventFormat.substring(InsertIndex);

		String nonColoredMsgFormat = formatStart + me.getChatTag().trim() + formatEnd;

		// Relation Colored?
		if (Conf.chatTagRelationColored) {
			// We must choke the standard message and send out individual messages to all players
			// Why? Because the relations will differ.
			event.setCancelled(true);

			event.getRecipients().forEach(listeningPlayer -> {
				FPlayer you = FPlayerColl.get(listeningPlayer);
				String yourFormat = formatStart + me.getChatTag(you).trim() + formatEnd;
				try {
					listeningPlayer.sendMessage(String.format(yourFormat, talkingPlayer.getDisplayName(), msg));
				} catch (UnknownFormatConversionException ex) {
					Conf.chatTagInsertIndex = 0;
					Factions.get().error("Critical error in chat message formatting!");
					Factions.get().error("NOTE: This has been automatically fixed right now by setting chatTagInsertIndex to 0.");
					Factions.get().error("For a more proper fix, please read this regarding chat configuration: http://massivecraft.com/plugins/factions/config#Chat_configuration");
					return;
				}
			});
			
			// Write to the log... We will write the non colored message.
			String nonColoredMsg = ChatColor.stripColor(String.format(nonColoredMsgFormat, talkingPlayer.getDisplayName(), msg));
			Bukkit.getLogger().log(Level.INFO, nonColoredMsg);
		} else {
			// No relation color.
			event.setFormat(nonColoredMsgFormat);
		}
	}
	
}
