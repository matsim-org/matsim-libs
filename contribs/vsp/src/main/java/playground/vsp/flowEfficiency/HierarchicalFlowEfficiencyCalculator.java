/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.flowEfficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.lanes.Lane;

import javax.annotation.Nullable;
import java.util.LinkedList;

/**
 * a {@link FlowEfficiencyCalculator} that holds several implementations of {@link FlowEfficiencyCalculator} in hierarchical order, called {@link SituationalFlowEfficiencyImpact}.
 * Each of these impacts the resulting flow efficiency (by multiplication). Depending on the situation, a  {@link SituationalFlowEfficiencyImpact} can prevent any following
 * impacts in the hierarchical order.
 *
 * @author tschlenther
 */
public final class HierarchicalFlowEfficiencyCalculator implements FlowEfficiencyCalculator {

	private final LinkedList<SituationalFlowEfficiencyImpact> situationalImpacts;

	public HierarchicalFlowEfficiencyCalculator(LinkedList<SituationalFlowEfficiencyImpact> situationalImpacts) {
		this.situationalImpacts = situationalImpacts;
	}

	@Override
	public double calculateFlowEfficiency(QVehicle qVehicle, @Nullable QVehicle previousQVehicle, @Nullable Double timeGapToPreviousVeh, Link link, Id<Lane> laneId) {
		double flowEfficiencyFactor = qVehicle.getVehicle().getType().getFlowEfficiencyFactor();

		for (SituationalFlowEfficiencyImpact situationalImpact : this.situationalImpacts) {
			flowEfficiencyFactor *= situationalImpact.calculateFlowEfficiency(qVehicle, previousQVehicle, timeGapToPreviousVeh, link, laneId);
			if(situationalImpact.isFinalImpact(qVehicle, previousQVehicle, timeGapToPreviousVeh, link, laneId)) break;
		}

		return flowEfficiencyFactor;
	}
}
