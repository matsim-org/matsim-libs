/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.api.experimental.events.EventsManager;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.routes.NetworkRoute;
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
 */
public class FISS implements DepartureHandler, MobsimEngine {

	private static final Logger LOG = LogManager.getLogger(FISS.class);

	private final QNetsimEngineI qNetsimEngine;
    private final DepartureHandler delegate;
	private final FISSConfigGroup fissConfigGroup;
	private final TeleportationEngine teleport;
	private final Network network;
	private final TravelTime travelTime;
	private final Random random;

	private final MatsimServices matsimServices;


	FISS(MatsimServices matsimServices, QNetsimEngineI qNetsimEngine, Scenario scenario, EventsManager eventsManager, FISSConfigGroup fissConfigGroup,
			TravelTime travelTime) {
		this.qNetsimEngine = qNetsimEngine;
        this.delegate = qNetsimEngine.getDepartureHandler();
		this.fissConfigGroup = fissConfigGroup;
		this.teleport = new DefaultTeleportationEngine(scenario, eventsManager);
		this.travelTime = travelTime;
		this.network = scenario.getNetwork();
		this.random = MatsimRandom.getLocalInstance();
		this.matsimServices = matsimServices;
	}

    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (this.fissConfigGroup.sampledModes.contains(agent.getMode())) {
			if (random.nextDouble() < fissConfigGroup.sampleFactor || agent instanceof TransitDriverAgent || this.switchOffFISS()) {
				return delegate.handleDeparture(now, agent, linkId);
			} else {
				if (!(agent instanceof DynAgent)) {
					if (agent instanceof PlanAgent planAgent) {
						Leg currentLeg = (Leg) planAgent.getCurrentPlanElement();
						Gbl.assertIf(this.fissConfigGroup.sampledModes.contains(currentLeg.getMode()));
						NetworkRoute networkRoute = (NetworkRoute) currentLeg.getRoute();
						Person person = planAgent.getCurrentPlan().getPerson();
						Vehicle vehicle = this.matsimServices.getScenario().getVehicles().getVehicles()
								.get(networkRoute.getVehicleId());

						// update travel time with travel times of last iteration
						double newTravelTime = 0.0;
						// start and end link are not considered in NetworkRoutingModule for travel time
						for (Id<Link> routeLinkId : networkRoute.getLinkIds()) {
							newTravelTime += this.travelTime.getLinkTravelTime(network.getLinks().get(routeLinkId),
									now + newTravelTime, person, vehicle);
						}
						LOG.debug("New travelTime: {}, was {}", newTravelTime,
								networkRoute.getTravelTime().orElseGet(() -> Double.NaN));
						networkRoute.setTravelTime(newTravelTime);
					}
					// remove vehicle of teleported agent from parking spot
					QVehicle removedVehicle = null;
					if (agent instanceof MobsimDriverAgent driverAgent) {
						Id<Vehicle> vehicleId = driverAgent.getPlannedVehicleId();
						QVehicle vehicle = qNetsimEngine.getVehicles().get(vehicleId);
						QLinkI qLinkI = (QLinkI) this.qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
						removedVehicle = qLinkI.removeParkedVehicle(vehicleId);
						if (removedVehicle == null) {
							throw new RuntimeException(
									"Could not remove parked vehicle with id " + vehicleId + " on the link id "
									// + linkId
											+ vehicle.getCurrentLink().getId()
											+ ".  Maybe it is currently used by someone else?"
											+ " (In which case ignoring this exception would lead to duplication of this vehicle.) "
											+ "Maybe was never placed onto a link?");
						}
					}
					boolean result = teleport.handleDeparture(now, agent, linkId);
					// teleport vehicle right after agent
					if (removedVehicle != null) {
						Id<Link> destinationLinkId = agent.getDestinationLinkId();
						QLinkI qLinkDest = (QLinkI) this.qNetsimEngine.getNetsimNetwork()
								.getNetsimLink(destinationLinkId);
						qLinkDest.addParkedVehicle(removedVehicle);
					}
					return result;
				}
			}
		}
		return false;
	}

	@Override
	public void doSimStep(double time) {
		teleport.doSimStep(time);
	}

	@Override
	public void onPrepareSim() {
		if (switchOffFISS()) {
			deflateVehicleTypes(matsimServices.getScenario(), this.fissConfigGroup);
		}
		teleport.onPrepareSim();
	}

	private void deflateVehicleTypes(Scenario scenario, FISSConfigGroup fissConfigGroup) {
		for (String sampledQsimModes : fissConfigGroup.sampledModes) {
			VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(Id.create(sampledQsimModes,VehicleType.class));
			vehicleType.setPcuEquivalents(vehicleType.getPcuEquivalents() * fissConfigGroup.sampleFactor);
		}
	}

	@Override
	public void afterSim() {
		teleport.afterSim();
	}

	private boolean switchOffFISS() {
		return (this.fissConfigGroup.switchOffFISSLastIteration && this.matsimServices.getConfig().controller().getLastIteration() == this.matsimServices.getIterationNumber());
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		teleport.setInternalInterface(internalInterface);
	}
}
