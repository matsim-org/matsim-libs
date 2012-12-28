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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author mrieser
 */
public class CalculateActivityToZoneMapping {

	private final ActivityToZoneMapping mapping;
	private final Zones zones;
	private final String idAttributeName;

	public CalculateActivityToZoneMapping(final ActivityToZoneMapping mapping, final Zones zones, final String idAttributeName) {
		this.mapping = mapping;
		this.zones = zones;
		this.idAttributeName = idAttributeName;
	}

	public void run(final Population population) {
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			ArrayList<String> zoneIds = new ArrayList<String>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					SimpleFeature zone = this.zones.getContainingZone(act.getCoord().getX(), act.getCoord().getY());
					if (zone != null) {
						zoneIds.add(zone.getAttribute(this.idAttributeName).toString());
					} else {
						zoneIds.add(null);
					}
				}
			}
			String[] tmp = zoneIds.toArray(new String[zoneIds.size()]);
			this.mapping.addAgentActivityZones(person.getId(), tmp);
		}
	}
}
