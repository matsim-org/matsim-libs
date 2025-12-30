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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OnlyStartEndActivityEmulator extends AbstractPlanElementEmulator implements ActivityEmulator {

	@Inject
	public OnlyStartEndActivityEmulator(final Scenario scenario) {
		super(scenario);
	}

	@Override
	public void configure(final EventsManager eventsManager, final double simEndTime_s, final boolean overwritePlanTimes) {
		this.configureInternally(eventsManager, simEndTime_s, overwritePlanTimes);
	}

	@Override
	public Double emulateActivityAndReturnEndTime_s(final Activity activity, final Person person, double time_s,
			final boolean isFirstElement, final boolean isLastElement) {
		if (!isFirstElement) {
			this.eventsManager.processEvent(new ActivityStartEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType(), activity.getCoord()));
			if (this.overwritePlanTimes) {
				// 2025-05-22 Added try/catch to deal with removed setter in InteractionActivity. Gunnar
				try {
					activity.setStartTime(time_s);
				} catch (UnsupportedOperationException e) {
					// happens in InteractionActivity TODO likely a slow solution
				}
			}
		}
		if (!isLastElement) {
			time_s = Math.max(time_s, this.timeInterpretation.decideOnActivityEndTime(activity, time_s).seconds());
			this.eventsManager.processEvent(new ActivityEndEvent(time_s, person.getId(), activity.getLinkId(),
					activity.getFacilityId(), activity.getType(), activity.getCoord()));
			if (this.overwritePlanTimes) {
				// 2025-05-22 Added try/catch to deal with removed setter in InteractionActivity. Gunnar
				try {
				activity.setEndTime(time_s);
				} catch (UnsupportedOperationException e) {
					// happens in InteractionActivity TODO likely a slow solution
				}
			}
			return time_s;
		} else {
			return null;
		}
	}
}
