package dev.vortex.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** A single persisted, validated module setting. Kinds: BOOL, INT, FLOAT, CHOICE. */
public final class Setting {
    public enum Kind { BOOL, INT, FLOAT, CHOICE }

    public final String key;
    public final String label;
    public final Kind kind;
    public final double min, max;
    public final List<String> choices;
    private final Object def;
    private Object value;

    private Setting(String key, String label, Kind kind, Object def, double min, double max, List<String> choices) {
        this.key = key;
        this.label = label;
        this.kind = kind;
        this.def = def;
        this.value = def;
        this.min = min;
        this.max = max;
        this.choices = choices;
    }

    public static Setting bool(String key, String label, boolean def) {
        return new Setting(key, label, Kind.BOOL, def, 0, 1, null);
    }

    public static Setting integer(String key, String label, int def, int min, int max) {
        return new Setting(key, label, Kind.INT, def, min, max, null);
    }

    public static Setting number(String key, String label, float def, float min, float max) {
        return new Setting(key, label, Kind.FLOAT, def, min, max, null);
    }

    public static Setting choice(String key, String label, String def, List<String> choices) {
        return new Setting(key, label, Kind.CHOICE, def, 0, choices.size() - 1, choices);
    }

    public boolean getBool() { return (Boolean) value; }
    public int getInt() { return (Integer) value; }
    public float getFloat() { return (Float) value; }
    public String getChoice() { return (String) value; }

    public void setBool(boolean v) { value = v; }
    public void setInt(int v) { value = Math.max((int) min, Math.min((int) max, v)); }
    public void setFloat(float v) { value = Math.max((float) min, Math.min((float) max, v)); }
    public void setChoice(String v) { if (choices.contains(v)) value = v; }

    public void cycle() {
        if (kind == Kind.BOOL) setBool(!getBool());
        else if (kind == Kind.CHOICE) setChoice(choices.get((choices.indexOf(getChoice()) + 1) % choices.size()));
    }

    public void reset() { value = def; }

    public JsonElement toJson() {
        return switch (kind) {
            case BOOL -> new JsonPrimitive(getBool());
            case INT -> new JsonPrimitive(getInt());
            case FLOAT -> new JsonPrimitive(getFloat());
            case CHOICE -> new JsonPrimitive(getChoice());
        };
    }

    /** Invalid or wrong-typed values are ignored, leaving the current/default value in place. */
    public void fromJson(JsonElement e) {
        try {
            if (e == null || !e.isJsonPrimitive()) return;
            switch (kind) {
                case BOOL -> setBool(e.getAsBoolean());
                case INT -> setInt(e.getAsInt());
                case FLOAT -> setFloat(e.getAsFloat());
                case CHOICE -> setChoice(e.getAsString());
            }
        } catch (RuntimeException ignored) {
            // keep default
        }
    }
}
