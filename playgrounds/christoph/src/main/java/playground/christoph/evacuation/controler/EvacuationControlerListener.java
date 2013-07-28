/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.controller.WithinDayControlerListener;

import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;

public class EvacuationControlerListener implements StartupListener {

	private final WithinDayControlerListener withinDayControlerListener;
	private final MultiModalControlerListener multiModalControlerListener;
	
	protected JointDepartureOrganizer jointDepartureOrganizer;
	
	public EvacuationControlerListener(WithinDayControlerListener withinDayControlerListener, 
			MultiModalControlerListener multiModalControlerListener) {
		this.withinDayControlerListener = withinDayControlerListener;
		this.multiModalControlerListener = multiModalControlerListener;
		
		this.jointDepartureOrganizer = new JointDepartureOrganizer();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {

		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household and adds the replanning Manager as mobsim engine.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory(this.withinDayControlerListener.getWithinDayEngine(), 
				this.jointDepartureOrganizer, this.multiModalControlerListener.getMultiModalTravelTimes());
		event.getControler().setMobsimFactory(mobsimFactory);
	}

}
