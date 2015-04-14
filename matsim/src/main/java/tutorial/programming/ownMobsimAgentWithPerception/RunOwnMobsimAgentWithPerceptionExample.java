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

package tutorial.programming.ownMobsimAgentWithPerception;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

/**
 * Untested code.  Idea is that an observer notes the traffic congestion, and returns the "best" of all outgoing links to the vehicle.
 * 
 * @author nagel
 */
public class RunOwnMobsimAgentWithPerceptionExample {

	public static void main(String[] args) {
		
		final Controler ctrl = new Controler( args[0] ) ;

		// observer.  Will probably NOT need one instance per agent in order to be thread safe since the threads will only get info from this but not set.
		// However, if one wants different perceptions per agent then one also needs different observers.  Or observers that are parameterized in the agents. 
		final MyObserver eventsObserver = new MyObserver( ctrl.getScenario() ) ;
		ctrl.getEvents().addHandler( eventsObserver );
		
		// guidance.  Will need one instance per agent in order to be thread safe
		final MyGuidance guidance = new MyGuidance( eventsObserver, ctrl.getScenario() ) ;
		
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				
				MobsimFactory factory = QSimFactory.createQSimFactory();
				// (one can look up often-used mobsim factories in the MobsimRegistrar class)

				final QSim qsim = (QSim) factory.createMobsim(sc, eventsManager) ;
				
				// Why agent source instead of inserting them directly?  Inserting agents into activities is, in fact possible just
				// after the QSim constructor.  However, inserting vehicles or agents into links is not.  Agentsource makes
				// sure that this is appropriately delayed.
				qsim.addAgentSource(new AgentSource(){
					@Override
					public void insertAgentsIntoMobsim() {
						// insert traveler agent:
						final MobsimAgent ag = new MyMobsimAgent( guidance ) ;
						qsim.insertAgentIntoMobsim(ag) ;
						
						// insert vehicle:
						final Id<Vehicle> vehId = Id.create(ag.getId(), Vehicle.class);
						final VehicleType vehType = VehicleUtils.getDefaultVehicleType();
						final VehiclesFactory vehFactory = VehicleUtils.getFactory();
						final Vehicle vehicle = vehFactory.createVehicle(vehId, vehType );
						Id<Link> linkId4VehicleInsertion = null ; // replace by something meaningful
						qsim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
					}
				}) ;
				return qsim ;
			}
		}) ;
	}

}
