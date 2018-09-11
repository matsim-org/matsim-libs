/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of when every single private vehicle enters which link.
 * 
 * @author Gunnar Flötteröd
 *
 */
class PrivateTrafficLinkUsageListener implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Population population;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> driverId2indicators;

	private final Map<Id<Vehicle>, Id<Person>> privateVehicleId2DriverId = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	PrivateTrafficLinkUsageListener(final TimeDiscretization timeDiscretization, final Population population,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> driverId2indicators) {
		this.timeDiscretization = timeDiscretization;
		this.population = population;
		this.driverId2indicators = driverId2indicators;
	}

	// -------------------- RESULT ACCESS --------------------

	Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getIndicatorView() {
		return Collections.unmodifiableMap(this.driverId2indicators);
	}

	// -------------------- INTERNALS --------------------

	private void registerLinkEntry(final Id<Link> linkId, final Id<Vehicle> vehicleId, final double time_s) {
		final Id<Person> driverId = this.privateVehicleId2DriverId.get(vehicleId);
		if ((driverId != null) && (time_s >= this.timeDiscretization.getStartTime_s())
				&& (time_s < this.timeDiscretization.getEndTime_s())) {
			SpaceTimeIndicators<Id<?>> indicators = this.driverId2indicators.get(driverId);
			if (indicators == null) {
				indicators = new SpaceTimeIndicators<Id<?>>(this.timeDiscretization.getBinCnt());
				this.driverId2indicators.put(driverId, indicators);
			}
			indicators.visit(linkId, this.timeDiscretization.getBin(time_s));
		}
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		this.driverId2indicators.clear();
		this.privateVehicleId2DriverId.clear();
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		final Id<Person> driverId = event.getPersonId();
		if ((driverId != null) && this.population.getPersons().containsKey(driverId)) {
			this.privateVehicleId2DriverId.put(event.getVehicleId(), driverId);
			this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("Started ...");

		final Config config = ConfigUtils.loadConfig("./testdata/berlin_2014-08-01_car_1pct/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final PrivateTrafficLinkUsageListener loa = new PrivateTrafficLinkUsageListener(timeDiscr,
				scenario.getPopulation(), new LinkedHashMap<Id<Person>, SpaceTimeIndicators<Id<?>>>());
		controler.getEvents().addHandler(loa);

		controler.run();

		System.out.println("... done.");
	}

}
