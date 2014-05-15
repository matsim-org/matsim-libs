/* *********************************************************************** *
 * project: org.matsim.*
 * QSimFactory.java
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

package playground.agarwalamit.patnaIndia;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * The MobsimFactory is necessary so that something can be passed to the controler which instantiates this.
 * Can (presumably) be something much more minimalistic than QSimI.  kai, jun'10
 *
 * @author dgrether
 *
 */
public class PatnaQSimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(PatnaQSimFactory.class);
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		//        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		//        if (conf == null) {
		//            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		//        }

		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
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

		log.warn("asking for time dep network") ;
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			log.warn("enabling time dep network") ;
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(new IdImpl("car"));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);

		VehicleType motorcycle = VehicleUtils.getFactory().createVehicleType(new IdImpl("motorbike"));
		motorcycle.setMaximumVelocity(60.0/3.6);
		motorcycle.setPcuEquivalents(0.25);
		modeVehicleTypes.put("motorbike", motorcycle);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(new IdImpl("bike"));
		bicycle.setMaximumVelocity(15.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bike", bicycle);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(new IdImpl("walk"));
		walk.setMaximumVelocity(1.5);
		walk.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
		modeVehicleTypes.put("walk", walk);

		agentSource.setModeVehicleTypes(modeVehicleTypes);

		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
