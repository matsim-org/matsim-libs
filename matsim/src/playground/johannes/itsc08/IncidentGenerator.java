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
import java.util.Random;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

/**
 * @author illenberger
 *
 */
public class IncidentGenerator implements StartupListener, BeforeMobsimListener {

	private NetworkChangeEvent badEvent;
	
	private List<NetworkChangeEvent> badEvents;
	
	private List<NetworkChangeEvent> emptyEvents = new LinkedList<NetworkChangeEvent>();

	private double capacityFactor;
	
	private Random random;
	
	private boolean isBadDay;
	
	public IncidentGenerator(double capacityFactor, long rndSeed) {
		this.capacityFactor = capacityFactor;
		random = new Random(rndSeed);
	}
	
	public void notifyStartup(StartupEvent event) {
		badEvent = new NetworkChangeEvent(0);
		badEvent.addLink(event.getControler().getNetwork().getLinks().get(new IdImpl("2")));
		badEvent.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacityFactor));
//		badEvent.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 0.3));
		badEvents = new LinkedList<NetworkChangeEvent>();
		badEvents.add(badEvent);
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {	
		if(event.getIteration() > -1) {
//			if(event.getIteration() % 2 == 0)
			if(random.nextDouble() <= 0.5) {
				((NetworkImpl) event.getControler().getNetwork()).setNetworkChangeEvents(badEvents);
				isBadDay = true;
			} else {
				isBadDay = false;
				((NetworkImpl) event.getControler().getNetwork()).setNetworkChangeEvents(emptyEvents);
			}
		}
	}	
	
	public boolean isBadDay() {
		return isBadDay;
	}
}
