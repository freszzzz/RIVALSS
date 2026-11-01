package pl.rivals.gadget;

import pl.rivals.shared.RivalsItem;

import java.util.EnumMap;
import java.util.Map;

/** Rejestr wszystkich 16 gadżetów. */
public final class Gadgets {

    private final Map<RivalsItem, Gadget> map = new EnumMap<>(RivalsItem.class);

    public Gadgets() {
        register(new ControlGadgets.Cryo());
        register(new ControlGadgets.Cage());
        register(new ControlGadgets.Net());
        register(new ControlGadgets.Flash());
        register(new ControlGadgets.Anchor());
        register(new MovementGadgets.Grapple());
        register(new MovementGadgets.Trampoline());
        register(new MovementGadgets.Recall());
        register(new MovementGadgets.Dash());
        register(new DamageGadgets.Boomerang());
        register(new DamageGadgets.Sticky());
        register(new DamageGadgets.Rocket());
        register(new DefenseGadgets.Shield());
        register(new DefenseGadgets.Decoy());
        register(new DefenseGadgets.Mirror());
        register(new DefenseGadgets.Medkit());
    }

    private void register(Gadget g) {
        map.put(g.item, g);
    }

    public Gadget get(RivalsItem item) {
        return map.get(item);
    }
}
