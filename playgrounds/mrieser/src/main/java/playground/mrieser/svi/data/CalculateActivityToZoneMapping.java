/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data;

import java.util.ArrayList;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class CalculateActivityToZoneMapping {

	private final ActivityToZoneMapping mapping;
	private final Zones zones;
	
	public CalculateActivityToZoneMapping(final ActivityToZoneMapping mapping, final Zones zones) {
		this.mapping = mapping;
		this.zones = zones;
	}
	
	public void run(final Population population) {
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			ArrayList<String> zoneIds = new ArrayList<String>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					Feature zone = this.zones.getContainingZone(act.getCoord().getX(), act.getCoord().getY());
					zoneIds.add(zone.getAttribute("ZONE_NEU").toString());
				}
			}
			String[] tmp = zoneIds.toArray(new String[zoneIds.size()]);
			this.mapping.addAgentActivityZones(person.getId(), tmp);
		}
	}
}
