/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.ls;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.router.TransitActsRemover;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;
import playground.jbischoff.utils.JbUtils;

/**
 * @author jbischoff
 *
 */
public class PlansPreparator {
	String path = "C:/Users/Joschka/Desktop/public/";
	String newPopFile = path + "taxibuspop.xml.gz";
	String shapefile = path + "shp/zones1.shp";
	// designated taxibus corridor

	String popfile = path + "output/cb01/output_plans.xml.gz";
	String networkfile = path + "output/cb01/output_network.xml.gz";
	Geometry geo;
	List<Id<Person>> agentsToModify;
	Scenario scenario;
	Id<Link> interactionLinkId = Id.createLinkId("567");
	Id<Link> interactionLinkBackId = Id.createLinkId("566");

	private void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		geo = JbUtils.readShapeFileAndExtractGeometry(shapefile, "ID").get("schmellwitz");
		new MatsimPopulationReader(scenario).readFile(popfile);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (Person p : scenario.getPopulation().getPersons().values()) {

			Person newP = pop2.getFactory().createPerson(p.getId());
			Plan plan = p.getSelectedPlan();
			Plan newPlan = pop2.getFactory().createPlan();
			newP.addPlan(newPlan);
			pop2.addPerson(newP);

			new TransitActsRemover().run(plan);
			for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
				Activity act = (Activity) plan.getPlanElements().get(i);
				newPlan.addActivity(act);
				
				
				
				if (i + 1 < plan.getPlanElements().size()) {
					Leg possibletbleg = (Leg) plan.getPlanElements().get(i + 1);
					newPlan.addLeg(possibletbleg);
					Activity nextAct = (Activity) plan.getPlanElements().get(i + 2);
					Coord actCoord = scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord();
					Coord nextActCoord = scenario.getNetwork().getLinks().get(nextAct.getLinkId()).getCoord();
					if ((geo.contains(MGC.coord2Point(actCoord)))
							&& ((!geo.contains(MGC.coord2Point(nextAct.getCoord()))))) {
						if (possibletbleg.getMode().equals(TransportMode.pt)) {
							possibletbleg.setRoute(null);
							possibletbleg.setMode(TaxibusUtils.TAXIBUS_MODE);
							ActivityImpl interactionAct = (ActivityImpl) pop2.getFactory().createActivityFromLinkId("taxibus interaction",
									interactionLinkId);
							interactionAct.setMaximumDuration(120);
							interactionAct.setCoord(new Coord(454101.0123827555,5735442.673285995));
							newPlan.addActivity(interactionAct);
							newPlan.addLeg(pop2.getFactory().createLeg(TransportMode.pt));
						}

					} else if((!geo.contains(MGC.coord2Point(actCoord)))
							&& ((geo.contains(MGC.coord2Point(nextActCoord))))) {
						
						if (possibletbleg.getMode().equals(TransportMode.pt)) {
							possibletbleg.setRoute(null);
							ActivityImpl interactionAct = (ActivityImpl) pop2.getFactory().createActivityFromLinkId("taxibus interaction",
									interactionLinkBackId);
							interactionAct.setCoord(new Coord(454101.0123827555,5735442.673285995));
							interactionAct.setMaximumDuration(120);
							newPlan.addActivity(interactionAct);
							newPlan.addLeg(pop2.getFactory().createLeg(TaxibusUtils.TAXIBUS_MODE));
						}
						
					}

				}
			}

		}

		
		
		
		new PopulationWriter(pop2).write(newPopFile);

	}

	public static void main(String[] args) {
		new PlansPreparator().run();

	}
}
