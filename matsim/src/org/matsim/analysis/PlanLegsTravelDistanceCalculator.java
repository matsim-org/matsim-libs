/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLegsTravelDistanceCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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


package org.matsim.analysis;

import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

public class PlanLegsTravelDistanceCalculator {
	
	private double sumLegsTravelDistance = 0.0;	
	// later: distinguish between modes
	private double nbrOfLegs;
	
	public void handle(final PlanImpl plan){
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				final LegImpl leg = (LegImpl) pe;
				this.sumLegsTravelDistance += leg.getRoute().getDistance();
				this.nbrOfLegs++;
			}
		}
	}

	public double getSumLegsTravelDistance() {
		return sumLegsTravelDistance;
	}

	public void setSumLegsTravelDistance(double sumLegsTravelDistance) {
		this.sumLegsTravelDistance = sumLegsTravelDistance;
	}

	public double getNbrOfLegs() {
		return nbrOfLegs;
	}

	public void setNbrOfLegs(double nbrOfLegs) {
		this.nbrOfLegs = nbrOfLegs;
	}
}
