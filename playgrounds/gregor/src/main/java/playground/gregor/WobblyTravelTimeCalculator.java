/* *********************************************************************** *
 * project: org.matsim.*
 * WobblyTravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

public class WobblyTravelTimeCalculator extends TravelTimeCalculator {

	private double overEstimationCoefficient = 1;
	

	public WobblyTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, ttconfigGroup);
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		resetOECoeff();
	}

	@Override
	public double getLinkToLinkTravelTime(Id fromLinkId, Id toLinkId,
			double time) {
		throw new RuntimeException("not implemented yet!!");
	}


	@Override
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink,
			double time) {
		throw new RuntimeException("not implemented yet!!");
	}



	@Override
	public double getLinkTravelTime(Id linkId, double time) {
		return super.getLinkTravelTime(linkId, time)*this.overEstimationCoefficient;
	}


	@Override
	public double getLinkTravelTime(Link link, double time) {
		return getLinkTravelTime(link.getId(), time);
	}

	private void resetOECoeff() {
		double rnd = MatsimRandom.getRandom().nextDouble();
		if (rnd <= 0.33333333) {
			this.overEstimationCoefficient = 1.25;
		} else if (rnd <= 0.666666) {
			this.overEstimationCoefficient = 1;
		} else {
			this.overEstimationCoefficient = 0.95;
		}
		
	}





}
