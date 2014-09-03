///* *********************************************************************** *
// * project: org.matsim.*
// * PersonCreatePlanFromKnowledge.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.ciarif.modechoice_old;
//
//import java.util.ArrayList;
//
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.facilities.ActivityOptionImpl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.population.ActivityImpl;
//import org.matsim.core.population.LegImpl;
//import org.matsim.core.population.PersonImpl;
//import org.matsim.core.population.PlanImpl;
//import org.matsim.population.algorithms.AbstractPersonAlgorithm;
//
//public class PersonCreatePlanFromKnowledge extends AbstractPersonAlgorithm {
//
//	private final Knowledges knowledges;
//
//	public PersonCreatePlanFromKnowledge(Knowledges knowledges) {
//		super();
//		this.knowledges = knowledges;
//	}
//
//	@Override
//	public void run(final Person person) {
//		PlanImpl p = ((PersonImpl) person).createAndAddPlan(true);
//        ArrayList<ActivityOptionImpl> result1;
//        throw new RuntimeException("Knowledges are no more.");
//
//        ActivityFacility home_facility = result1.get(0).getFacility();
//        ArrayList<ActivityOptionImpl> result;
//        throw new RuntimeException("Knowledges are no more.");
//
//        ArrayList<ActivityOptionImpl> acts = result;
//
//		// first act end time = [7am.9am]
//		int time = 7*3600 + (MatsimRandom.getRandom().nextInt(2*3600));
//
//		// first act (= home)
//		ActivityImpl a = p.createAndAddActivity("home", home_facility.getCoord());
//		a.setLinkId(home_facility.getLinkId());
//		a.setStartTime(0.0);
//		a.setMaximumDuration(time);
//		a.setEndTime(time);
//		a.setFacilityId(home_facility.getId());
//		LegImpl l = p.createAndAddLeg(TransportMode.car);
//		l.setDepartureTime(time);
//		l.setTravelTime(0);
//		l.setArrivalTime(time);
//
//		int nof_acts = 1 + MatsimRandom.getRandom().nextInt(3);
//		int dur = 12*3600/nof_acts;
//
//		// in between acts
//		for (int i=0; i<nof_acts; i++) {
//			int act_index = MatsimRandom.getRandom().nextInt(acts.size());
//			ActivityOptionImpl act = acts.get(act_index);
//			ActivityFacility f = act.getFacility();
//			a = p.createAndAddActivity(act.getType(),f.getCoord());
//			a.setLinkId(f.getLinkId());
//			a.setStartTime(time);
//			a.setMaximumDuration(dur);
//			a.setEndTime(time + dur);
//			a.setFacilityId(f.getId());
//			time += dur;
//			l = p.createAndAddLeg(TransportMode.car);
//			l.setDepartureTime(time);
//			l.setTravelTime(0);
//			l.setArrivalTime(time);
//		}
//
//		// last act (= home)
//		a = p.createAndAddActivity("home",home_facility.getCoord());
//		a.setLinkId(home_facility.getLinkId());
//		a.setStartTime(time);
//		a.setEndTime(24*3600);
//		a.setMaximumDuration(24*3600 - time);
//		a.setFacilityId(home_facility.getId());
//	}
//}
