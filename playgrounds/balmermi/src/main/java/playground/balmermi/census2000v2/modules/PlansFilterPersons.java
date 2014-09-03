///* *********************************************************************** *
// * project: org.matsim.*
// * PlansFilterArea.java
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
//package playground.balmermi.census2000v2.modules;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Set;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.facilities.ActivityFacilityImpl;
//import org.matsim.core.facilities.ActivityOptionImpl;
//
//import playground.balmermi.census2000v2.data.CAtts;
//import playground.balmermi.census2000v2.data.Household;
//
//public class PlansFilterPersons {
//
//	//////////////////////////////////////////////////////////////////////
//	// member variables
//	//////////////////////////////////////////////////////////////////////
//
//	private final static Logger log = Logger.getLogger(PlansFilterPersons.class);
//	private Object knowledges;
//
//	//////////////////////////////////////////////////////////////////////
//	// constructors
//	////////////////////////////////////////////////////////////////////
//
//	public PlansFilterPersons(Object knowledges) {
//		super();
//		this.knowledges = knowledges;
//		log.info("    init " + this.getClass().getName() + " module...");
//		log.info("    done.");
//	}
//
//	//////////////////////////////////////////////////////////////////////
//	// private methods
//	//////////////////////////////////////////////////////////////////////
//
//	//////////////////////////////////////////////////////////////////////
//	// run method
//	//////////////////////////////////////////////////////////////////////
//
//	public void run(final Population plans) {
//		log.info("    running " + this.getClass().getName() + " module...");
//
//		// remove persons which are only part of the 'zivilrechtliche' population
//		Set<Person> persons = new HashSet<Person>();
//		for (Person p : plans.getPersons().values()) {
//			if (p.getCustomAttributes().get(CAtts.HH_W) == null) { persons.add(p); }
//		}
//		for (Person p : persons) {
//			Object o = p.getCustomAttributes().get(CAtts.HH_Z);
//			if (o == null) { throw new RuntimeException("pid="+p.getId()+": no hh_z. That must not happen!"); }
//			((Household)o).removePersonZ(p.getId());
//		}
//		log.info("      "+persons.size()+" persons without '"+CAtts.HH_W+"' household.");
//
//		// remove 'zivilrechtliche' data from the population
//		for (Person p : plans.getPersons().values()) {
//			Object o = p.getCustomAttributes().get(CAtts.HH_Z);
//			if (o != null) {
//				ActivityFacilityImpl f = ((Household)o).getFacility();
//
//				((Household)o).removePersonZ(p.getId());
//				p.getCustomAttributes().remove(CAtts.HH_Z);
//
//                ArrayList<ActivityOptionImpl> result3;
//                throw new RuntimeException("Knowledges are no more.");
//
//                if (result3.size() == 2) {
//                    ArrayList<ActivityOptionImpl> result2;
//                    throw new RuntimeException("Knowledges are no more.");
//
//                    ActivityOptionImpl a0 = result2.get(0);
//                    ArrayList<ActivityOptionImpl> result1;
//                    throw new RuntimeException("Knowledges are no more.");
//
//                    ActivityOptionImpl a1 = result1.get(1);
//					if (a0.getFacility().getId().equals(f.getId())) {
//                        boolean result;
//                        throw new RuntimeException("Knowledges are no more.");
//
//                        if (!result) { throw new RuntimeException("pid="+p.getId()+": That must not happen!"); }
//					}
//					else if (a1.getFacility().getId().equals(f.getId())) {
//                        boolean result;
//                        throw new RuntimeException("Knowledges are no more.");
//
//                        if (!result) { throw new RuntimeException("pid="+p.getId()+": That must not happen!"); }
//					}
//					else {
//						throw new RuntimeException("pid="+p.getId()+": That must not happen!");
//					}
//				}
//			}
//			// checks
//			if (p.getCustomAttributes().get(CAtts.HH_Z) != null) {
//					throw new RuntimeException("pid="+p.getId()+": Still containing hh_z!");
//				}
//			if (p.getCustomAttributes().get(CAtts.HH_W) == null) {
//				throw new RuntimeException("pid="+p.getId()+": No hh_w!");
//			}
//            ArrayList<ActivityOptionImpl> result1;
//            throw new RuntimeException("Knowledges are no more.");
//
//            if (result1.size() != 1) {
//                ArrayList<ActivityOptionImpl> result;
//                throw new RuntimeException("Knowledges are no more.");
//
//                throw new RuntimeException("pid="+p.getId()+": "+ result.size() + " home acts!");
//			}
//		}
//
//		log.info("    done.");
//	}
//}
