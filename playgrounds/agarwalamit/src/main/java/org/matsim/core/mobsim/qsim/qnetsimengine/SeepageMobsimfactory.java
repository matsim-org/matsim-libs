/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;


/**
 * @author amit
 */

public class SeepageMobsimfactory  implements MobsimFactory{
	public static enum QueueWithBufferType { standard, amit, seep }

	private QueueWithBufferType queueWithBufferType; ;
	
	public SeepageMobsimfactory() {
		this.queueWithBufferType = QueueWithBufferType.standard ;
	} ;
	
	public SeepageMobsimfactory( QueueWithBufferType type ) {
		this.queueWithBufferType = type ;
	}

	static final List<String> mainModes = Arrays.asList(TransportMode.car,TransportMode.bike);
	
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager events) {
		//From QSimFactory inspired code
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		/**/
		QSim qSim = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		SeepageNetworkFactory netsimNetworkFactory = new SeepageNetworkFactory(queueWithBufferType);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory);

		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

//		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
//
//		for(String travelMode:mainModes){
//			VehicleType mode = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode,VehicleType.class));
//			mode.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(travelMode));
//			mode.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(travelMode));
//			modeVehicleTypes.put(travelMode, mode);
//		}
//
//		agentSource.setModeVehicleTypes(modeVehicleTypes);

		qSim.addAgentSource(agentSource);
		return qSim;
	}
}
