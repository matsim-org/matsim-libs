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

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OnlyDepartureArrivalLegEmulator extends AbstractPlanElementEmulator implements LegEmulator {

	@Inject
	public OnlyDepartureArrivalLegEmulator(final Scenario scenario) {
		super(scenario);
	}

	protected Leg getLeg(int legIndexInPlan, List<PlanElement> planElements) {
		return (Leg) planElements.get(legIndexInPlan);
	}

	protected Activity getPreviousActivity(int legIndexInPlan, List<PlanElement> planElements) {
		return (Activity) planElements.get(legIndexInPlan - 1);
	}

	protected Activity getFollowingActivity(int legIndexInPlan, List<PlanElement> planElements) {
		return (Activity) planElements.get(legIndexInPlan + 1);
	}

	@Override
	public void configure(final EventsManager eventsManager, final TravelTime travelTime, final double maxEndTime_s,
			final boolean overwritePlanTimes) {
		this.configureInternally(eventsManager, maxEndTime_s, overwritePlanTimes);
	}

	@Override
	public double emulateLegAndReturnEndTime_s(int legIndexInPlan, List<PlanElement> planElements, Person person,
			double time_s) {
		final Leg leg = this.getLeg(legIndexInPlan, planElements);
		final Activity previousActivity = this.getPreviousActivity(legIndexInPlan, planElements);
		final Activity nextActivity = this.getFollowingActivity(legIndexInPlan, planElements);

		// Every leg starts with a departure.
		if (time_s <= this.maxEndTime_s) {

			this.eventsManager.processEvent(new PersonDepartureEvent(time_s, person.getId(),
					PopulationUtils.decideOnLinkIdForActivity(previousActivity, this.scenario), leg.getMode(),
					TripStructureUtils.getRoutingMode(leg)));
			if (this.overwritePlanTimes) {
				leg.setDepartureTime(time_s);
			}

			time_s = this.emulateBetweenDepartureAndArrivalAndReturnEndTime_s(leg, person, time_s);

			// Every leg ends with an arrival.
			if (time_s <= this.maxEndTime_s) {
				this.eventsManager.processEvent(new PersonArrivalEvent(time_s, person.getId(),
						PopulationUtils.decideOnLinkIdForActivity(nextActivity, this.scenario), leg.getMode()));
			}
			if (this.overwritePlanTimes) {
				leg.setTravelTime(time_s - leg.getDepartureTime().seconds());
				if (leg.getRoute() != null) {
					leg.getRoute().setTravelTime(time_s - leg.getDepartureTime().seconds());
				}
			}
		}
		return time_s;
	}

	// Hook for stuff that happens between departure and arrival.
	public double emulateBetweenDepartureAndArrivalAndReturnEndTime_s(final Leg leg, final Person person,
			double time_s) {
		time_s += this.timeInterpretation.decideOnLegTravelTime(leg).seconds();
		this.eventsManager.processEvent(
				new TeleportationArrivalEvent(time_s, person.getId(), leg.getRoute().getDistance(), leg.getMode()));
		return time_s;
	}
}
