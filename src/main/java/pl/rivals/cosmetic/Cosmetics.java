package pl.rivals.cosmetic;

import pl.rivals.RivalsPlugin;
import pl.rivals.data.PlayerProfile;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.RivalsItem;

/** Posiadanie, zakładanie i kupowanie kosmetyków. */
public final class Cosmetics {

    private Cosmetics() {
    }

    public enum BuyResult { OK, ALREADY_OWNED, NOT_ENOUGH, NO_PROFILE }

    public static Cosmetic skinFor(PlayerProfile p, RivalsItem item) {
        if (p == null) return null;
        String id = p.skins.get(item.id);
        Cosmetic c = id == null ? null : Cosmetic.byId(id);
        return c != null && c.weapon == item && p.owned.contains(c.id) ? c : null;
    }

    public static boolean owns(PlayerProfile p, Cosmetic c) {
        return p != null && p.owned.contains(c.id);
    }

    public static boolean equipped(PlayerProfile p, Cosmetic c) {
        if (p == null) return false;
        return switch (c.type) {
            case SKIN -> c.id.equals(p.skins.get(c.weapon.id));
            case KILL_EFFECT -> c.id.equals(p.killEffect);
            case TRAIL -> c.id.equals(p.trail);
        };
    }

    /** Zakłada albo zdejmuje (gdy już założony). */
    public static void toggle(PlayerProfile p, Cosmetic c) {
        boolean on = equipped(p, c);
        switch (c.type) {
            case SKIN -> {
                if (on) p.skins.remove(c.weapon.id);
                else p.skins.put(c.weapon.id, c.id);
            }
            case KILL_EFFECT -> p.killEffect = on ? "default" : c.id;
            case TRAIL -> p.trail = on ? "default" : c.id;
        }
    }

    public static BuyResult buy(RivalsPlugin plugin, PlayerProfile p, Cosmetic c) {
        if (p == null) return BuyResult.NO_PROFILE;
        if (owns(p, c)) return BuyResult.ALREADY_OWNED;
        int price = plugin.settings().price(c.id);
        if (p.coins < price) return BuyResult.NOT_ENOUGH;
        p.coins -= price;
        p.owned.add(c.id);
        toggle(p, c);
        plugin.profiles().save(p);
        return BuyResult.OK;
    }
}
