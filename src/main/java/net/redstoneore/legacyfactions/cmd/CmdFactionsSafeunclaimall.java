package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FactionColl;

public class CmdFactionsSafeunclaimall extends FCommand {

    public CmdFactionsSafeunclaimall() {
        this.aliases.addAll(Conf.cmdAliasesSafeunclaimall);
        
        this.permission = Permission.MANAGE_SAFE_ZONE.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;

    }

    @Override
    public void perform() {
        Board.get().unclaimAll(FactionColl.get().getSafeZone().getId());
        msg(Lang.COMMAND_SAFEUNCLAIMALL_UNCLAIMED);

        if (Conf.logLandUnclaims) {
            Factions.get().log(Lang.COMMAND_SAFEUNCLAIMALL_UNCLAIMEDLOG.format(sender.getName()));
        }
    }

    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_SAFEUNCLAIMALL_DESCRIPTION.toString();
    }

}
