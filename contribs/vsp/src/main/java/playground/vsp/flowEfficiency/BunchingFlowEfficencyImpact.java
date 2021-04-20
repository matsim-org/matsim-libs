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

import static playground.vsp.flowEfficiency.LinkTurnDirectionAttributesFromGraphHopper.*;

/**
 * this class is experimental! please do not use without caution!
 * tschlenther dec '20
 * @author tschlenther
 */
public class BunchingFlowEfficencyImpact implements SituationalFlowEfficiencyImpact {

	/**
	 * determines how large the time gap between vehicles can deviate from the standard time gap at congested state (i.e. when flow capacity is fully exploited and pcu equivalents are 1)
	 */
	private final double toleratedTimeGapDeviationFactor;
	private final Set<VehicleType> bunchableVehicleTypes;
	private final double impact;

	public BunchingFlowEfficencyImpact(Set<VehicleType> bunchableVehicleTypes, double impact, double toleratedTimeGapDeviationFactor) {
		this.bunchableVehicleTypes = bunchableVehicleTypes;
		this.impact = impact;
		this.toleratedTimeGapDeviationFactor = toleratedTimeGapDeviationFactor;
	}

	@Override
	public double calculateFlowEfficiency(QVehicle qVehicle, @Nullable QVehicle previousQVehicle, @Nullable Double timeGapToPreviousVeh, Link link, Id<Lane> laneId) {

		// currently, we can not call chooseNextLinkId for TransitQVehicles because no link Id caching is performed!
		if(qVehicle instanceof TransitQVehicle) { return 1;}

		//querying the next link id forces the driver to take it (diversion can only take place after next link)
		//this might change drt performance
		Id<Link> nextLinkId = qVehicle.getDriver().chooseNextLinkId();

		if(previousQVehicle == null || timeGapToPreviousVeh == null || nextLinkId == null) return 1;

		double flowCapPerSecond = link.getFlowCapacityPerSec();
		double standardMinTimeGap = 1 / flowCapPerSecond;

		if(timeGapToPreviousVeh < standardMinTimeGap * toleratedTimeGapDeviationFactor && //is the time gap between the two vehicles small enough?
				this.bunchableVehicleTypes.contains(qVehicle.getVehicle().getType()) && //are the two vehicles able to bunch? (i.e. can they communicate or whatever..)
				this.bunchableVehicleTypes.contains(previousQVehicle.getVehicle().getType()) &&
				previousQVehicle.getCurrentLink().getId().equals(nextLinkId)){ // do they drive in the same direction?

			if(link.getAttributes().getAttribute("turns") != null){
				Map<String, String> turns = (Map<String, String>) link.getAttributes().getAttribute("turns");
				String direction = turns.get(nextLinkId.toString());
				Preconditions.checkNotNull(direction, "could not find toLinkId=" + nextLinkId + " in turns map of link " + link);

				if(direction.equals(TurnDirection.STRAIGHT.toString())){ //bunching only in straight direction
					return impact;
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
