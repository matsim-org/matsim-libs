/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityProduction.java
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

import playground.staheale.preprocess.AddOpentimes;
import playground.staheale.preprocess.FacilitiesCreation;

import java.io.IOException;

import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.FacilitiesWriter;

public class FacilitiesProduction {
	
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

	// retail shopping
	public static final String ACT_TYPE_SHOP_RETAIL = "shop_retail";
	public static final String SHOP_RETAIL_GT2500 = ACT_TYPE_SHOP_RETAIL + "_gt2500sqm";
	public static final String SHOP_RETAIL_GET1000 = ACT_TYPE_SHOP_RETAIL + "_get1000sqm";
	public static final String SHOP_RETAIL_GET400 = ACT_TYPE_SHOP_RETAIL + "_get400sqm";
	public static final String SHOP_RETAIL_GET100 = ACT_TYPE_SHOP_RETAIL + "_get100sqm";
	public static final String SHOP_RETAIL_LT100 = ACT_TYPE_SHOP_RETAIL + "_lt100sqm";
	public static final String SHOP_RETAIL_OTHER = ACT_TYPE_SHOP_RETAIL + "_other";

	// service shopping
	public static final String ACT_TYPE_SHOP_SERVICE = "shop_service";
	public static final String SHOP_SERVICE = ACT_TYPE_SHOP_SERVICE;
	
	// sports & fun
	public static final String ACT_TYPE_SPORTS_FUN = "sports_fun";
	public static final String SPORTS_FUN = ACT_TYPE_SPORTS_FUN;
	
	// gastro & culture
	public static final String ACT_TYPE_GASTRO_CULTURE = "gastro_culture";
	public static final String GASTRO_CULTURE = ACT_TYPE_GASTRO_CULTURE;
	
	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.loadConfig(args[0]);
		FacilitiesProduction.facilitiesProduction(KTIYear.KTI_YEAR_2008, config);

	}

	private static void facilitiesProduction(KTIYear ktiYear, Config config) {

		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();

		facilities.setName(
				"Facilities based on the Swiss National Enterprise Census of the year 2000."
				);

		new FacilitiesCreation(ktiYear).run(facilities);
		AddOpentimes addOpentimes = new AddOpentimes();
		addOpentimes.init();
		for (ActivityFacility facility: facilities.getFacilities().values()) {
			addOpentimes.run(facility);
		}
		

		System.out.println("  writing facilities file... ");
		new FacilitiesWriter(facilities).write("output/facilities.xml.gz");
		System.out.println("  done.");

	}
}
