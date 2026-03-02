package org.matsim.contrib.ev.extensions.placement;

import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.core.events.MobsimScopeEventHandler;

/**
 * Outscoring blacklisted chargers
 */
public class ChargerPlacementScorer implements ChargingStartEventHandler, MobsimScopeEventHandler {
    private final ChargerPlacementManager manager;
    private final ChargingPlanScoring scoring;

    private final double penalty;

    public ChargerPlacementScorer(ChargerPlacementManager manager, ChargingPlanScoring scoring, double penalty) {
        this.manager = manager;
        this.scoring = scoring;
        this.penalty = penalty;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        if (manager.isBlacklisted(event.getChargerId())) {
            scoring.addScoreForVehicle(event.getVehicleId(), penalty);
            scoring.trackScoreForVehicle(event.getTime(), event.getVehicleId(), "placement", penalty, penalty);
        }
    }
}
