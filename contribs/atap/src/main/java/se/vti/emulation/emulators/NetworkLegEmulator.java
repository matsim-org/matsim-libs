/**
 * se.vti.emulation
 * 
 * Copyright (C) 2023, 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
 * Partially based on Sebastian Hörl's IER.
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.emulation.emulators;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * TODO: Include toll.
 *
 * @author Gunnar Flötteröd
 *
 */
public class NetworkLegEmulator extends OnlyDepartureArrivalLegEmulator {

	private TravelTime travelTime;

	@Inject
	public NetworkLegEmulator(final Scenario scenario) {
		super(scenario);
	}

	@Override
	public void configure(final EventsManager eventsManager, final TravelTime travelTime, final double simEndTime_s,
			final boolean overwritePlanTimes) {
		super.configure(eventsManager, travelTime, simEndTime_s, overwritePlanTimes);
		this.travelTime = travelTime;
	}

	@Override
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(final Leg leg, final Person person,
			double time_s) {

		final NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
		if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())) {

			final Id<Vehicle> vehicleId = Id.createVehicleId(person.getId());
			this.eventsManager.processEvent(new PersonEntersVehicleEvent(time_s, person.getId(), vehicleId));

			// First link of a network route. Vehicle enters downstream (i.e. does not need
			// to traverse) and joints the queue.
			// Assumes that waiting vehicles are inserted BEFORE driving vehicles (qSim config)
			Link link = this.scenario.getNetwork().getLinks().get(networkRoute.getStartLinkId());
			final double delay_s = Math.max(0.0, this.travelTime.getLinkTravelTime(link, time_s, person, null)
					- link.getLength() / link.getFreespeed(time_s));
			time_s += 0.5 * delay_s;
			this.eventsManager.processEvent(
					new VehicleEntersTrafficEvent(time_s, person.getId(), link.getId(), vehicleId, leg.getMode(), 1.0));
			time_s += 0.5 * delay_s;
			this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));
			
			// Intermediate links of a network route.
			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				link = this.scenario.getNetwork().getLinks().get(linkId);
				this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, link.getId()));
				final double travelTime_s = this.travelTime.getLinkTravelTime(link, time_s, person, null);
				time_s += travelTime_s;
				this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));
			}

			// Last link of a network route. Vehicle departs downstream (i.e. does need to traverse) but does not join the queue.
			this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, networkRoute.getEndLinkId()));
			time_s += link.getLength() / link.getFreespeed(time_s);
			this.eventsManager.processEvent(new VehicleLeavesTrafficEvent(time_s, person.getId(),
					networkRoute.getEndLinkId(), vehicleId, leg.getMode(), 1.0));

			this.eventsManager.processEvent(new PersonLeavesVehicleEvent(time_s, person.getId(), vehicleId));
		}

		return time_s;
	}
}
