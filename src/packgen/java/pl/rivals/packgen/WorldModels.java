package pl.rivals.packgen;

import pl.rivals.shared.WorldModel;

import java.util.EnumMap;
import java.util.Map;

import static pl.rivals.packgen.Pal.*;

/**
 * Modele display entities w Äąâ€şwiecie (skala 1:1, 16 px = 1 blok, Äąâ€şrodek (8,8,8) = pozycja bytu)
 * oraz ikony GUI. Elementy kierunkowe (rakieta, beÄąâ€št, ogniwo Äąâ€šaÄąâ€žcucha) sĂ„â€¦ uÄąâ€šoÄąÄ˝one wzdÄąâ€šuÄąÄ˝ osi Z,
 * ÄąÄ˝eby obrÄ‚Ĺ‚t bytu (yaw/pitch) kierowaÄąâ€š je wzdÄąâ€šuÄąÄ˝ toru lotu.
 */
public final class WorldModels {

    private WorldModels() {
    }

    public static Map<WorldModel, Model> all() {
        Map<WorldModel, Model> map = new EnumMap<>(WorldModel.class);
        for (WorldModel wm : WorldModel.values()) map.put(wm, build(wm));
        return map;
    }

    private static Model w(WorldModel wm) {
        return new Model(wm.path, wm.path.startsWith("ui/") ? Model.Kind.UI : Model.Kind.WORLD);
    }

