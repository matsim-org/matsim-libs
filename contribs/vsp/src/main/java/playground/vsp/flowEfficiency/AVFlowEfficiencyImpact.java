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

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.pt.TransitQVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.VehicleType;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * this class is experimental! please do not use without caution!
 * tschlenther dec '20
 * @author tschlenther
 */
public class AVFlowEfficiencyImpact implements SituationalFlowEfficiencyImpact {

	private final Set<VehicleType> autonomousVehicleTypes;

	public AVFlowEfficiencyImpact(Set<VehicleType> autonomousVehicleTypes) {
		this.autonomousVehicleTypes = autonomousVehicleTypes;
	}

	@Override
	public double calculateFlowEfficiency(QVehicle qVehicle, @Nullable QVehicle previousQVehicle, @Nullable Double timeGapToPreviousVeh, Link link, Id<Lane> laneId) {

		// currently, we can not call chooseNextLinkId for TransitQVehicles because no link Id caching is performed!
		if(qVehicle instanceof TransitQVehicle) { return 1;}

		Id<Link> nextLinkId = qVehicle.getDriver().chooseNextLinkId();
		if(nextLinkId != null && //vehicle is not arriving
				this.autonomousVehicleTypes.contains(qVehicle.getVehicle().getType())){
			if(link.getAttributes().getAttribute("turns") != null) {
				Map<String, String> turns = (Map<String, String>) link.getAttributes().getAttribute("turns");
				String direction = turns.get(nextLinkId.toString());
				Preconditions.checkNotNull(direction, "could not find toLinkId=" + nextLinkId + " in turns map of link " + link);

				switch(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.valueOf(direction)){
					case STRAIGHT:
						return 2.0; //double efficiency
					case LEFT:
						return 0.5; //half efficiency
					case RIGHT:
						return 0.5; //half efficiency
					case UNKNOWN:
						return 1.0; //no impact - not a clear situation
					case UTURN:
						return 0.5;
					default:
						throw new RuntimeException("could not deal with direction=" + direction + " from link " + link + " to link " + nextLinkId);
				}
			} else {
				throw new RuntimeException("currently, " + this.getClass() + " only works if turns are provided for each link.");
			}
		}
		return 1; //no impact
	}

	@Override
	public boolean isFinalImpact(QVehicle qVehicle, QVehicle previousQVehicle, Double previousTimeDiff, Link link, Id<Lane> laneId) {
		return false;
	}
}
