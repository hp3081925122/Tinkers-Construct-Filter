package org.hp.tinkers_construct_filter.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;

final class TinkersFilterKubeJsEvents {
    private static final EventGroup GROUP = EventGroup.of("TinkersFilterEvents");
    private static final EventHandler REGISTER = GROUP.client("register", () -> TinkersFilterRegistrationEvent.class);

    private TinkersFilterKubeJsEvents() {
    }

    static void register() {
        GROUP.register();
    }

    static void postRegistration() {
        REGISTER.post(ScriptType.CLIENT, new TinkersFilterRegistrationEvent());
    }
}
