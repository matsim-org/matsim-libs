///* *********************************************************************** *
// * project: org.matsim.*
// * FacilitiesCombine.java
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
//import java.util.TreeMap;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.facilities.ActivityFacilitiesImpl;
//import org.matsim.core.facilities.ActivityFacilityImpl;
//import org.matsim.core.facilities.ActivityOptionImpl;
//import org.matsim.core.gbl.Gbl;
//
//public class FacilitiesReduceTypes {
//
//	//////////////////////////////////////////////////////////////////////
//	// member variables
//	//////////////////////////////////////////////////////////////////////
//
//	private final static Logger log = Logger.getLogger(FacilitiesDistributeCenter.class);
//
//	//////////////////////////////////////////////////////////////////////
//	// constructors
//	//////////////////////////////////////////////////////////////////////
//
//	public FacilitiesReduceTypes() {
//		super();
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
//	public void run(final ActivityFacilitiesImpl facilities) {
//		log.info("    running " + this.getClass().getName() + " module...");
//		
//		for (ActivityFacility f : facilities.getFacilities().values()) {
//			TreeMap<String,ActivityOptionImpl> h_map = new TreeMap<String, ActivityOptionImpl>();
//			TreeMap<String,ActivityOptionImpl> w_map = new TreeMap<String, ActivityOptionImpl>();
//			TreeMap<String,ActivityOptionImpl> e_map = new TreeMap<String, ActivityOptionImpl>();
//			TreeMap<String,ActivityOptionImpl> s_map = new TreeMap<String, ActivityOptionImpl>();
//			TreeMap<String,ActivityOptionImpl> l_map = new TreeMap<String, ActivityOptionImpl>();
//			TreeMap<String,ActivityOptionImpl> t_map = new TreeMap<String, ActivityOptionImpl>();
//			for (String t : f.getActivityOptions().keySet()) {
//				if (t.equals("home")) {
//					h_map.put(t,f.getActivityOptions().get(t));
//				}
//				if (t.equals("work_sector2") || t.equals("work_sector3")) {
//					w_map.put(t,f.getActivityOptions().get(t));
//				}
//				if (t.equals("education_higher") || t.equals("education_kindergarten") || t.equals("education_other") ||
//				    t.equals("education_primary") || t.equals("education_secondary")) {
//					e_map.put(t,f.getActivityOptions().get(t));
//				}
//				if (t.equals("shop_other") || t.equals("shop_retail_get1000sqm") || t.equals("shop_retail_get100sqm") ||
//				    t.equals("shop_retail_get400sqm") || t.equals("shop_retail_gt2500sqm") || t.equals("shop_retail_lt100sqm")) {
//					s_map.put(t,f.getActivityOptions().get(t));
//				}
//				if (t.equals("leisure_culture") || t.equals("leisure_gastro") || t.equals("leisure_sports")) {
//					l_map.put(t,f.getActivityOptions().get(t));
//				}
//				if (t.equals("tta")) {
//					t_map.put(t,f.getActivityOptions().get(t));
//				}
//			}
//			if (h_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one home activity!"); }
//			if (w_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one work activity!"); }
//			if (e_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one education activity!"); }
//			if (s_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one shopping activity!"); }
//			if (l_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one leisure activity!"); }
//			if (t_map.size() > 1) { Gbl.errorMsg("fid="+f.getId()+": more than one tta activity!"); }
//
//			StringBuffer desc = new StringBuffer();
//			if (!h_map.isEmpty()) { desc.append("("); desc.append(h_map.keySet().iterator().next()); desc.append(")");}
//			if (!w_map.isEmpty()) { desc.append("("); desc.append(w_map.keySet().iterator().next()); desc.append(")");}
//			if (!e_map.isEmpty()) { desc.append("("); desc.append(e_map.keySet().iterator().next()); desc.append(")");}
//			if (!s_map.isEmpty()) { desc.append("("); desc.append(s_map.keySet().iterator().next()); desc.append(")");}
//			if (!l_map.isEmpty()) { desc.append("("); desc.append(l_map.keySet().iterator().next()); desc.append(")");}
//			if (!t_map.isEmpty()) { desc.append("("); desc.append(t_map.keySet().iterator().next()); desc.append(")");}
//			f.setDesc(desc.toString());
//			
//			if (!s_map.isEmpty()) {
//				ActivityOptionImpl old_act = s_map.values().iterator().next();
//				ActivityOptionImpl new_act = f.createActivityOption("shop");
//				new_act.setCapacity(old_act.getCapacity());
//				new_act.setOpeningTimes(old_act.getOpeningTimes());
//				f.getActivityOptions().remove(old_act.getType());
//			}
//			if (!l_map.isEmpty()) {
//				ActivityOptionImpl old_act = l_map.values().iterator().next();
//				ActivityOptionImpl new_act = f.createActivityOption("leisure");
//				new_act.setCapacity(old_act.getCapacity());
//				new_act.setOpeningTimes(old_act.getOpeningTimes());
//				f.getActivityOptions().remove(old_act.getType());
//			}
//		}
//
//		log.info("    done.");
//	}
//}
