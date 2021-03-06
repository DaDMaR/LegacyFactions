package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.FLocation;
import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Role;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.Faction;

public class CmdFactionsSethome extends FCommand {

    public CmdFactionsSethome() {
        this.aliases.addAll(Conf.cmdAliasesSethome);

        this.optionalArgs.put("faction tag", "mine");

        this.permission = Permission.SETHOME.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        if (!Conf.homesEnabled) {
            fme.msg(Lang.COMMAND_SETHOME_DISABLED);
            return;
        }

        Faction faction = this.argAsFaction(0, myFaction);
        if (faction == null) {
            return;
        }

        // Can the player set the home for this faction?
        if (faction == myFaction) {
            if (!Permission.SETHOME_ANY.has(sender) && !assertMinRole(Role.MODERATOR)) {
                return;
            }
        } else {
            if (!Permission.SETHOME_ANY.has(sender, true)) {
                return;
            }
        }

        // Can the player set the faction home HERE?
        if (!Permission.BYPASS.has(me) &&
                    Conf.homesMustBeInClaimedTerritory &&
                    Board.get().getFactionAt(new FLocation(me)) != faction) {
            fme.msg(Lang.COMMAND_SETHOME_NOTCLAIMED);
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostSethome, Lang.COMMAND_SETHOME_TOSET, Lang.COMMAND_SETHOME_FORSET)) {
            return;
        }

        faction.setHome(me.getLocation());

        faction.msg(Lang.COMMAND_SETHOME_SET, fme.describeTo(myFaction, true));
        faction.sendMessage(CmdFactions.get().cmdHome.getUseageTemplate());
        if (faction != myFaction) {
            fme.msg(Lang.COMMAND_SETHOME_SETOTHER, faction.getTag(fme));
        }
    }

    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_SETHOME_DESCRIPTION.toString();
    }

}
