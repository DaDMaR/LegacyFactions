package net.redstoneore.legacyfactions;

public interface EconomyParticipator extends RelationParticipator {

    public String getAccountId();

    public void msg(String str, Object... args);

    public void msg(Lang translation, Object... args);
    
}
