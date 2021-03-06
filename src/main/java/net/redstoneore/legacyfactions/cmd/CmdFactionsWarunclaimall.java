package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FactionColl;

public class CmdFactionsWarunclaimall extends FCommand {

    public CmdFactionsWarunclaimall() {
        this.aliases.addAll(Conf.cmdAliasesWarunclaimall);
        
        this.permission = Permission.MANAGE_WAR_ZONE.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Board.get().unclaimAll(FactionColl.get().getWarZone().getId());
        msg(Lang.COMMAND_WARUNCLAIMALL_SUCCESS);

        if (Conf.logLandUnclaims) {
            Factions.get().log(Lang.COMMAND_WARUNCLAIMALL_LOG.format(fme.getName()));
        }
    }

    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_WARUNCLAIMALL_DESCRIPTION.toString();
    }

}
