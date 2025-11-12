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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet;
import se.vti.emulation.EmulationParameters;
import se.vti.emulation.handlers.EmulationHandler;

/**
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 */
public class PlanEmulator {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final Set<String> passengerModes;

	private final Map<String, ActivityEmulator> actType2emulator;

	private final Map<String, LegEmulator> mode2emulator;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public PlanEmulator(final MatsimServices services, final Map<String, ActivityEmulator> actType2emulator,
			final Map<String, LegEmulator> mode2emulator) {
		this.services = services;
		this.actType2emulator = actType2emulator;
		this.mode2emulator = mode2emulator;

		// Transit-specific parameters and checks below.
		this.passengerModes = new LinkedHashSet<>();
		if (services.getConfig().transit().isUseTransit()) {
			// Emulation is only compatible with checkLineAndStop. This could be improved.
			if (!TransitConfigGroup.BoardingAcceptance.checkLineAndStop
					.equals(services.getConfig().transit().getBoardingAcceptance())) {
				throw new RuntimeException("Only supporting " + TransitConfigGroup.BoardingAcceptance.checkLineAndStop);
			}
			// Extract passenger modes from SwissRailRaptor.
			// raptorConfig.getModeMappingForPassengers() returns an empty
			// collection if mode mappings are not used.
			final SwissRailRaptorConfigGroup raptorConfig = (SwissRailRaptorConfigGroup) this.services.getConfig()
					.getModules().get(SwissRailRaptorConfigGroup.GROUP);
			if (raptorConfig == null) {
				throw new RuntimeException("Only supporting SwissRailRaptor.");
			}
			for (ModeMappingForPassengersParameterSet modeMappingParams : raptorConfig.getModeMappingForPassengers()) {
				this.passengerModes.add(modeMappingParams.getPassengerMode());
			}
		}
	}

	// --------------- IMPLEMENTATION OF SimulationEmulator ---------------

	public void emulate(final Person person, final Plan plan, 
			// final TravelTime overridingCarTravelTime, 
			final Map<String, ? extends TravelTime> mode2travelTime,
			EventHandler eventsHandler,
			Provider<Set<EmulationHandler>> emulationHandlerProvider, final int iteration, EventsManager eventsManager,
			EventsToScore events2score, final boolean overwritePlanTimes) {

		final Set<EmulationHandler> emulationHandlers;
		synchronized (emulationHandlerProvider) {
			emulationHandlers = emulationHandlerProvider.get();
		}
		for (EmulationHandler handler : emulationHandlers) {
			// leaves it to the handler to (i) register itself with the manager, and (ii)
			// possibly memorize the manager
			handler.configure(eventsManager);
		}
		
		final double maxEndTime_s = (this.services.getConfig().qsim().getEndTime().isDefined()
				? this.services.getConfig().qsim().getEndTime().seconds()
				: Double.POSITIVE_INFINITY);

		double time_s = 0.0;
		boolean stuck = false;

		final List<PlanElement> planElements = plan.getPlanElements();
		for (int planElementIndex = 0; (planElementIndex < planElements.size()) && !stuck; planElementIndex++) {
			final PlanElement element = planElements.get(planElementIndex);

			final boolean isFirstElement = (planElementIndex == 0);
			final boolean isLastElement = (planElementIndex == (planElements.size() - 1));

			if (element instanceof Activity) {
				
				final Activity activity = (Activity) element;

				final ActivityEmulator activityEmulator = this.actType2emulator.getOrDefault(activity.getType(),
						this.actType2emulator.get(EmulationParameters.DEFAULT));
				activityEmulator.configure(eventsManager, maxEndTime_s, overwritePlanTimes);

				final Double activityEndTime_s = activityEmulator.emulateActivityAndReturnEndTime_s(activity, person,
						time_s, isFirstElement, isLastElement);
				if (!isLastElement) {
					time_s = activityEndTime_s;
				}

			} else if (element instanceof Leg) {

				final Leg leg = (Leg) element;

				final LegEmulator legEmulator = this.mode2emulator.getOrDefault(leg.getMode(),
						this.mode2emulator.get(EmulationParameters.DEFAULT));
				legEmulator.configure(eventsManager,						
						mode2travelTime.get(leg.getMode()),						
						maxEndTime_s, overwritePlanTimes);
				
				time_s = legEmulator.emulateLegAndReturnEndTime_s(planElementIndex, planElements, person, time_s);
			} else {
				throw new RuntimeException("Unknown instance of " + PlanElement.class.getSimpleName() + ": "
						+ element.getClass().getSimpleName());
			}

			if (time_s > maxEndTime_s) {
				stuck = true; // Exits the loop, i.e. terminates the emulation of this person.
				eventsManager.processEvent(new PersonStuckEvent(time_s, person.getId(), null, null));
			}
		}
	}
}
