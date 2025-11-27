package org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.lanes.Lane;

import jakarta.annotation.Nullable;

/**
 * Calculator for the amount of "flow capacity" that a vehicle consumes when traveling over a link.
 */
public interface FlowEfficiencyCalculator {

    /**
     * Calculate the amount of flow capacity consumed by a vehicle.
     *
     * @param qVehicle         the vehicle that consumes efficiency.
     * @param previousQVehicle the vehicle previously traveling on this link before {@code qVehicle}, may be null
     * @param timeGapToPreviousVeh time gap to the previous vehicle traveling on this {@code link}. May be null if {@code qVehicle} is the first to travel on {@code link}.
     * @param link             the link the {@code qVehicle} is currently on.
     * @param laneId           id of the lane the {@code qVehicle} is traveling on
     */
    double calculateFlowEfficiency(QVehicle qVehicle, @Nullable QVehicle previousQVehicle, @Nullable Double timeGapToPreviousVeh, Link link, Id<Lane> laneId);
}
