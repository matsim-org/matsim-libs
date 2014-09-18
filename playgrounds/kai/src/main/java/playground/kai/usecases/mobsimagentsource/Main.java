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

package playground.kai.usecases.mobsimagentsource;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author nagel
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controler ctrl = new Controler("abd") ;
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				final QSim qsim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;

				// not recommended (insertion too early):
				MobsimAgent ag = new MyMobsimAgent() ;
				qsim.insertAgentIntoMobsim(ag) ;

				return qsim ;
			}
		}) ;
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				final QSim qsim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
				
				// Why agent source instead of inserting them directly?  Inserting agents into activities is, in fact possible just
				// after the QSim constructor.  However, inserting vehicles or agents into links is not.  Agentsource makes
				// sure that this is appropriately delayed.
				qsim.addAgentSource(new AgentSource(){
					@Override
					public void insertAgentsIntoMobsim() {
						// insert traveler agent:
						final MobsimAgent ag = new MyMobsimAgent() ;
						qsim.insertAgentIntoMobsim(ag) ;
						
						// insert vehicle:
						final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(ag.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType() );
						Id linkId4VehicleInsertion = null ;
						qsim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
					}
				}) ;
				return qsim ;
			}
		}) ;
	}

}
	
	class MyMobsimAgent implements MobsimAgent {

		@Override
		public Id getCurrentLinkId() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Id getDestinationLinkId() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Id getId() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public State getState() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public double getActivityEndTime() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void endActivityAndComputeNextState(double now) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void endLegAndComputeNextState(double now) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void setStateToAbort(double now) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Double getExpectedTravelTime() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public String getMode() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

	}
