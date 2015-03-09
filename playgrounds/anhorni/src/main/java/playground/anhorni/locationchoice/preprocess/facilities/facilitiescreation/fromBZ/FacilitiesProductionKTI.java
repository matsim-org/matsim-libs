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

import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.OpeningTime.DayType;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.csestimation.Location;
import playground.anhorni.csestimation.ShopLocation;
import playground.anhorni.csestimation.UniversalChoiceSetReader;
import playground.anhorni.csestimation.Utils;

/**
 * Generates the facilities file for all of Switzerland from the Swiss
 * National Enterprise Census of the year 2000 (published 2001).
 */
public class FacilitiesProductionKTI {

	public enum KTIYear {KTI_YEAR_2007, KTI_YEAR_2008}

	// work
	public static final String ACT_TYPE_WORK = "w";
	public static final String WORK_SECTOR2 = "w"; // "work_sector2";
	public static final String WORK_SECTOR3 = "w"; // "work_sector3";

	// education
	public static final String ACT_TYPE_EDUCATION = "e";

	public static final String EDUCATION_KINDERGARTEN = ACT_TYPE_EDUCATION + "_kindergarten";
	public static final String EDUCATION_PRIMARY = ACT_TYPE_EDUCATION + "_primary";
	public static final String EDUCATION_SECONDARY = ACT_TYPE_EDUCATION + "_secondary";
	public static final String EDUCATION_HIGHER = ACT_TYPE_EDUCATION + "_higher";
	public static final String EDUCATION_OTHER = ACT_TYPE_EDUCATION + "_other";

	// shopping
	public static final String ACT_TYPE_SHOP = "s";
	public static final String SHOP_RETAIL_GT2500 = ACT_TYPE_SHOP + "_retail_gt2500sqm";
	public static final String SHOP_RETAIL_GET1000 = ACT_TYPE_SHOP + "_retail_get1000sqm";
	public static final String SHOP_RETAIL_GET400 = ACT_TYPE_SHOP + "_retail_get400sqm";
	public static final String SHOP_RETAIL_GET100 = ACT_TYPE_SHOP + "_retail_get100sqm";
	public static final String SHOP_RETAIL_LT100 = ACT_TYPE_SHOP + "_retail_lt100sqm";
	public static final String SHOP_OTHER = ACT_TYPE_SHOP + "_other";

	// leisure
	public static final String ACT_TYPE_LEISURE = "l";
	public static final String LEISURE_SPORTS = ACT_TYPE_LEISURE + "_sports";
	public static final String LEISURE_CULTURE = ACT_TYPE_LEISURE + "_culture";
	public static final String LEISURE_GASTRO = ACT_TYPE_LEISURE + "_gastro";
	public static final String LEISURE_HOSPITALITY = ACT_TYPE_LEISURE + "_hospitality";

	private static Logger log = Logger.getLogger(FacilitiesProductionKTI.class);
	private ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();

	private final ObjectAttributes facilitiesAttributes = new ObjectAttributes();

	private final Coord center = new CoordImpl(682756, 248732); // Letten
	private final double radius = 5000.0;

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
		String zhShops = args[4];
		String outFile = args[5];

