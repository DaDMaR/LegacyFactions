package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.FPlayerColl;
import net.redstoneore.legacyfactions.entity.Faction;


public class CmdFactionsPermanent extends FCommand {

    public CmdFactionsPermanent() {
        this.aliases.addAll(Conf.cmdAliasesPermanent);

        this.requiredArgs.add("faction tag");

        this.permission = Permission.SET_PERMANENT.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction faction = this.argAsFaction(0);
        if (faction == null) {
            return;
        }

        String change;
        if (faction.isPermanent()) {
            change = Lang.COMMAND_PERMANENT_REVOKE.toString();
            faction.setPermanent(false);
        } else {
            change = Lang.COMMAND_PERMANENT_GRANT.toString();
            faction.setPermanent(true);
        }

        Factions.get().log((fme == null ? "A server admin" : fme.getName()) + " " + change + " the faction \"" + faction.getTag() + "\".");

        // Inform all players
        for (FPlayer fplayer : FPlayerColl.all(true)) {
            String blame = (fme == null ? Lang.GENERIC_SERVERADMIN.toString() : fme.describeTo(fplayer, true));
            if (fplayer.getFaction() == faction) {
                fplayer.msg(Lang.COMMAND_PERMANENT_YOURS, blame, change);
            } else {
                fplayer.msg(Lang.COMMAND_PERMANENT_OTHER, blame, change, faction.getTag(fplayer));
            }
        }
    }

    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_PERMANENT_DESCRIPTION.toString();
    }
}
