package pl.rivals.gadget;

import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;

/** Gadżet - czwarty slot zestawu. */
public abstract class Gadget {

    public final RivalsItem item;

    protected Gadget(RivalsItem item) {
        this.item = item;
    }

    /**
     * Użycie gadżetu (PPM).
     * @return true = zużyto ładunek (start cooldownu); false = nic się nie stało albo gadżet sam zarządza cooldownem
     */
    public abstract boolean use(GadgetUse u);

    public int cooldownTicks(RivalsPlugin plugin) {
        return plugin.settings().statTicks(item, "cooldown");
    }
}
