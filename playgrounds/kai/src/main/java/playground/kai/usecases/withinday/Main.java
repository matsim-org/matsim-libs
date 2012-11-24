/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.kai.usecases.withinday;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.vis.otfvis.OTFClientLive;

class Main {

	public static void main(String[] args) {
		
		// I want a config out of nothing:
		Config config = ConfigUtils.createConfig() ;
		
		// set some config stuff:
		config.addQSimConfigGroup(new QSimConfigGroup() ) ;
		config.network().setInputFile("../../../matsim/trunk/examples/siouxfalls/network-wo-dummy-node.xml") ; 
		config.controler().setLastIteration(0) ;
		config.getQSimConfigGroup().setEndTime(26.*3600) ;
		config.getQSimConfigGroup().setSnapshotStyle( QSimConfigGroup.SNAPSHOT_AS_QUEUE ) ;
		
		// base the controler on that:
		Controler ctrl = new Controler( config ) ;
		ctrl.setOverwriteFiles(true) ;
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(final Scenario sc, final EventsManager ev) {

				// take the default mobsim factory, but since the population is empty, it will not be filled with demand:
				final QSim qsim = (QSim) (new QSimFactory()).createMobsim(sc,ev) ;
				
				// add my own agent(s):
				qsim.addAgentSource(new AgentSource() {
					@Override
					public void insertAgentsIntoMobsim() {
						Id startLinkId = (Id) (sc.getNetwork().getLinks().keySet().toArray())[0] ;
						MobsimVehicle veh = QSimUtils.createDefaultVehicle(new IdImpl("testVehicle")) ;
						qsim.addParkedVehicle(veh, startLinkId) ;
						qsim.insertAgentIntoMobsim(new MyAgent(sc,ev,qsim,startLinkId,veh) ) ;
						// (the Id of the parked vehicle needs to be known to the agent, otherwise it will not work!)
					}
				} ) ;
				
				// add otfvis live.  Can't do this in core since otfvis is not accessible from there.
				OTFClientLive.run(sc.getConfig(), OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, ev, qsim));
				
				// return the whole thing:
				return qsim ;
			}
		}) ;
		
		ctrl.run();
		
	}

}

class MyAgent implements MobsimDriverAgent {
	private static Logger log = Logger.getLogger("MyAgent") ;

	private MobsimVehicle vehicle;
	private Scenario sc;
	private EventsManager ev;
	private Id currentLinkId;
	private Id myId;
	private State state = State.ACTIVITY;
	private Netsim netsim;
	private Id destinationLinkId = new IdImpl("dummy");
	private Id plannedVehicleId ;

	private double activityEndTime = 1. ;
	
	MyAgent( Scenario sc, EventsManager ev, Netsim netsim, Id startLinkId, MobsimVehicle veh ) {
		log.info( "calling MyAgent" ) ;
		this.sc = sc ;
		this.ev = ev ;
		this.myId = new IdImpl("testveh") ;
		this.netsim = netsim ;
		this.currentLinkId = startLinkId ;
		this.plannedVehicleId = veh.getId() ;
	}

	@Override
	public void abort(double now) {
		// I don't think this needs to do anything.
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		this.state = State.LEG ; // want to move
		log.info( "calling endActivityAndComputeNextState; setting state to: " + this.state ) ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.state = State.ACTIVITY ;
		this.activityEndTime = Double.POSITIVE_INFINITY ;
		log.info( "calling endLegAndComputeNextState; setting state to: " + this.state ) ;
	}

	@Override
	public double getActivityEndTime() {
		log.info ("calling getActivityEndTime; answer: " + this.activityEndTime ) ;
		return this.activityEndTime ;
	}

	@Override
	public Double getExpectedTravelTime() {
		return 0. ;  // what does this matter for?
	}

	@Override
	public String getMode() {
		return TransportMode.car ; // either car of nothing
	}

	@Override
	public State getState() {
		log.info( "calling getState; answer: " + this.state ) ;
		return this.state ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		this.currentLinkId = linkId ;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.currentLinkId ;
	}

	@Override
	public Id getDestinationLinkId() {
		return this.destinationLinkId ;
	}

	@Override
	public Id getId() {
		return this.myId ;
	}

	@Override
	public Id chooseNextLinkId() {
		Link currentLink = sc.getNetwork().getLinks().get(this.currentLinkId) ;
		Object[] outLinks = currentLink.getToNode().getOutLinks().keySet().toArray() ;
		int idx = MatsimRandom.getRandom().nextInt(outLinks.length) ;
		if ( this.netsim.getSimTimer().getTimeOfDay() < 24.*3600 ) {
			return (Id) outLinks[idx] ;
		} else {
			this.destinationLinkId  = (Id) outLinks[idx] ;
			return null ;
		}
	}

	@Override
	public Id getPlannedVehicleId() {
		return this.plannedVehicleId ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}
	
}
