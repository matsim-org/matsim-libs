/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesProductionKTIYear1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Generates the facilities file for all of Switzerland from the Swiss
 * National Enterprise Census of the year 2000 (published 2001).
 */
public class FacilitiesProductionKTI {

	public enum KTIYear {KTI_YEAR_2007, KTI_YEAR_2008}

	// work
	public static final String ACT_TYPE_WORK = "work";
	public static final String WORK_SECTOR2 = "work_sector2";
	public static final String WORK_SECTOR3 = "work_sector3";

	// education
	public static final String ACT_TYPE_EDUCATION = "education";

	public static final String EDUCATION_KINDERGARTEN = ACT_TYPE_EDUCATION + "_kindergarten";
	public static final String EDUCATION_PRIMARY = ACT_TYPE_EDUCATION + "_primary";
	public static final String EDUCATION_SECONDARY = ACT_TYPE_EDUCATION + "_secondary";
	public static final String EDUCATION_HIGHER = ACT_TYPE_EDUCATION + "_higher";
	public static final String EDUCATION_OTHER = ACT_TYPE_EDUCATION + "_other";

	// shopping
	public static final String ACT_TYPE_SHOP = "shop";
	public static final String SHOP_RETAIL_GT2500 = ACT_TYPE_SHOP + "_retail_gt2500sqm";
	public static final String SHOP_RETAIL_GET1000 = ACT_TYPE_SHOP + "_retail_get1000sqm";
	public static final String SHOP_RETAIL_GET400 = ACT_TYPE_SHOP + "_retail_get400sqm";
	public static final String SHOP_RETAIL_GET100 = ACT_TYPE_SHOP + "_retail_get100sqm";
	public static final String SHOP_RETAIL_LT100 = ACT_TYPE_SHOP + "_retail_lt100sqm";
	public static final String SHOP_OTHER = ACT_TYPE_SHOP + "_other";

	// leisure
	public static final String ACT_TYPE_LEISURE = "leisure";
	public static final String LEISURE_SPORTS = ACT_TYPE_LEISURE + "_sports";
	public static final String LEISURE_CULTURE = ACT_TYPE_LEISURE + "_culture";
	public static final String LEISURE_GASTRO = ACT_TYPE_LEISURE + "_gastro";
	public static final String LEISURE_HOSPITALITY = ACT_TYPE_LEISURE + "_hospitality";

	private static Logger log = Logger.getLogger(FacilitiesProductionKTI.class);	
	private ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
	
	/**
	 * @param 
	 * 	inputHectareAggregationFile BZ01_UNT.TXT
	 *  presenceCodeFile BZ01_UNT_P_DSVIEW.TXT
	 */
	public static void main(String[] args) {
		String inputHectareAggregationFile = args[0];
		String presenceCodeFile = args[1];
		String shopsOf2005Filename = args[2];
		String facilitiesFile = args[3];
		String outFile = args[4];
		
		FacilitiesProductionKTI creator = new FacilitiesProductionKTI();
		creator.facilitiesProduction(
				KTIYear.KTI_YEAR_2008, inputHectareAggregationFile, presenceCodeFile, shopsOf2005Filename, facilitiesFile, outFile);
	}
	
	public void facilitiesProduction(KTIYear ktiYear, String inputHectareAggregationFile, 
			String presenceCodeFile, String shopsOf2005Filename, String facilitesFile, String outFile) {		
		this.facilities = new ActivityFacilitiesImpl();//(FacilitiesImpl)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);	
		facilities.setName("Facilities based on the Swiss National Enterprise Census.");		
		log.info("Adding and running facilities algorithms ...");
		new FacilitiesAllActivitiesFTE(ktiYear).run(facilities, inputHectareAggregationFile, presenceCodeFile);
		AddOpentimes addOpentimes = new AddOpentimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())), shopsOf2005Filename);
		addOpentimes.init();
		addOpentimes.run(this.facilities);
		log.info("adding home facilities ... ");
		ScenarioImpl scenario = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		facilities_reader.readFile(facilitesFile);		
		this.combineFacilities(scenario);
		
		log.info("  writing facilities file... ");
		new FacilitiesWriter(this.facilities).write(outFile);
		log.info("Writting: " + this.facilities.getFacilities().size() + " facilities --------------------- ");	
	}
	
	private void combineFacilities(ScenarioImpl scenario) {
		for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
			if (f.getActivityOptions().containsKey("home")) {
				this.facilities.createFacility(f.getId(), f.getCoord());
				ActivityFacilityImpl facility= (ActivityFacilityImpl)this.facilities.getFacilities().get(f.getId());	
				ActivityOptionImpl ao = new ActivityOptionImpl("h", f);
				ao.addOpeningTime(new OpeningTimeImpl(DayType.wk, 0.0 * 3600, 24.0 * 3600));
				facility.getActivityOptions().put("h", ao);
			}
		}
	}
}
