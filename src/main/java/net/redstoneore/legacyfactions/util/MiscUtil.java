package net.redstoneore.legacyfactions.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.Relation;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class MiscUtil {

    public static EntityType creatureTypeFromEntity(Entity entity) {
        if (!(entity instanceof Creature)) {
            return null;
        }

        String name = entity.getClass().getSimpleName();
        name = name.substring(5); // Remove "Craft"

        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException e) {
        	return null;
        }
    }

    // Inclusive range
    public static long[] range(long start, long end) {
        long[] values = new long[(int) Math.abs(end - start) + 1];

        if (end < start) {
            long oldstart = start;
            start = end;
            end = oldstart;
        }

        for (long i = start; i <= end; i++) {
            values[(int) (i - start)] = i;
        }

        return values;
    }

    /// TODO create tag whitelist!!
    public static HashSet<String> substanceChars = new HashSet<String>(Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"}));

    public static String getComparisonString(String str) {
        String ret = "";

        str = ChatColor.stripColor(str);
        str = str.toLowerCase();

        for (char c : str.toCharArray()) {
            if (substanceChars.contains(String.valueOf(c))) {
                ret += c;
            }
        }
        return ret.toLowerCase();
    }

    public static ArrayList<String> validateTag(String str) {
        ArrayList<String> errors = new ArrayList<String>();

        if (getComparisonString(str).length() < Conf.factionTagLengthMin) {
            errors.add(Factions.get().getTextUtil().parse(Lang.GENERIC_FACTIONTAG_TOOSHORT.toString(), Conf.factionTagLengthMin));
        }

        if (str.length() > Conf.factionTagLengthMax) {
            errors.add(Factions.get().getTextUtil().parse(Lang.GENERIC_FACTIONTAG_TOOLONG.toString(), Conf.factionTagLengthMax));
        }

        for (char c : str.toCharArray()) {
            if (!substanceChars.contains(String.valueOf(c))) {
                errors.add(Factions.get().getTextUtil().parse(Lang.GENERIC_FACTIONTAG_ALPHANUMERIC.toString(), c));
            }
        }

        return errors;
    }

    public static Iterable<FPlayer> rankOrder(Iterable<FPlayer> players) {
        List<FPlayer> admins = new ArrayList<FPlayer>();
        List<FPlayer> moderators = new ArrayList<FPlayer>();
        List<FPlayer> normal = new ArrayList<FPlayer>();

        for (FPlayer player : players) {
            switch (player.getRole()) {
                case ADMIN:
                    admins.add(player);
                    break;

                case MODERATOR:
                    moderators.add(player);
                    break;

                case NORMAL:
                    normal.add(player);
                    break;
            }
        }

        List<FPlayer> ret = new ArrayList<FPlayer>();
        ret.addAll(admins);
        ret.addAll(moderators);
        ret.addAll(normal);
        return ret;
    }
    
    public static LinkedHashSet<String> linkedHashSet(String... items) {
    	LinkedHashSet<String> set = new LinkedHashSet<String>();
    	
    	for (String item : items) {
    		set.add(item);
    	}
    	
    	return set;
    }
    
    public static Map<Relation, Integer> map(Relation a, Integer b, Object... extras) {
    	Map<Relation, Integer> map = new HashMap<Relation, Integer>();
    	map.put(a, b);
    	
    	Relation key = null;
    	
    	for (Object o : extras) {
    		if (key == null) {
    			key = (Relation) o;
    		} else {
    			map.put(key, (Integer) o);
    			key = null;
    		}
    	}
    	
    	return map;
    }
    
    public static Map<String, Double> map(String a, Double b, Object... extras) {
    	Map<String, Double> map = new HashMap<String, Double>();
    	map.put(a, b);
    	
    	String key = null;
    	
    	for(Object o : extras) {
    		if (key == null) {
    			key = (String) o;
    		} else {
    			map.put(key, (Double) o);
    			key = null;
    		}
    	}
    	
    	return map;
    }
    
    @SuppressWarnings("unchecked")
	public static Map<String, List<String>> map(String a, List<String> b, Object... extras) {
    	 Map<String, List<String>> map = new HashMap<String, List<String>>();
    	 map.put(a, b);
    	 
     	String key = null;
    	
     	for(Object o : extras) {
     		if (key == null) {
     			key = (String) o;
     		} else {
     			map.put(key, (List<String>) o);
     			key = null;
     		}
     	}
     	
     	return map;

    }
}

