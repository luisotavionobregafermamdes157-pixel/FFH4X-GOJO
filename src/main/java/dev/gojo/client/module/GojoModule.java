package dev.gojo.client.module;

public final class GojoModule {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final boolean implemented;
    private boolean enabled;
    private int value = 50;

    public GojoModule(String name, String description, ModuleCategory category, boolean implemented) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.implemented = implemented;
    }

    public String name() { return name; }
    public String description() { return description; }
    public ModuleCategory category() { return category; }
    public boolean implemented() { return implemented; }
    public boolean enabled() { return enabled; }
    public int value() { return value; }
    public int maxValue() { return name.equals("Aimbot") ? 180 : 100; }
    public void setValue(int value) { this.value = Math.max(1, Math.min(maxValue(), value)); }
    public void setEnabled(boolean enabled) { this.enabled = implemented && enabled; }
    public void toggle() { if (implemented) enabled = !enabled; }
}
