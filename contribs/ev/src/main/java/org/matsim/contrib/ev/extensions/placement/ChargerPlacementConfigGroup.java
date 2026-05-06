package org.matsim.contrib.ev.extensions.placement;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ChargerPlacementConfigGroup extends ReflectiveConfigGroup {
    static public final String CONFIG_GROUP = "ev:charger_placement";

    public ChargerPlacementConfigGroup() {
        super(CONFIG_GROUP);
    }

    @Parameter
    private double blacklistPenalty = -1e3;

    @Parameter
    private int removalInterval = 10;

    @Parameter
    private double removalQuantile = 0.01;

    @Parameter
    private boolean removeUnused = true;

    public enum ChargerPlacementObjective {
        Revenue, Energy
    }

    private ChargerPlacementObjective objective = ChargerPlacementObjective.Energy;

    public double getBlacklistPenalty() {
        return blacklistPenalty;
    }

    public void setBlacklistPenalty(double blacklistPenalty) {
        this.blacklistPenalty = blacklistPenalty;
    }

    public int getRemovalInterval() {
        return removalInterval;
    }

    public void setRemovalInterval(int removalInterval) {
        this.removalInterval = removalInterval;
    }

    public double getRemovalQuantile() {
        return removalQuantile;
    }

    public void setRemovalQuantile(double removalQuantile) {
        this.removalQuantile = removalQuantile;
    }

    public boolean getRemoveUnused() {
        return removeUnused;
    }

    public void setRemoveUnused(boolean removeUnused) {
        this.removeUnused = removeUnused;
    }

    public ChargerPlacementObjective getObjective() {
        return objective;
    }

    public void setObjective(ChargerPlacementObjective objective) {
        this.objective = objective;
    }

    static public ChargerPlacementConfigGroup get(Config config, boolean create) {
        ChargerPlacementConfigGroup placementConfig = (ChargerPlacementConfigGroup) config.getModules()
                .get(CONFIG_GROUP);

        if (placementConfig == null && create) {
            placementConfig = new ChargerPlacementConfigGroup();
            config.addModule(placementConfig);
        }

        return placementConfig;
    }
}
