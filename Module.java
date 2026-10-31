package dev.vortex.module;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;
    public boolean favorite;
    /** GLFW key code, or -1 for none. */
    public int keybind = -1;
    private final boolean defaultEnabled;

    public Module(String id, String name, String description, Category category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        this.defaultEnabled = defaultEnabled;
    }

    public <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public void toggle() { enabled = !enabled; }

    public void resetToDefaults() {
        enabled = defaultEnabled;
        keybind = -1;
        for (Setting s : settings) s.reset();
    }
}
