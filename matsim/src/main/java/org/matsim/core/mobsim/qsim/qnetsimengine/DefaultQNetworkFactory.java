/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim.qnetsimengine;


import jakarta.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingSearchTimeCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import java.util.Collections;
import java.util.Set;


/**
 * The idea here is that there are the following levels:<ul>
 * <li> Run-specific objects, such as {@link QSimConfigGroup}, {@link EventsManager}, etc.  These are in general guice-injected, but can
 * also be configured by the constructor.  In the longer run, I would like to get rid of {@link Scenario}, but I haven't checked where
 * it us truly needed yet.
 * <li> Mobsim-specific objects, such as {@link AgentCounter} or {@link QNetsimEngineWithThreadpool}.  Since the mobsim is re-created in every
 * iteration, they cannot be injected via guice, at least not via the global inject mechanism that has the override facility.
 * <li> The last level are link- oder node-specific objects such as {@link Link}  or {@QNode}.  They are arguments to the
 * creational methods.
 * <li> The main underlying implementations are {@link QueueWithBuffer}, the factory of which is inserted into {@link QLinkImpl}.  This
 * was the syntax that could subsume all other syntactic variants.  Builders are used to set defaults and to avoid overly long
 * constructors.
 * <li> {@link QLinkImpl} is essentially the container where vehicles are parked, agents perform activities, etc.  MATSim has some tendency to
 * have them centralized in the QSim (see, e.g., {@link TransitStopAgentTracker}), but both for parallel computing and for visualization,
 * having agents on a decentralized location is helpful.
 * <li> Most functionality of {@link QLinkImpl} is actually in {@link AbstractQLink}, which
 * can also be used as basic infrastructure by other qnetworks.
 * <li> {@link QueueWithBuffer} is an instance of {@link QLaneI} and can be replaced accordingly.
 * <li> One can also replace the {@link VehicleQ} that works inside {@link QueueWithBuffer}.
 * </ul>
 *
 * @author dgrether, knagel
 * @see ConfigurableQNetworkFactory
 */
public final class DefaultQNetworkFactory implements QNetworkFactory {
	private final EventsManager events;
	private final Scenario scenario;
	@Inject
	private Set<LinkSpeedCalculator> calculators = Collections.emptySet();

	@Inject
	private Set<VehicleHandler> vehicleHandlers = Collections.emptySet();

	@Inject
	private Set<ParkingSearchTimeCalculator> parkingSearchTimeCalculators = Collections.emptySet();

	private NetsimEngineContext context;
	private NetsimInternalInterface netsimEngine1;

	@Inject
	DefaultQNetworkFactory(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;
	}

	@Override
	public void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
		double effectiveCellSize = scenario.getNetwork().getEffectiveCellSize();

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis(scenario.getConfig().qsim().getLinkWidthForVis());
		linkWidthCalculator.setLaneWidth(scenario.getNetwork().getEffectiveLaneWidth());

		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngineWithThreadpool.createAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);

		// (vis needs network and may need population attributes and config; in consequence, makes sense to have scenario here. kai, apr'16)
		context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, scenario.getConfig().qsim(),
			mobsimTimer, linkWidthCalculator);

		Gbl.assertNotNull(context);

		this.netsimEngine1 = netsimEngine1;
	}

	@Override
	public QLinkI createNetsimLink(final Link link, final QNodeI toQueueNode) {
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine1);
		// it is not possible to construct the builder in the initializeFactory method.  I do not know why.  kai, jun'23

		DefaultLinkSpeedCalculator theCalculator = new DefaultLinkSpeedCalculator();
		for (LinkSpeedCalculator calculator : calculators) {
			theCalculator.addLinkSpeedCalculator(calculator);
		}
		linkBuilder.setLinkSpeedCalculator(theCalculator);


		DefaultVehicleHandler vehicleHandler = new DefaultVehicleHandler();
		for (VehicleHandler handler : vehicleHandlers) {
			vehicleHandler.addVehicleHandler(handler);
		}
		linkBuilder.setVehicleHandler(vehicleHandler);

		DefaultParkingSearchTime parkingSearchTime = new DefaultParkingSearchTime();
		for (ParkingSearchTimeCalculator calculator : parkingSearchTimeCalculators) {
			parkingSearchTime.addHandler(calculator);
		}
		linkBuilder.setParkingSearchTimeCalculator(parkingSearchTime);

		return linkBuilder.build(link, toQueueNode);
	}

	@Override
	public QNodeI createNetsimNode(final Node node) {
		QNodeImpl.Builder nodeBuilder = new QNodeImpl.Builder(netsimEngine1, context, scenario.getConfig().qsim());
		return nodeBuilder.build(node);
	}
}
