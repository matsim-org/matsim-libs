/* *********************************************************************** *
 * project: org.matsim.*
 * KtiNodeNetworkRouteImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;

/**
 * Temporary solution to calculate the route distance as it is simulated in the JEDQSim.
 * 
 * TODO Generalize in MATSim that routes are handled consistently with their interpretation in the traffic simulation.
 * 
 * @author meisterk
 *
 */
public class KtiNodeNetworkRouteImpl extends NodeNetworkRouteImpl {

	final private PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;

	public KtiNodeNetworkRouteImpl(Link fromLink, Link toLink, SimLegInterpretation simLegInterpretation) {
		super(fromLink, toLink);
		this.simLegInterpretation = simLegInterpretation;
	}

	@Override
	public double calcDistance() {

		double distance = super.calcDistance();

		if (!this.getStartLink().equals(this.getEndLink())) {
			PlanomatConfigGroup.SimLegInterpretation simLegInterpretation = this.simLegInterpretation;
			switch (simLegInterpretation) {
			case CetinCompatible:
				distance += this.getEndLink().getLength();
				break;
			case CharyparEtAlCompatible:
				distance += this.getStartLink().getLength();
				break;
			}
		}
		return distance;

	}

}
