/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.staheale.preprocess;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class AdaptFacilities {

	private static Logger log = Logger.getLogger(AdaptFacilities.class);
	private ScenarioImpl scenario;
	private final Coord ZurichCenter = new Coord(683508.5, 246832.9063);

	public AdaptFacilities() {
		super();
	}

	public static void main(String[] args) throws IOException {
		AdaptFacilities adaptFacilities = new AdaptFacilities();
		adaptFacilities.run();
	}

	public void run() {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader(this.scenario);
		System.out.println("Reading facilities xml file... ");
		FacReader.readFile("./input/facilities.xml.gz");
		System.out.println("Reading facilities xml file...done.");
		ActivityFacilities facilities = this.scenario.getActivityFacilities();
		log.info("Number of facilities: " +facilities.getFacilities().size());

		    for (ActivityFacility f : facilities.getFacilities().values()) {
		    	if (f.getActivityOptions().containsKey("shop_retail")){
		    		ActivityFacilityImpl fImpl = (ActivityFacilityImpl) f;
		    		ActivityOption a = fImpl.getActivityOptions().get("shop_retail");
//		    		double cap = a.getCapacity();
//		    		fImpl.getActivityOptions().put("shop_retail", new ActivityOptionImpl("shop", fImpl));
		    		fImpl.addActivityOption(new ActivityOptionImpl("shop"));

					if (a.getOpeningTimes()!=null) {;}

		    	}
		    	if (f.getActivityOptions().containsKey("shop_service")){
//		    		ActivityFacilityImpl fImpl = (ActivityFacilityImpl) f;
//		    		fImpl.getActivityOptions().put("shop_service", new ActivityOptionImpl("shop", fImpl));
		    		f.addActivityOption(new ActivityOptionImpl("shop"));
		    	}
		    	if (f.getActivityOptions().containsKey("leisure_gastro_culture")){
//		    		ActivityFacilityImpl fImpl = (ActivityFacilityImpl) f;
//		    		fImpl.getActivityOptions().put("leisure_gastro_culture", new ActivityOptionImpl("leisure", fImpl));
		    		f.addActivityOption(new ActivityOptionImpl("leisure"));
		    	}
		    	if (f.getActivityOptions().containsKey("leisure_sports_fun")){
//		    		ActivityFacilityImpl fImpl = (ActivityFacilityImpl) f;
//		    		fImpl.getActivityOptions().put("leisure_sports_fun", new ActivityOptionImpl("leisure", fImpl));
		    		f.addActivityOption(new ActivityOptionImpl("leisure"));
		    	}
		    }

		    new FacilitiesWriter(facilities).write("./output/facilitiesAdapted2013.xml.gz");


		//    for (ActivityFacility f : facilities.getFacilitiesForActivityType("shop_retail").values()) {
		//    	double cap = Math.max(10,Math.round((f.getActivityOptions().get("shop_retail").getCapacity())/10));
		//    	f.getActivityOptions().get("shop_retail").setCapacity(cap);
		//    }

//		try {
//
//			final String header="Facility_id\tx\ty\tdistance\tunder12";
//			final BufferedWriter out =
//					IOUtils.getBufferedWriter("./output/distance.txt");
//
//			out.write(header);
//			out.newLine();
//
//			for (ActivityFacility facility : facilities.getFacilities().values()) {
//				out.write(facility.getId().toString() + "\t"+
//						facility.getCoord().getX() + "\t"+
//						facility.getCoord().getY()
//						+ "\t");
//				double distance = CoordUtils.calcDistance(facility.getCoord(), ZurichCenter);
//				out.write((int) distance +"\t");
//				if (distance < 12000){
//					out.write("1\t");
//				}
//				else {
//					out.write("0\t");
//				}
//				out.newLine();
//			}
//			out.flush();
//			out.close();
//		} catch (final IOException e) {
//			Gbl.errorMsg(e);
//		}
	}
}
