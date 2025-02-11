 /*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.Random;

/**
 * FISS Flow-inflated selective sampling (working title).
 *
 * The aim is to only assign a specified fraction of vehicular agents and teleport the rest.
 * This is achieved by making use of a VehicularDepartureHandler (so far not enforced, as the
 * implementation is not exposed, so in theory could be any DepartureHandler) and a TeleportationEngine.
 * Both used as delegates.
 *
 * Transit driver agents are always assigned. In addition does not handle DynAgents (e.g., DVRP agents)
 *
 *
 * Also implements MobsimEngine to delegate required teleportation steps (such as arrivals).
 *
 *
 * @author nkuehnel / MOIA, hrewald
 *
 * <p>
 * Remarks @kainagel: <ul>
 * <li> I have renamed the VehicularDepartureHandler to {@link NetworkModeDepartureHandler}.  Also, this is now an interface, bound against a default implementation. </li>
 * <li>There are probably more changes to come, like _replacing_ the {@link NetworkModeDepartureHandler} rather than adding another {@link DepartureHandler} which effectively over-writes it. </li>
 * <li> yyyy I have changed a fair amount of things so I am not sure how much of the above text is still correct. </li>
 * </ul>
 * </p>
 */
public class FISS implements NetworkModeDepartureHandler, MobsimEngine {

	private static final Logger LOG = LogManager.getLogger(FISS.class);

	private final DepartureHandler delegate;
	private final FISSConfigGroup fissConfigGroup;
	private final TeleportationEngine teleport;
	@Inject private Network network;
	private final TravelTime travelTime;
	private final Random random;

	private final MatsimServices matsimServices;
	private final QSimConfigGroup qsimConfig;
	private final Scenario scenario;


	@Inject FISS( MatsimServices matsimServices, QNetsimEngineI qNetsimEngine, Scenario scenario, EventsManager eventsManager, FISSConfigGroup fissConfigGroup,
		      @Named(TransportMode.car) TravelTime travelTime, NetworkModeDepartureHandlerDefaultImpl networkModeDepartureHandler ) {
		this.delegate = networkModeDepartureHandler;
		this.fissConfigGroup = fissConfigGroup;

		this.teleport = new DefaultTeleportationEngine(scenario, eventsManager);
		// yyyy should be replaced by using the binding.  kai, feb'25

		this.travelTime = travelTime;
		this.random = MatsimRandom.getLocalInstance();
		this.matsimServices = matsimServices;
		this.qsimConfig = scenario.getConfig().qsim();
		this.scenario = scenario;
	}

	@Override public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if ( !this.qsimConfig.getMainModes().contains( agent.getMode() ) ) {
			return false ;
		} else if ( agent instanceof DynAgent ) {
			return delegate.handleDeparture( now,agent, linkId );
		} else if ( !this.fissConfigGroup.sampledModes.contains( agent.getMode() ) ) {
			return delegate.handleDeparture( now,agent, linkId ); // not covered by test
			// (the earlier design would have such agents fall through, in which case they would be treated by the standard network mode
			// dp handler)
		} else if (random.nextDouble() < fissConfigGroup.sampleFactor || agent instanceof TransitDriverAgent || this.switchOffFISS()) {
			return delegate.handleDeparture(now, agent, linkId);
		}

		// This updates the travel time.  Teleportation departure is handled further down.
		if (agent instanceof PlanAgent planAgent) {
			Leg currentLeg = (Leg) planAgent.getCurrentPlanElement();
			NetworkRoute networkRoute = (NetworkRoute) currentLeg.getRoute();
			Person person = planAgent.getCurrentPlan().getPerson();
			Vehicle vehicle = this.scenario.getVehicles().getVehicles().get(networkRoute.getVehicleId());

			double newTravelTime = RouteUtils.calcTravelTimeExcludingStartEndLink( networkRoute, now, person, vehicle, network, travelTime );
			// yyyy This travel time _should_ include start and end link.  Also in regular teleportation!  kai, jan'25
			LOG.debug("New travelTime: {}, was {}", newTravelTime, networkRoute.getTravelTime().orElseGet(() -> Double.NaN));

			networkRoute.setTravelTime(newTravelTime);
		}

		// !!!! The following is not a teleportation-pull (as it is in NetworkModeDpHandler...), but a teleportation-push.  yyyy Are we sure
		// that these are the same?  E.g. if the vehicle needs to be made available for the next driver?  kai, feb'25

//		// remove vehicle of teleported agent from parking spot
//		QVehicle removedVehicle = null;
//		if (agent instanceof MobsimDriverAgent driverAgent) {
//			Id<Vehicle> vehicleId = driverAgent.getPlannedVehicleId();
//			QLinkI qLinkI = (QLinkI) this.qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
//			removedVehicle = qLinkI.removeParkedVehicle(vehicleId);
//			if (removedVehicle == null) {
//				throw new RuntimeException(
//						"Could not remove parked vehicle with id " + vehicleId + " on the link id "
// 								+ qNetsimEngine.getVehicles().get(vehicleId).getCurrentLink().getId()
//								+ ".  Maybe it is currently used by someone else?"
//								+ " (In which case ignoring this exception would lead to duplication of this vehicle.) "
//								+ "Maybe was never placed onto a link?");
//			}
//			// yyyy need to check how this is handled elsewhere.  I think that with some config settings we wait until the vehicle becomes available--???
//		}
		boolean result = teleport.handleDeparture(now, agent, linkId);
		Gbl.assertIf( result ); // otherwise we are now confused

		// teleport vehicle right after agent
//		if (removedVehicle != null) {
//			Id<Link> destinationLinkId = agent.getDestinationLinkId();
//			QLinkI qLinkDest = (QLinkI) this.qNetsimEngine.getNetsimNetwork().getNetsimLink(destinationLinkId);
//			qLinkDest.addParkedVehicle(removedVehicle);
//		}

		return result;

	}

	@Override public void doSimStep(double time) {
		teleport.doSimStep(time);
	}

	@Override public void onPrepareSim() {
		if (switchOffFISS()) {
			deflateVehicleTypes(matsimServices.getScenario(), this.fissConfigGroup);
			// (This is before the QVehicles are generated, so it wil be taken into account when generating them.)
		}
		teleport.onPrepareSim();
	}

	private void deflateVehicleTypes(Scenario scenario, FISSConfigGroup fissConfigGroup) {
		for (String sampledQsimModes : fissConfigGroup.sampledModes) {
			VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create(sampledQsimModes,VehicleType.class));
			vehicleType.setPcuEquivalents(vehicleType.getPcuEquivalents() * fissConfigGroup.sampleFactor);
		}
	}

	@Override public void afterSim() {
		teleport.afterSim();
	}

	private boolean switchOffFISS() {
		return (this.fissConfigGroup.switchOffFISSLastIteration && this.matsimServices.getConfig().controller().getLastIteration() == this.matsimServices.getIterationNumber());
		// yyyy note that with current implementation one canNOT change this to "switchOffFISS every x timesteps", since the pces are
		// deflated to their original values in onPrepareSim, and there is nothing in the code that multiply them to the FISS values
		// afterwards again.
	}

	@Override public void setInternalInterface(InternalInterface internalInterface) {
		teleport.setInternalInterface(internalInterface);
	}
}
