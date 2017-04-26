/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.utils.plans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class BackwardCompatibilityForOldPlansType {

	public BackwardCompatibilityForOldPlansType(String inputPlansFile, Collection<String> mainModes2) {
		this.inputPlans = inputPlansFile;
		this.mainModes = mainModes2;
		this.scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	private final String inputPlans;
	private final Scenario scOut;
	private final Collection<String> mainModes ;

	public static void main(String[] args) {
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");
		BackwardCompatibilityForOldPlansType bcrouteTyp = new BackwardCompatibilityForOldPlansType("../../../../repos/runs-svn/patnaIndia/inputs/SelectedPlansOnly.xml", mainModes);
		bcrouteTyp.extractPlansExcludingLinkInfo();
		bcrouteTyp.writePopOut("../../../../repos/runs-svn/patnaIndia/inputs/SelectedPlans_new.xml.gz");
	}

	/**
	 * Excluding routes in legs and assigning activities on coords rather than on link 
	 * because, link information in network  may not be same after some time.
	 */
	public void extractPlansExcludingLinkInfo(){
		Scenario scIn = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		Population popOut = scOut.getPopulation();
		for (Person p : scIn.getPopulation().getPersons().values()) {
			Person pOut = popOut.getFactory().createPerson(p.getId());
			popOut.addPerson(pOut);
			for (Plan plan : p.getPlans()){
				Plan planOut = popOut.getFactory().createPlan();
				List<PlanElement> pes = plan.getPlanElements();
				for ( PlanElement pe : pes){
					if(pe instanceof Leg) {
						Leg leg = (Leg) pe;
						Leg legOut = popOut.getFactory().createLeg(leg.getMode());
						planOut.addLeg(legOut);
					} else {
						Activity actIn = (Activity)pe;
						Activity actOut = popOut.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
						actOut.setEndTime(actIn.getEndTime());
						planOut.addActivity(actOut);
					}
				}
				pOut.addPlan(planOut);
			}
		}
	}
	
	/**
	 * All main mode had "Generic" route type which is converted to "links" route (nov'15).
	 * Consequently, old plans file can not be used with the current head. Routes needs to be converted first.
	 */
	private void extractPlansIncludingLegRoutes(){
		Scenario scIn = LoadMyScenarios.loadScenarioFromPlans(inputPlans);
		Population popOut = scOut.getPopulation();
		for (Person p : scIn.getPopulation().getPersons().values()) {
			Person pOut = popOut.getFactory().createPerson(p.getId());
			popOut.addPerson(pOut);
			for (Plan plan : p.getPlans()){
				Plan planOut = popOut.getFactory().createPlan();
				List<PlanElement> pes = plan.getPlanElements();
				for ( PlanElement pe : pes){
					if(pe instanceof Leg) {
						Leg leg = (Leg) pe;
						Leg legOut = popOut.getFactory().createLeg(leg.getMode());
						if( this.mainModes.contains(leg.getMode()) ) {
							//	route here will be generic type which needs to be converted to links route type
							Route r = leg.getRoute();
							String routeLinks = r.getRouteDescription();
							List<Id<Link>> linkIds = convertRouteDescriptionToListOfLinkIds(routeLinks);
							NetworkRoute nr = popOut.getFactory().getRouteFactories().createRoute(NetworkRoute.class, r.getStartLinkId(), r.getEndLinkId());
							//exclude first and last lnik from linkIds
							if(linkIds.isEmpty()) {
							} else if(linkIds.size()==1) {
								linkIds.remove(0);
							} else {
								linkIds.remove(linkIds.size()-1);
								linkIds.remove(0);
							}
							nr.setLinkIds(r.getStartLinkId(), linkIds, r.getEndLinkId());
							nr.setDistance(r.getDistance());
							nr.setTravelTime(r.getTravelTime());
							legOut.setRoute(nr);
						} else {
							legOut = leg;
							// nothing to do here; already generic route
						}
						planOut.addLeg(legOut);
					} else {
						Activity actIn = (Activity)pe;
						planOut.addActivity(actIn);
					}
				}
				pOut.addPlan(planOut);
			}
		}
	}

	private List<Id<Link>> convertRouteDescriptionToListOfLinkIds(String routeDescription){
		List<Id<Link>> linkIds = new ArrayList<>();
		List<String> linkIdsAsString = Arrays.asList(routeDescription.split(" "));
		for ( String linkId :linkIdsAsString) {
			linkIds.add(Id.createLinkId(linkId));
		}
		return linkIds;
	}

	public Scenario getOutScenario() {
		return scOut;
	}

	public void writePopOut(String outputFile){
		new PopulationWriter(scOut.getPopulation()).write(outputFile);
	}
}
