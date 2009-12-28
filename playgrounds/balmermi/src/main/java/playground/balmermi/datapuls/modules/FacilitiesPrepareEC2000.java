/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesPrepareEC2000.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.datapuls.modules;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;

public class FacilitiesPrepareEC2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesPrepareEC2000.class);
	private final static int ID_SHIFT = 1000000000;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesPrepareEC2000() {
		super();
		log.info("init " + this.getClass().getName() + " module...");
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void setIdsAndCaps(final ActivityFacilitiesImpl facilities) {
		log.info("  setting new ids with shift of "+ID_SHIFT+" and setting caps <1 to 1...");
		log.info("    number of facilities: " + facilities.getFacilities().size());
		
		int capCnt = 0;
		Map<Id,ActivityFacilityImpl> afs = new TreeMap<Id, ActivityFacilityImpl>(facilities.getFacilities());
		facilities.getFacilities().clear();
		for (ActivityFacilityImpl af : afs.values()) {
			ActivityFacilityImpl afNew = facilities.createFacility(new IdImpl(Integer.parseInt(af.getId().toString())+ID_SHIFT),af.getCoord());
			afNew.getActivityOptions().putAll(af.getActivityOptions());
			for (ActivityOptionImpl ao : afNew.getActivityOptions().values()) {
				if (ao.getCapacity() < 1) { ao.setCapacity(1.0); capCnt++; }
			}
		}

		log.info("    => number of capacities adapted: " + capCnt);
		log.info("    => number of facilities: " + facilities.getFacilities().size());
		log.info("  done.");
	}
	
	private final void reduceShopAndLeisureOptions(final ActivityFacilitiesImpl facilities) {
		log.info("  reducing different shop and leisure act-options to 'shop' and 'leisure'...");
		log.info("    number of facilities: " + facilities.getFacilities().size());

		int shopCnt = 0;
		int leisCnt = 0;
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			TreeMap<String,ActivityOptionImpl> s_map = new TreeMap<String, ActivityOptionImpl>();
			TreeMap<String,ActivityOptionImpl> l_map = new TreeMap<String, ActivityOptionImpl>();
			for (String t : f.getActivityOptions().keySet()) {
				if (t.equals("shop_other") || t.equals("shop_retail_get1000sqm") || t.equals("shop_retail_get100sqm") ||
				    t.equals("shop_retail_get400sqm") || t.equals("shop_retail_gt2500sqm") || t.equals("shop_retail_lt100sqm")) {
					s_map.put(t,f.getActivityOptions().get(t));
				}
				if (t.equals("leisure_culture") || t.equals("leisure_gastro") || t.equals("leisure_sports")) {
					l_map.put(t,f.getActivityOptions().get(t));
				}
			}
			if (s_map.size() > 1) { throw new RuntimeException("fid="+f.getId()+": more than one shopping activity!"); }
			if (l_map.size() > 1) { throw new RuntimeException("fid="+f.getId()+": more than one leisure activity!"); }

			if (!s_map.isEmpty()) {
				ActivityOptionImpl old_act = s_map.values().iterator().next();
				ActivityOptionImpl new_act = f.createActivityOption("shop");
				new_act.setCapacity(old_act.getCapacity());
				new_act.setOpeningTimes(old_act.getOpeningTimes());
				f.getActivityOptions().remove(old_act.getType());
				shopCnt++;
			}
			if (!l_map.isEmpty()) {
				ActivityOptionImpl old_act = l_map.values().iterator().next();
				ActivityOptionImpl new_act = f.createActivityOption("leisure");
				new_act.setCapacity(old_act.getCapacity());
				new_act.setOpeningTimes(old_act.getOpeningTimes());
				f.getActivityOptions().remove(old_act.getType());
				leisCnt++;
			}
		}

		log.info("    => number of shop options adapted: " + shopCnt);
		log.info("    => number of leisure options adapted: " + leisCnt);
		log.info("    => number of facilities: " + facilities.getFacilities().size());
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilitiesImpl facilities) {
		log.info("running " + this.getClass().getName() + " module...");
		setIdsAndCaps(facilities);
		reduceShopAndLeisureOptions(facilities);
		log.info("done.");
	}
}
