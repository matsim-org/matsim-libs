///* *********************************************************************** *
// * project: org.matsim.*
// * PlansDefineKnowledge.java
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
//import java.util.Iterator;
//
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.facilities.ActivityOption;
//import org.matsim.core.gbl.MatsimRandom;
//
//public class PlansDefineKnowledge {
//
//	private final ActivityFacilities facilities;
//	private Knowledges knowledges;
//
//	public PlansDefineKnowledge(final ActivityFacilities facilities, Knowledges knowledges) {
//		this.facilities = facilities;
//		this.knowledges = knowledges;
//	}
//
//	public void run(Population plans) {
//		System.out.println("    running " + this.getClass().getName() + " algorithm...");
//
//		// get home, work and other activities
//		ArrayList<ActivityOption> home_acts = new ArrayList<ActivityOption>();
//		ArrayList<ActivityOption> work_acts = new ArrayList<ActivityOption>();
//		ArrayList<ActivityOption> other_acts = new ArrayList<ActivityOption>();
//		for (ActivityFacility f : this.facilities.getFacilities().values()) {
//			Iterator<? extends ActivityOption> a_it = f.getActivityOptions().values().iterator();
//			while (a_it.hasNext()) {
//				ActivityOption a = a_it.next();
//				if (a.getType().equals("home")) { home_acts.add(a); }
//				else if (a.getType().equals("work")) { work_acts.add(a); }
//				else { other_acts.add(a); }
//			}
//		}
//
//		// set exactly one home and four other activities for each person
//		for (Person p : plans.getPersons().values()) {
//			Object k = this.knowledges.getFactory().createKnowledge(p.getId(), "created by " + this.getClass().getName());
//			int index = MatsimRandom.getRandom().nextInt(home_acts.size());
//            boolean result2;
//            throw new RuntimeException("Knowledges are no more.");
//
//            index = MatsimRandom.getRandom().nextInt(work_acts.size());
//            boolean result1;
//            throw new RuntimeException("Knowledges are no more.");
//
//            for (int i=0; i<4; i++) {
//				index = MatsimRandom.getRandom().nextInt(other_acts.size());
//                boolean result;
//                throw new RuntimeException("Knowledges are no more.");
//
//            }
//		}
//
//		System.out.println("    done.");
//	}
//}
