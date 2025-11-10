/**
 * org.matsim.contrib.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.matsim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;

/**
 * 
 * @author GunnarF
 *
 */
abstract class AbstractPopulationDistance {

	static AbstractPopulationDistance newPopulationDistance(final Plans pop1, final Plans pop2, final Scenario scenario,
			final Map<String, ? extends TravelTime> mode2travelTime) {
//		final ATAPConfigGroup greedoConfig = ConfigUtils.addOrGetModule(scenario.getConfig(),
//				ATAPConfigGroup.class);
//		if (ATAPConfigGroup.PopulationDistanceType.Hamming.equals(greedoConfig.getPopulationDistance())) {
//			return new HammingPopulationDistance();
//		} else if (ATAPConfigGroup.PopulationDistanceType.Kernel.equals(greedoConfig.getPopulationDistance())) {
		return new KernelPopulationDistance(pop1, pop2, scenario, mode2travelTime);
//		} else {
//			throw new RuntimeException("Unknown distance type: " + greedoConfig.getPopulationDistance());
//		}
	}

	ConcurrentHashMap<Id<Person>, ConcurrentHashMap<Id<Person>, Double>> getPersonId2personId2aCoeff() {
		throw new UnsupportedOperationException();
	}

	abstract double getACoefficient(Id<Person> personId1, Id<Person> personId2);

}