/* *********************************************************************** *
 * project: org.matsim.*
 * IncidentGenerator.java
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

/**
 * 
 */
package playground.johannes.itsc08;

import java.util.LinkedList;
import java.util.List;

import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;

/**
 * @author illenberger
 *
 */
public class IncidentGenerator implements StartupListener, BeforeMobsimListener {

	private NetworkChangeEvent badEvent;
	
	private List<NetworkChangeEvent> badEvents;
	
	private List<NetworkChangeEvent> emptyEvents = new LinkedList<NetworkChangeEvent>();

	private double capacityFactor;
	
	public IncidentGenerator(double capacityFactor) {
		this.capacityFactor = capacityFactor;
	}
	
	public void notifyStartup(StartupEvent event) {
		badEvent = new NetworkChangeEvent(0);
		badEvent.addLink(event.getControler().getNetwork().getLink("2"));
		badEvent.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacityFactor));
//		badEvent.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 0.3));
		badEvents = new LinkedList<NetworkChangeEvent>();
		badEvents.add(badEvent);
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {	
		if(event.getIteration() > -1) {
			if(event.getIteration() % 2 == 0)
				event.getControler().getNetwork().setNetworkChangeEvents(badEvents);
			else
				event.getControler().getNetwork().setNetworkChangeEvents(emptyEvents);
		}
	}	
}
