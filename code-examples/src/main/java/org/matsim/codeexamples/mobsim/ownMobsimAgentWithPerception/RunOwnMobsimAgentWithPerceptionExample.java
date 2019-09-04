/* *********************************************************************** *
 * project: kai
 * Main.java
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

package org.matsim.codeexamples.mobsim.ownMobsimAgentWithPerception;

import java.net.URL;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Provider;

/**
 * Untested code.  Idea is that an observer notes the traffic congestion, and returns the "best" of all outgoing links to the vehicle.
 *
 * @author nagel
 */
public class RunOwnMobsimAgentWithPerceptionExample {
	
	public static void main(String[] args) {
		
		
		Config config = null ;
		if ( args!=null && args.length > 0 && args[0]!=null  ) {
			config = ConfigUtils.loadConfig(args[0]) ;
		} else {
			URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");;
			config = ConfigUtils.loadConfig(url) ;
			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setLastIteration(0);
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler controler = new Controler( scenario ) ;
		
		// observer.  Will probably NOT need one instance per agent in order to be thread safe since the threads will only get info
		// from this but not set. However, if one wants different perceptions per agent then one also needs different observers.  Or
		// observers that are parameterized in the agents.
		final MyObserver eventsObserver = new MyObserver( controler.getScenario() ) ;
		controler.getEvents().addHandler( eventsObserver );
		
		// guidance.  Will need one instance per agent in order to be thread safe
		final MyGuidance guidance = new MyGuidance( eventsObserver, controler.getScenario() ) ;
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Inject Scenario sc ;
					@Inject EventsManager eventsManager ;
					@Override
					public Mobsim get() {
						final QSim qsim = new QSimBuilder(getConfig()).useDefaults().build(sc, eventsManager);
						
						// Why agent source instead of inserting them directly?  Inserting agents into activities is, in fact possible just
						// after the QSim constructor.  However, inserting vehicles or agents into links is not.  Agentsource makes
						// sure that this is appropriately delayed.
						qsim.addAgentSource(new AgentSource() {
							@Override
							public void insertAgentsIntoMobsim() {
								final Id<Link> startingLinkId = Id.createLinkId(1); // replace by something meaningful
								
								// insert vehicle:
								final Id<Vehicle> vehId = Id.create("myVeh", Vehicle.class);
								final VehicleType vehType = VehicleUtils.getDefaultVehicleType();
								final VehiclesFactory vehFactory = VehicleUtils.getFactory();
								final Vehicle vehicle = vehFactory.createVehicle(vehId, vehType);
//								qsim.createAndParkVehicleOnLink(vehicle, startingLinkId);
								final QVehicle qVeh = new QVehicleImpl( vehicle ) ;
								qsim.addParkedVehicle( qVeh, startingLinkId );
								
								// insert traveler agent:
								final MobsimAgent ag = new MyMobsimAgent(guidance, startingLinkId, vehId,
																				qsim.getSimTimer());
								qsim.insertAgentIntoMobsim(ag);
								
							}
						});
						return qsim;
					}
				});
			}
		});
		
		controler.run() ;
		
	}
	
}