		FacilitiesProductionKTI creator = new FacilitiesProductionKTI();
		creator.facilitiesProduction(
				KTIYear.KTI_YEAR_2008, inputHectareAggregationFile, presenceCodeFile, shopsOf2005Filename, facilitiesFile, zhShops, outFile);
	}

	public void facilitiesProduction(KTIYear ktiYear, String inputHectareAggregationFile,
			String presenceCodeFile, String shopsOf2005Filename, String facilitesFile, String zhShopsFile, String outdir) {
		this.facilities = new ActivityFacilitiesImpl();//(FacilitiesImpl)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);
		this.facilities.setName("Facilities based on the Swiss National Enterprise Census.");
		log.info("Adding and running facilities algorithms ...");
		new FacilitiesAllActivitiesFTE(ktiYear).run(this.facilities, inputHectareAggregationFile, presenceCodeFile);
		AddOpentimes addOpentimes = new AddOpentimes(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())), shopsOf2005Filename);
		addOpentimes.init();
		addOpentimes.run(this.facilities);

		this.assignSizeAndPrice(zhShopsFile);

		log.info("adding home facilities ... ");
		ScenarioImpl scenario = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(scenario);
		facilities_reader.readFile(facilitesFile);
		this.combineFacilities(scenario);

		log.info("  writing facilities file... ");
		new FacilitiesWriter(this.facilities).write(outdir + "facilities_fr.xml.gz");
		log.info("Writting: " + this.facilities.getFacilities().size() + " facilities --------------------- ");

		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.facilitiesAttributes);
		attributesWriter.writeFile(outdir + "/facilitiesAttributes.xml");
	}

	private void combineFacilities(ScenarioImpl scenario) {
		for (ActivityFacility f : scenario.getActivityFacilities().getFacilities().values()) {
			if (f.getActivityOptions().containsKey("home")) {
				this.facilities.createAndAddFacility(f.getId(), f.getCoord());
				ActivityFacilityImpl facility = (ActivityFacilityImpl)this.facilities.getFacilities().get(f.getId());
				ActivityOptionImpl ao = new ActivityOptionImpl("h");
				ao.addOpeningTime(new OpeningTimeImpl(DayType.wk, 0.0 * 3600, 24.0 * 3600));
				facility.addActivityOption(ao);
			}
		}
	}

	private void assignSizeAndPrice(String zhShopsFile) {
		CH1903LV03toWGS84 trafo = new CH1903LV03toWGS84();
		WGS84toCH1903LV03 trafoback = new WGS84toCH1903LV03();
		QuadTree<Location> zhShopsQuadTree = this.createZHShops(zhShopsFile);

		int sizeCnt = 0;
		int priceCnt = 0;

		for (ActivityFacility f : this.facilities.getFacilitiesForActivityType("s").values()) {
			ShopLocation shop = (ShopLocation) zhShopsQuadTree.get(
					trafo.transform(f.getCoord()).getX(), trafo.transform(f.getCoord()).getY());
			if (CoordUtils.calcDistance(f.getCoord(), trafoback.transform(shop.getCoord())) < 150.0 &&
					this.isFood(f)) {
				this.facilitiesAttributes.putAttribute(f.getId().toString(), "price", 1.0 * shop.getPrice());

				double dist = CoordUtils.calcDistance(f.getCoord(), this.center);
					if (dist < this.radius) {
						((ActivityFacilityImpl)f).createActivityOption("sg");

						// copy opening times
						ActivityOptionImpl optionNew = (ActivityOptionImpl) f.getActivityOptions().get("sg");

						SortedSet<OpeningTime> ot = ((ActivityOptionImpl)f.getActivityOptions().get("s")).getOpeningTimes();
						optionNew.setOpeningTimes(ot);
					}
				if (shop.getPrice() > 0) priceCnt++;

			}
			else {
				this.facilitiesAttributes.putAttribute(f.getId().toString(), "price", -1.0);
			}
			int size = this.assignSize(f);
			this.facilitiesAttributes.putAttribute(f.getId().toString(), "size", size);

			if (size > 0) sizeCnt++;
		}
		log.info("price cnt: " + priceCnt + " size cnt: " + sizeCnt);
	}

	private boolean isFood(ActivityFacility f) {
		boolean isFood = false;
		if (f.getActivityOptions().containsKey("B015211E") ||
				f.getActivityOptions().containsKey("B015211D") ||
				f.getActivityOptions().containsKey("B015211C") ||
				f.getActivityOptions().containsKey("B015211B") ||
				f.getActivityOptions().containsKey("B015211A")) {
			isFood = true;
		}
		return isFood;
	}

	private QuadTree<Location> createZHShops(String zhShopsFile) {
		UniversalChoiceSetReader ucsReader = new UniversalChoiceSetReader();
		TreeMap<Id<Location>, ShopLocation> shops = new TreeMap<Id<Location>, ShopLocation>();
		shops = ucsReader.readUniversalCS(zhShopsFile);

		this.assignPrice(shops);

		QuadTree<Location> zhShopsQuadTree = Utils.buildLocationQuadTree(shops);
		return zhShopsQuadTree;
	}

	/*
	2008:
	B0808471101;B0808471102;B0808471103;B0808471104;B0808471105
	471101	5	Verbrauchermärkte (> 2500 m2)
	471102	4	Grosse Supermärkte (1000-2499 m2)
	471103	3	Kleine Supermärkte (400-999 m2)
	471104	2	Grosse Geschäfte (100-399 m2)
	471105	1	Kleine Geschäfte (< 100 m2)

	2001:
	B015211A	|	shop_retail_gt2500sqm
	B015211B	|	shop_retail_get1000sqm
	B015211C	|	shop_retail_get400sqm
	B015211D	|	shop_retail_get100sqm
	B015211E	|	shop_retail_lt100sqm
	*/
	private int assignSize(ActivityFacility f) {
		int size = -1;
		if (f.getActivityOptions().containsKey("B015211E")) {
			size = 1;
		}
		if (f.getActivityOptions().containsKey("B015211D")) {
			size = 2;
		}
		if (f.getActivityOptions().containsKey("B015211C")) {
			size = 3;
		}
		if (f.getActivityOptions().containsKey("B015211B")) {
			size = 4;
		}
		if (f.getActivityOptions().containsKey("B015211A")) {
			size = 4;
		}
		return size;
	}

	private void assignPrice(TreeMap<Id<Location>, ShopLocation> shops) {
		for (ShopLocation shop:shops.values()) {
			int lidl_aldi = 1;
			int denner = 2;
			int migros_coop = 3;
			int spar_other = 4;
			int marinello_globus = 5;
			int idint = Integer.parseInt(shop.getId().toString());

			if (idint < 20000) {
				shop.setPrice(denner);
			}
			else if (idint > 20000 && idint < 30000) {
				shop.setPrice(spar_other);
			}
			else if ((idint > 30000 && idint < 50000)) {
				shop.setPrice(migros_coop);
			}
			else if ((idint > 60000 && idint < 70000) || (idint == 100004) || (idint == 100005) || (idint == 100006)) {
				shop.setPrice(marinello_globus);
			}
			else if ((idint > 70000 && idint < 80000) || (idint == 100074) || (idint == 100147) || (idint == 100148)) {
				shop.setPrice(lidl_aldi);
			}
			else {
				shop.setPrice(spar_other);
			}
		}
	}
}
