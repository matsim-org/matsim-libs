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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AbstractPlanElementEmulator {

	protected final Scenario scenario;
	protected final TimeInterpretation timeInterpretation;

	protected EventsManager eventsManager;
	protected double maxEndTime_s;
	protected boolean overwritePlanTimes;

	@Inject
	public AbstractPlanElementEmulator(final Scenario scenario) {
		this.scenario = scenario;
		this.timeInterpretation = TimeInterpretation.create(scenario.getConfig());
	}

	protected void configureInternally(EventsManager eventsManager, double maxEndTime_s, boolean overwritePlanTimes) {
		this.eventsManager = eventsManager;
		this.maxEndTime_s = maxEndTime_s;
		this.overwritePlanTimes = overwritePlanTimes;
	}

}