    private static Model build(WorldModel wm) {
        Model m = w(wm);
        switch (wm) {
            case CAGE_MINE_WORLD -> {
                m.box(4, 7, 5.5f, 12, 8, 10.5f, TRIM);
                m.box(5.5f, 7, 4, 10.5f, 8, 12, TRIM);
                m.box(5.5f, 8, 5.5f, 10.5f, 8.4f, 10.5f, BODY2);
                m.anim(7.5f, 8.4f, 7.5f, 8.5f, 8.8f, 8.5f, "led_blink", RED);
            }
            case CAGE_BAR -> {
                m.box(7.5f, -8, 7.5f, 8.5f, 24, 8.5f, BODY2);
                m.glow(7.25f, 23, 7.25f, 8.75f, 24, 8.75f, ACCENT);
                m.box(7.25f, -8, 7.25f, 8.75f, -7, 8.75f, TRIM);
            }
            case NET_BALL -> {
                m.box(6, 6, 6, 10, 10, 10, LIGHT);
                m.box(5.75f, 7.5f, 5.75f, 10.25f, 8.5f, 10.25f, BODY2);
                m.box(7.5f, 5.75f, 5.75f, 8.5f, 10.25f, 10.25f, BODY2);
            }
            case NET_OPEN -> {
                for (int i = 0; i <= 4; i++) {
                    float p = i * 4 - 0.25f;
                    m.box(p, 7.75f, 0, p + 0.5f, 8.25f, 16, LIGHT);
                    m.box(0, 7.75f, p, 16, 8.25f, p + 0.5f, LIGHT);
                }
                m.box(-0.75f, 7, -0.75f, 1.25f, 9, 1.25f, TRIM);
                m.box(14.75f, 7, -0.75f, 16.75f, 9, 1.25f, TRIM);
                m.box(-0.75f, 7, 14.75f, 1.25f, 9, 16.75f, TRIM);
                m.box(14.75f, 7, 14.75f, 16.75f, 9, 16.75f, TRIM);
            }
            case NET_WRAPPED -> {
                for (float y : new float[]{-6, 0, 6, 12}) m.ringXZ(1, 1, 15, 15, y, y + 0.6f, 0.5f, LIGHT, false);
                for (float x : new float[]{1, 14.5f}) for (float z : new float[]{1, 14.5f}) m.box(x, -6, z, x + 0.5f, 13, z + 0.5f, LIGHT);
                m.box(7.75f, -6, 0.9f, 8.25f, 13, 1.4f, LIGHT);
                m.box(7.75f, -6, 14.6f, 8.25f, 13, 15.1f, LIGHT);
            }
            case FLASHBANG_ARMED -> {
                return HandItems.flashbang(false, wm.path);
            }
            case CHAIN_LINK -> {
                m.box(6, 7.5f, 5, 10, 8.5f, 6, BODY2);
                m.box(6, 7.5f, 10, 10, 8.5f, 11, BODY2);
                m.box(6, 7.5f, 6, 7, 8.5f, 10, BODY2);
                m.box(9, 7.5f, 6, 10, 8.5f, 10, BODY2);
                m.glow(7, 7.75f, 7.5f, 9, 8.25f, 8.5f, PURPLE);
            }
            case ROPE -> m.box(7.7f, 7.7f, 0, 8.3f, 8.3f, 16, LIGHT);
            case GRAPPLE_HOOK -> {
                m.box(7.5f, 7.5f, 4, 8.5f, 8.5f, 12, BODY2);
                m.box(8.5f, 7.5f, 10.5f, 11, 8.5f, 11.5f, LIGHT);
                m.box(5, 7.5f, 10.5f, 7.5f, 8.5f, 11.5f, LIGHT);
                m.box(7.5f, 8.5f, 10.5f, 8.5f, 11, 11.5f, LIGHT);
                m.glow(7, 7, 5.5f, 9, 9, 6.5f, ACCENT);
            }
            case TRAMPOLINE_FOLDED -> {
                return HandItems.trampolineFolded(wm.path, Model.Kind.WORLD);
            }
            case TRAMPOLINE_DEPLOYED -> {
                // 1.6 bloku szerokoÄąâ€şci; y = 0 to podÄąâ€šoga (byt stoi 0.5 bloku nad podÄąâ€šogĂ„â€¦)
                m.box(-5, 6, -5, 21, 7, 21, CORE);
                m.glow(-6, 6, -6, 22, 7.5f, -5, ACCENT);
                m.glow(-6, 6, 21, 22, 7.5f, 22, ACCENT);
                m.glow(-6, 6, -5, -5, 7.5f, 21, ACCENT);
                m.glow(21, 6, -5, 22, 7.5f, 21, ACCENT);
                for (float x : new float[]{-5, 19}) for (float z : new float[]{-5, 19}) m.box(x, 0, z, x + 2, 6, z + 2, BODY2);
                m.box(2, 7, 2, 14, 7.2f, 14, TRIM);
            }
            case HOLOGRAM_BODY -> {
                int c = argb(0x70, ACCENT);
                m.glow(4, -8, 6, 7.5f, 4, 10, c);
                m.glow(8.5f, -8, 6, 12, 4, 10, c);
                m.glow(4, 4, 5.5f, 12, 16, 10.5f, c);
                m.glow(1, 4.5f, 6.5f, 4, 15, 9.5f, c);
                m.glow(12, 4.5f, 6.5f, 15, 15, 9.5f, c);
                m.glow(4.5f, 16, 4.5f, 11.5f, 23, 11.5f, argb(0x90, ACCENT));
            }
            case STICKY_LIT -> {
                return HandItems.sticky(wm.path, true, Model.Kind.WORLD);
            }
            case ROCKET -> {
                m.box(7, 7, 3, 9, 9, 12, LIGHT);
                m.box(7.25f, 7.25f, 12, 8.75f, 8.75f, 14, RED);
                m.box(6, 7.75f, 3, 10, 8.25f, 5, TRIM);
                m.box(7.75f, 6, 3, 8.25f, 10, 5, TRIM);
                m.glow(7.25f, 7.25f, 2.5f, 8.75f, 8.75f, 3, ORANGE);
            }
            case SHIELD_WALL, SHIELD_WALL_CRACKED -> {
                // 3 x 2 bloki: x -16..32, y -8..24
                String anim = wm == WorldModel.SHIELD_WALL ? "energy" : "energy_crack";
                m.anim(-15.5f, -7, 7.75f, 31.5f, 23.5f, 8.25f, anim, argb(0x90, ACCENT));
                m.box(-16, -8, 7.5f, 32, -7, 8.5f, BODY2);
                m.box(-16, 23.5f, 7.5f, 32, 24, 8.5f, BODY2);
                m.box(-16, -7, 7.5f, -15.5f, 23.5f, 8.5f, BODY2);
                m.box(31.5f, -7, 7.5f, 32, 23.5f, 8.5f, BODY2);
                m.glow(-14, -8.2f, 7.4f, 30, -7.8f, 8.6f, wm == WorldModel.SHIELD_WALL ? ACCENT : RED);
            }
            case MIRROR_PANE -> {
                m.box(-4, -8, 7.25f, 20, 24, 8.75f, GOLD_DARK);
                m.glow(-3, -7, 8.75f, 19, 23, 8.9f, argb(0xC0, ICE));
                m.glow(-3, -7, 7.1f, 19, 23, 7.25f, argb(0xC0, ICE));
                m.glow(0, 18, 8.9f, 3, 21, 9, WHITE);
            }
            case MEDKIT_BASE -> {
                m.box(3, 0, 4, 13, 5, 12, WHITE);
                m.box(3, 2.5f, 3.8f, 13, 3, 12.2f, LIGHT);
                m.glow(4, 4.9f, 5, 12, 5, 11, argb(0xC0, GREEN));
                m.glow(7, 1, 12, 9, 4, 12.1f, RED);
                m.glow(6.5f, 2, 12, 9.5f, 3, 12.1f, RED);
            }
            case MEDKIT_LID -> {
                // zawias na Äąâ€şrodku modelu (8,8,8) Ă˘â‚¬â€ś obrÄ‚Ĺ‚t wokÄ‚Ĺ‚Äąâ€š osi X otwiera wieko
                m.box(3, 8, 8, 13, 9.5f, 16, WHITE);
                m.glow(7, 9.5f, 10.5f, 9, 9.6f, 13.5f, RED);
                m.glow(6.5f, 9.5f, 11, 9.5f, 9.6f, 13, RED);
                m.box(6, 9.5f, 14.5f, 10, 10.5f, 15.5f, TRIM);
            }
            case SHOCKWAVE_RING -> {
                m.glow(0, 7.5f, 0, 16, 8.5f, 1, argb(0xD0, ACCENT));
                m.glow(0, 7.5f, 15, 16, 8.5f, 16, argb(0xD0, ACCENT));
                m.glow(0, 7.5f, 1, 1, 8.5f, 15, argb(0xD0, ACCENT));
                m.glow(15, 7.5f, 1, 16, 8.5f, 15, argb(0xD0, ACCENT));
                m.glow(2.5f, 7.5f, 2.5f, 13.5f, 8.5f, 3.5f, argb(0x90, WHITE)).rot("y", 45, 8, 8, 8);
                m.glow(2.5f, 7.5f, 12.5f, 13.5f, 8.5f, 13.5f, argb(0x90, WHITE)).rot("y", 45, 8, 8, 8);
            }
            case ENERGY_BOLT -> {
                m.glow(7.5f, 7.5f, 3, 8.5f, 8.5f, 13, ACCENT);
                m.glow(7, 7, 12, 9, 9, 14, WHITE);
                m.glow(7.25f, 7.25f, 1, 8.75f, 8.75f, 3, argb(0x80, ACCENT));
            }
            case CROSSBOW_BOLT -> {
                m.box(7.75f, 7.75f, 2, 8.25f, 8.25f, 14, TRIM);
                m.box(7.5f, 7.5f, 13, 8.5f, 8.5f, 15, LIGHT);
                m.glow(7.9f, 7, 2, 8.1f, 9, 4, ACCENT);
                m.glow(7, 7.9f, 2, 9, 8.1f, 4, ACCENT);
            }
            case PEBBLE -> {
                m.box(7, 7, 7, 9, 9, 9, BODY2);
                m.box(6.5f, 7.25f, 7.25f, 9.5f, 8.75f, 8.75f, LIGHT);
            }
            case COIN -> {
                // pÄąâ€šaska moneta w pÄąâ€šaszczyÄąĹźnie XY Ă˘â‚¬â€ś obraca siĂ„â„˘ wokÄ‚Ĺ‚Äąâ€š osi Y
                m.box(4, 4, 7.5f, 12, 12, 8.5f, GOLD);
                m.box(3, 5, 7.6f, 13, 11, 8.4f, GOLD_LIGHT);
                m.box(5, 3, 7.6f, 11, 13, 8.4f, GOLD_LIGHT);
                m.box(7, 6, 7.3f, 9, 10, 8.7f, GOLD_DARK);
                m.box(6, 7, 7.3f, 10, 9, 8.7f, GOLD_DARK);
                m.glow(7.5f, 7.5f, 7.2f, 8.5f, 8.5f, 8.8f, GOLD_LIGHT);
            }
            case CAPSULE_BOTTOM -> {
                // kapsuÄąâ€ša ~1.25 bloku: oÄąâ€şmiokĂ„â€¦tna podstawa, jasny korpus, Äąâ€şwiecĂ„â€¦cy pas
                m.box(0, 0, -1, 16, 10, 17, BODY);
                m.box(-1, 0, 0, 17, 10, 16, BODY);
                m.box(-2, 1, 2, 18, 9, 14, BODY);
                m.box(2, 1, -2, 14, 9, 18, BODY);
                m.box(-1.5f, 0, -1.5f, 17.5f, 1.5f, 17.5f, CORE);
                m.glow(-2.1f, 6.5f, 3, 18.1f, 7.5f, 13, ACCENT);
                m.glow(3, 6.5f, -2.1f, 13, 7.5f, 18.1f, ACCENT);
                m.box(-2.2f, 3, 6.5f, 18.2f, 5, 9.5f, TRIM);
                m.box(6.5f, 3, -2.2f, 9.5f, 5, 18.2f, TRIM);
                m.box(1, 9.9f, 1, 15, 10.1f, 15, CORE);
            }
            case CAPSULE_LID -> {
                // zawias w (8,8,8); wieko w stronĂ„â„˘ +Z (z 8..28)
                m.box(-1, 8, 9, 17, 12, 27, BODY);
                m.box(0, 8, 8, 16, 12, 28, BODY);
                m.box(1, 12, 11, 15, 14, 25, BODY2);
                m.box(3, 14, 13, 13, 15, 23, BODY);
                m.glow(6, 15, 16, 10, 15.4f, 20, ACCENT);
                m.box(-1.2f, 9, 17, 17.2f, 10, 19, TRIM);
            }
            case HEAL_ORB -> {
                m.glow(5, 5, 5, 11, 11, 11, argb(0xA0, GREEN));
                m.glow(7, 6, 7, 9, 10, 9, WHITE);
                m.glow(6, 7, 7, 10, 9, 9, WHITE);
            }
            case GADGET_ORB -> {
                m.glow(5, 5, 5, 11, 11, 11, argb(0xA0, GOLD));
                m.glow(7, 6, 7, 9, 10, 9, WHITE);
                m.glow(6.5f, 9, 7, 9.5f, 10, 9, WHITE);
            }
            case FAN_HOUSING -> {
                // obudowa 2x2 bloki z kratkĂ„â€¦ z listew (Äąâ€šopatki to osobny model)
                m.box(-8, -8, 6, 24, -6, 10, BODY);
                m.box(-8, 22, 6, 24, 24, 10, BODY);
                m.box(-8, -6, 6, -6, 22, 10, BODY);
                m.box(22, -6, 6, 24, 22, 10, BODY);
                m.box(-6, -6, 9.5f, 22, 22, 10, CORE);
                for (int i = 0; i < 6; i++) {
                    float y = -4 + i * 5;
                    m.box(-6, y, 5.6f, 22, y + 0.8f, 6, BODY2);
                }
                m.box(6, 6, 6, 10, 10, 9.5f, TRIM);
                m.glow(-8.2f, -8.2f, 9.6f, -5.8f, -5.8f, 10.2f, ACCENT);
                m.glow(21.8f, 21.8f, 9.6f, 24.2f, 24.2f, 10.2f, ACCENT);
                m.glow(-8.2f, 21.8f, 9.6f, -5.8f, 24.2f, 10.2f, ACCENT);
                m.glow(21.8f, -8.2f, 9.6f, 24.2f, -5.8f, 10.2f, ACCENT);
            }
            case FAN_BLADES -> {
                m.box(-5, 7, 7.5f, 21, 9, 8.5f, LIGHT);
                m.box(7, -5, 7.5f, 9, 21, 8.5f, LIGHT);
                m.box(-2, 7.2f, 7.4f, 18, 8.8f, 8.6f, BODY2).rot("z", 45, 8, 8, 8);
                m.box(7.2f, -2, 7.4f, 8.8f, 18, 8.6f, BODY2).rot("z", 45, 8, 8, 8);
                m.glow(6, 6, 7, 10, 10, 9, ACCENT);
            }
            case LAMP_ON, LAMP_OFF -> {
                boolean on = wm == WorldModel.LAMP_ON;
                m.box(6, -8, 6, 10, -7, 10, BODY2);
                m.box(7.5f, -7, 7.5f, 8.5f, 12, 8.5f, TRIM);
                m.box(5.5f, 12, 5.5f, 10.5f, 13, 10.5f, CORE);
                if (on) m.glow(6, 13, 6, 10, 16, 10, ACCENT);
                else m.box(6, 13, 6, 10, 16, 10, ACCENT_DARK);
                m.box(5.5f, 16, 5.5f, 10.5f, 17, 10.5f, CORE);
            }
            case LOGO -> {
                // pÄąâ€šytka z literĂ„â€¦ R widocznĂ„â€¦ z obu stron
                m.box(0, 0, 7.5f, 16, 16, 8.5f, CORE);
                m.box(-0.5f, -0.5f, 7.3f, 16.5f, 0.5f, 8.7f, BODY);
                m.box(-0.5f, 15.5f, 7.3f, 16.5f, 16.5f, 8.7f, BODY);
                m.box(-0.5f, 0.5f, 7.3f, 0.5f, 15.5f, 8.7f, BODY);
                m.box(15.5f, 0.5f, 7.3f, 16.5f, 15.5f, 8.7f, BODY);
                m.glow(3, 2, 6.8f, 5, 14, 9.2f, ACCENT);
                m.glow(5, 12, 6.8f, 10, 14, 9.2f, ACCENT);
                m.glow(10, 9, 6.8f, 12, 13, 9.2f, ACCENT);
                m.glow(5, 8, 6.8f, 10, 10, 9.2f, ACCENT);
                m.glow(9, 2, 6.8f, 11, 8, 9.2f, ACCENT).rot("z", 22.5f, 10, 8, 8);
            }
            case PIPE -> {
                m.box(-8, 6, 6, 24, 10, 10, BODY2);
                m.box(-8.5f, 5.5f, 5.5f, -6.5f, 10.5f, 10.5f, TRIM);
                m.box(22.5f, 5.5f, 5.5f, 24.5f, 10.5f, 10.5f, TRIM);
                m.box(7, 5.5f, 5.5f, 9, 10.5f, 10.5f, TRIM);
                m.glow(7.5f, 10.5f, 7.5f, 8.5f, 11.5f, 8.5f, RED);
                m.box(6.5f, 11.5f, 6.5f, 9.5f, 12, 9.5f, BODY2);
            }
            case SCREEN -> {
                m.box(-8, -2, 7, 24, 18, 9, CORE);
                m.anim(-7, -1, 9, 23, 17, 9.1f, "screen", ACCENT);
                m.box(6, -8, 7.5f, 10, -2, 8.5f, TRIM);
            }
            case PEDESTAL_TOP -> {
                m.box(1, 0, 1, 15, 2, 15, BODY2);
                m.ringXZ(0.5f, 0.5f, 15.5f, 15.5f, 1.5f, 2.3f, 0.6f, ACCENT, true);
                m.box(4, 2, 4, 12, 2.4f, 12, TRIM);
            }
            // UI
            case UI_PLAY -> {
                m.gui(0, 0, 0);
                m.box(2, 4, 7, 14, 12, 7.9f, CORE);                  // plakietka (oÄąâ€şmiokĂ„â€¦t)
                m.box(4, 2, 7, 12, 14, 7.9f, CORE);
                m.box(3, 3, 7, 13, 13, 8, CORE);
                m.glow(4, 2, 7.9f, 12, 2.5f, 8.1f, ACCENT);
                m.glow(4, 13.5f, 7.9f, 12, 14, 8.1f, ACCENT);
                m.glow(2, 4, 7.9f, 2.5f, 12, 8.1f, ACCENT);
                m.glow(13.5f, 4, 7.9f, 14, 12, 8.1f, ACCENT);
                m.box(6, 5, 8, 7.5f, 11, 8.6f, WHITE);               // trÄ‚Ĺ‚jkĂ„â€¦t
                m.box(7.5f, 5.75f, 8, 9, 10.25f, 8.6f, WHITE);
                m.box(9, 6.5f, 8, 10.5f, 9.5f, 8.6f, WHITE);
                m.box(10.5f, 7.25f, 8, 11.5f, 8.75f, 8.6f, WHITE);
            }
            case UI_KIT -> {
                m.box(2, 3, 5, 14, 11, 11, BODY2);
                m.box(2, 11, 5, 14, 12, 11, TRIM);
                m.box(6, 12, 7.5f, 10, 13.5f, 8.5f, TRIM);
                m.glow(7, 6, 11, 9, 9, 11.1f, ACCENT);
            }
            case UI_SHOP -> {
                m.box(3, 2, 5, 13, 11, 11, GOLD);
                m.box(5, 11, 7, 6, 14, 9, GOLD_DARK);
                m.box(10, 11, 7, 11, 14, 9, GOLD_DARK);
                m.box(5, 13.5f, 7, 11, 14.5f, 9, GOLD_DARK);
                m.glow(7, 5, 11, 9, 8, 11.1f, GOLD_LIGHT);
            }
            case UI_PROFILE -> {
                m.gui(10, -20, 0);
                m.box(3, 2, 6, 13, 8, 10, BODY2);                    // ramiona
                m.box(5, 7.5f, 6.5f, 11, 8.5f, 9.5f, TRIM);
                m.glow(7, 3, 10, 9, 7, 10.1f, ACCENT);               // pas na piersi
                m.box(5.5f, 8.5f, 6, 10.5f, 13.5f, 10, BODY);        // gÄąâ€šowa
                m.box(5.3f, 13, 5.8f, 10.7f, 14, 10.2f, TRIM);
                m.glow(6, 10.5f, 10, 10, 11.8f, 10.1f, ACCENT);      // wizjer
            }
            case UI_READY -> {
                m.gui(0, 0, 0);
                m.box(2, 4, 7, 14, 12, 7.9f, CORE);
                m.box(4, 2, 7, 12, 14, 7.9f, CORE);
                m.box(3, 3, 7, 13, 13, 8, CORE);
                m.glow(3.6f, 6, 8, 7.6f, 7.6f, 8.6f, GREEN).rot("z", -45, 5.6f, 6.8f, 8);
                m.glow(5.8f, 7.2f, 8, 13.4f, 8.8f, 8.6f, GREEN).rot("z", 45, 9.6f, 8, 8);
            }
            case UI_LOCKED -> {
                m.box(3, 2, 6, 13, 9, 10, GOLD_DARK);
                m.box(4, 9, 7, 5.5f, 13, 9, BODY2);
                m.box(10.5f, 9, 7, 12, 13, 9, BODY2);
                m.box(4, 12.5f, 7, 12, 14, 9, BODY2);
                m.box(7.25f, 4, 10, 8.75f, 7, 10.1f, INK);
            }
            case UI_TAB_SKINS -> {
                m.box(7, 1, 7, 9, 4, 9, CORE);
                m.box(5, 4, 7, 11, 5, 9, BODY2);
                m.box(7, 5, 7.5f, 9, 15, 8.5f, LIGHT);
                m.glow(7.9f, 5, 7.4f, 8.1f, 15, 8.6f, PURPLE);
            }
            case UI_TAB_EFFECTS -> {
                m.glow(7, 2, 7, 9, 14, 9, GOLD);
                m.glow(2, 7, 7, 14, 9, 9, GOLD);
                m.glow(4, 4, 7.2f, 12, 12, 8.8f, GOLD_LIGHT).rot("z", 45, 8, 8, 8);
                m.glow(6.5f, 6.5f, 6.8f, 9.5f, 9.5f, 9.2f, WHITE);
            }
            case UI_TAB_TRAILS -> {
                m.glow(1, 6, 7.5f, 6, 7, 8.5f, argb(0x80, ACCENT));
                m.glow(3, 9, 7.5f, 8, 10, 8.5f, argb(0xB0, ACCENT));
                m.box(8, 5, 6, 14, 11, 10, BODY2);
                m.glow(7, 6, 6.5f, 8, 10, 9.5f, ACCENT);
            }
        }
        return m;
    }
}
