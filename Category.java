package dev.vortex.module;

public enum Category {
    PERFORMANCE("Performance"),
    HUD("HUD"),
    VISUALS("Visuals"),
    PVP("PvP"),
    UTILITY("Utility"),
    MISC("Miscellaneous");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
