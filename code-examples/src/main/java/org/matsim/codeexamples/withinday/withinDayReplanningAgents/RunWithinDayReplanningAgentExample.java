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

package org.matsim.codeexamples.withinday.withinDayReplanningAgents;

import com.google.inject.Provider;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;

import javax.inject.Inject;
import java.net.URL;

public class RunWithinDayReplanningAgentExample {

	public static void main(String[] args) {

		// I want a config out of nothing:
		Config config = ConfigUtils.createConfig() ;

		// set some config stuff:
		URL context = ExamplesUtils.getTestScenarioURL("siouxfalls");
		URL networkUrl = IOUtils.extendUrl(context, "network-wo-dummy-node.xml");
//		config.network().setInputFile("scenarios/siouxfalls/network-wo-dummy-node.xml") ;
		config.network().setInputFile(networkUrl.toString());
		config.controler().setLastIteration(0) ;
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(26.*3600) ;
		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.queue ) ;
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		// base the controler on that:
		Controler ctrl = new Controler( config ) ;
		ctrl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<>() {

					@Inject
					Scenario sc;
					@Inject
					EventsManager ev;

					@Override
					public Mobsim get() {
						// take the default mobsim, but since the population is empty, it will not be filled with demand:
						final QSim qsim = new QSimBuilder(getConfig()).useDefaults().build(sc, ev);

						// add my own agent(s):
						qsim.addAgentSource(new AgentSource() {
							VehicleType basicVehicleType;

							@Override
							public void insertAgentsIntoMobsim() {
								if (basicVehicleType == null) {
									basicVehicleType = sc.getVehicles().getFactory().createVehicleType(Id.create("basicVehicleType", VehicleType.class));
								}
								final Id<Link> startLinkId = sc.getNetwork().getLinks().keySet().iterator().next();
								final MobsimVehicle veh = new QVehicleImpl(VehicleUtils.createVehicle(Id.create("testVehicle", Vehicle.class), basicVehicleType));
//								final MobsimVehicle veh = new QVehicle(new VehicleImpl(Id.create("testVehicle", Vehicle.class ), basicVehicleType));
								qsim.addParkedVehicle(veh, startLinkId);
								qsim.insertAgentIntoMobsim(new MyAgent(sc, qsim, startLinkId, veh));
								// (the Id of the parked vehicle needs to be known to the agent, otherwise it will not work!)
							}
						});

						// add otfvis live.  Can't do this in core since otfvis is not accessible from there.
						OTFClientLive.run(sc.getConfig(), OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, ev, qsim));

						return qsim;
					}
				});
			}
		});
		ctrl.run();

	}

}

/**
 * See {@link org.matsim.codeexamples.mobsim.ownMobsimAgentWithPerception.RunOwnMobsimAgentWithPerceptionExample}
 * and {@link org.matsim.codeexamples.mobsim.ownMobsimAgentUsingRouter.RunOwnMobsimAgentUsingRouterExample} for
 * more complete examples.
 *
 * @author nagel
 *
 */
class MyAgent implements MobsimDriverAgent {
	private static final Logger log = LogManager.getLogger("MyAgent") ;

	private MobsimVehicle vehicle;
	private final Scenario sc;
	private Id<Link> currentLinkId;
	private final Id<Person> myId;
	private State state = State.ACTIVITY;
	private final Netsim netsim;
	private Id<Link> destinationLinkId = Id.create("dummy", Link.class);
	private final Id<Vehicle> plannedVehicleId ;

	private double activityEndTime = 1. ;

	MyAgent(Scenario sc, Netsim netsim, Id<Link> startLinkId, MobsimVehicle veh) {
		log.info( "calling MyAgent" ) ;
		this.sc = sc ;
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
	public OptionalTime getExpectedTravelTime() {
		return OptionalTime.defined( 0. );  // what does this matter for?
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
		return false ;
	}

	@Override
	public Facility getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
