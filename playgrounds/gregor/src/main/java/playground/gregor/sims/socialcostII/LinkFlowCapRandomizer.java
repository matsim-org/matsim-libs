/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFlowCapRandomizer.java
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
package playground.gregor.sims.socialcostII;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class LinkFlowCapRandomizer implements BeforeMobsimListener{



	private double C;
	private NetworkImpl network;
	private final double increment;

	public LinkFlowCapRandomizer(NetworkImpl network, double c, double increment) {
		this.network = network;
		this.C = c;
		this.increment = increment;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>();
		double startTime = 0 * 3600;
		double endTime = 2 * 3600;

		for (Link link : this.network.getLinks().values()) {
			double detFlow = ((LinkImpl) link).getFlowCapacity();
			for (double time = startTime; time < endTime; time += 120) {
				NetworkChangeEvent e = new NetworkChangeEvent(time);
				e.addLink(link);
				double value =  detFlow + C * (MatsimRandom.getRandom().nextDouble() - .5) * detFlow;
				ChangeValue c = new ChangeValue(ChangeType.ABSOLUTE,value);
				e.setFlowCapacityChange(c);
				events.add(e);
			}
		}
		this.network.setNetworkChangeEvents(events);
		this.C += this.increment;
	}


}
