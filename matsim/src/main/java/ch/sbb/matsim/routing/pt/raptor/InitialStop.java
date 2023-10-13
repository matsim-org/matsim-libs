/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Specifies the access or egress time and costs for a specific TransitStopFacility and a specific mode.
 *
 * @author mrieser / SBB
 */
public class InitialStop {

    final TransitStopFacility stop;
    final double accessCost;
    final double accessTime;
    final double distance;
    final String mode;
    final List<? extends PlanElement> planElements;

    public InitialStop(TransitStopFacility stop, double accessCost, double accessTime, double distance, String mode) {
        this.stop = stop;
        this.accessCost = accessCost;
        this.accessTime = accessTime;
        this.distance = distance;
        this.mode = mode;
        this.planElements = null;
    }

    public InitialStop(TransitStopFacility stop, double accessCost, double accessTime, List<? extends PlanElement> planElements) {
        this.stop = stop;
        this.accessCost = accessCost;
        this.accessTime = accessTime;
        this.distance = Double.NaN;
        this.mode = null;
        this.planElements = planElements;
    }

	@Override
	public String toString() {
		return "[ stopId=" + stop.getId() + " | accessCost=" + accessCost + " | mode=" + mode + " ]" ;
	}
}
