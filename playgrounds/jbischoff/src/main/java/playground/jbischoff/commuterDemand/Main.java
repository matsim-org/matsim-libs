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

package playground.jbischoff.commuterDemand;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFClientLive;

public class Main {

	public static void main(String[] args) {
		
		// I want a config out of nothing:
		Config config = ConfigUtils.createConfig() ;
		
		// set some config stuff:
		config.network().setInputFile("C:/local_jb/workspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml") ; 
		config.controler().setLastIteration(0) ;
		config.qsim().setEndTime(26.*3600) ;
		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.queue ) ;
		
		// base the controler on that:
		final Controler ctrl = new Controler( config ) ;
		ctrl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		ctrl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new MobsimFactory() {
							@Override
							public Mobsim createMobsim(final Scenario sc, final EventsManager ev) {

								// take the default mobsim factory, but since the population is empty, it will not be filled with demand:
								final QSim qsim = (QSim) QSimUtils.createDefaultQSim(sc, ev);

								// add my own agent(s):
								qsim.addAgentSource(new AgentSource() {
									VehicleType basicVehicleType = new VehicleTypeImpl(Id.create("basicVehicleType", VehicleType.class));

									@Override
									public void insertAgentsIntoMobsim() {
										final Id<Link> startLinkId = (Id<Link>) (sc.getNetwork().getLinks().keySet().toArray())[0];
										final MobsimVehicle veh = new QVehicle(new VehicleImpl(Id.create("testVehicle", Vehicle.class), basicVehicleType));
										qsim.addParkedVehicle(veh, startLinkId);
										qsim.insertAgentIntoMobsim(new MyAgent(sc, ev, qsim, startLinkId, veh));
										// (the Id of the parked vehicle needs to be known to the agent, otherwise it will not work!)
									}
								});

								// add otfvis live.  Can't do this in core since otfvis is not accessible from there.
								OTFClientLive.run(sc.getConfig(), OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, ev, qsim));

								// return the whole thing:
								return qsim;
							}
						}.createMobsim(ctrl.getScenario(), ctrl.getEvents());
					}
				});
			}
		});

		ctrl.run();
		
	}

}

class MyAgent implements MobsimDriverAgent {
	private static Logger log = Logger.getLogger("MyAgent") ;

	private MobsimVehicle vehicle;
	private Scenario sc;
	private EventsManager ev;
	private Id<Link> currentLinkId;
	private Id<Person> myId;
	private State state = State.ACTIVITY;
	private Netsim netsim;
	private Id<Link> destinationLinkId = Id.create("dummy", Link.class);
	private Id<Vehicle> plannedVehicleId ;

	private double activityEndTime = 1. ;
	
	MyAgent( Scenario sc, EventsManager ev, Netsim netsim, Id<Link> startLinkId, MobsimVehicle veh ) {
		log.info( "calling MyAgent" ) ;
		this.sc = sc ;
		this.ev = ev ;
		this.myId = Id.create("testveh", Person.class) ;
		this.netsim = netsim ;
		this.currentLinkId = startLinkId ;
		this.plannedVehicleId = veh.getId() ;
	}

	@Override
	public void setStateToAbort(double now) {
		this.state = State.ABORT ;
		log.info( "calling abort; setting state to: " + this.state ) ;
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
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
	public String getMode() {
		return TransportMode.car ; // either car or nothing
	}

	@Override
	public State getState() {
		log.info( "calling getState; answer: " + this.state ) ;
		return this.state ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		this.currentLinkId = linkId ;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId ;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId ;
	}

	@Override
	public Id<Person> getId() {
		return this.myId ;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		Link currentLink = sc.getNetwork().getLinks().get(this.currentLinkId) ;
		Object[] outLinks = currentLink.getToNode().getOutLinks().keySet().toArray() ;
		int idx = MatsimRandom.getRandom().nextInt(outLinks.length) ;
		if ( this.netsim.getSimTimer().getTimeOfDay() < 24.*3600 ) {
			return (Id<Link>) outLinks[idx] ;
		} else {
			this.destinationLinkId  = (Id<Link>) outLinks[idx] ;
			return null ;
		}
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return this.plannedVehicleId ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// TODO Auto-generated method stub
//		throw new RuntimeException("not implemented") ;
	    return false;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	
}
