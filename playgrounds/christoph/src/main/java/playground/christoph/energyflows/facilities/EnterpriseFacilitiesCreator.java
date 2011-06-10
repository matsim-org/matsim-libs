/* *********************************************************************** *
 * project: org.matsim.*
 * EnterpriseFacilitiesCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows.facilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class EnterpriseFacilitiesCreator {

final private static Logger log = Logger.getLogger(EnterpriseFacilitiesCreator.class);
	
//	private String enterpriseToBuildingTextFile = "../../matsim/mysimulations/2kw/gis/EnterpriseToBuilding.txt";
//	private String enterpriseCensusVAETextFile =  "../../matsim/mysimulations/2kw/gis/AST_08_Capacities.csv";
//	private String enterpriseCensusTextFile = "../../matsim/mysimulations/2kw/facilities/AST_08.csv";
//	
//	private String inFacilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/facilitiesZH.xml.gz";
//	private String existingFacilitiesFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
//	private String outFacilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/output_facilitiesZH.xml.gz";
//	private String outEmptyFacilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/output_emptyFacilitiesZH.txt";
	
	private String muncipalityId = "261";	// Zurich city Id
		
	private String delimiter = ",";
	private String delimiter2 = ";";
	private Charset charset = Charset.forName("UTF-8");
	private Random random = MatsimRandom.getLocalInstance();
	
	private Map<Id, Id> enterpriseToBuildingMapping;	// <EnterpriseId, BuildingId>
	private Map<Id, Integer> enterpriseVAE;	// <EnterpriseId, Vollzeitaequivalente Arbeitsplaetze>

	private ActivityFacilities zhFacilities;
	private ActivityFacilities existingFacilities;
	private Config config;
	private Scenario scenario;
	
	private Map<Id, Integer> work_sector2CapacityMap;
	private Map<Id, Integer> work_sector3CapacityMap;
	private Map<Id, Integer> leisure_gastroCapacityMap;
	private Map<Id, Integer> leisure_culturalCapacityMap;
	private Map<Id, Integer> leisure_sportsCapacityMap;
	private Map<Id, Integer> education_kindergartenCapacityMap;
	private Map<Id, Integer> education_primaryCapacityMap;
	private Map<Id, Integer> education_secondaryCapacityMap;
	private Map<Id, Integer> education_otherCapacityMap;
	private Map<Id, Integer> education_higherCapacityMap;
	private Map<Id, Integer> shop_retail_gt2500sqmCapacityMap;
	private Map<Id, Integer> shop_retail_get1000sqmCapacityMap;
	private Map<Id, Integer> shop_retail_get400sqmCapacityMap;
	private Map<Id, Integer> shop_retail_get100sqmCapacityMap;
	private Map<Id, Integer> shop_retail_lt100sqmCapacityMap;
	private Map<Id, Integer> shop_otherCapacityMap;
	
	private QuadTree<Id> shop_retail_gt2500sqmCapacityQuadTree;
	private QuadTree<Id> shop_retail_get1000sqmCapacityMapQuadTree;
	private QuadTree<Id> shop_retail_get400sqmCapacityMapQuadTree;
	private QuadTree<Id> shop_retail_get100sqmCapacityMapQuadTree;
	private QuadTree<Id> shop_retail_lt100sqmCapacityMapQuadTree;
	private QuadTree<Id> shop_otherCapacityMapQuadTree;
	
	/**
	 * Capacity of third Activities (shopping, leisure, ...) in a Facility is
	 * calculated as in Meister (2008): cap3 = capacity * (10 + random(0..10))
	 * EDIT: Adapted formula for education_kindergarten (*2), education_primary (*5), education_higher (/3)
	 * 
	 * Expects 7 Strings as input parameters:
	 * - buildingsTextFile (input)
	 * - apartmentsTextFile (input)
	 * - apartmentBuildingsTextFile (output)
	 * - facilitiesZHFile (output)
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 7) return;
		
		String enterpriseToBuildingTextFile = args[0];
		String enterpriseCensusVAETextFile = args[1];
		String enterpriseCensusTextFile = args[2];
		String inFacilitiesZHFile = args[3];
		String existingFacilitiesFile = args[4];
		String outFacilitiesZHFile = args[5];
		String outEmptyFacilitiesZHFile = args[6];
		
		EnterpriseFacilitiesCreator creator = new EnterpriseFacilitiesCreator(inFacilitiesZHFile, existingFacilitiesFile);
		creator.createShopQuadTrees();
		creator.parseEnterpriseToBuildingTextFile(enterpriseToBuildingTextFile);
		creator.parseEnterpriseCensusVAETextFile(enterpriseCensusVAETextFile);
		creator.parseEnterpriseTextFile(enterpriseCensusTextFile);
		creator.setOpeningTimes();
		creator.analyseFacilites();
		creator.writeEmptyFacilitiesFile(outEmptyFacilitiesZHFile);
		creator.writeFacilitiesFile(outFacilitiesZHFile);
	}
	
	public EnterpriseFacilitiesCreator(String inFacilitiesZHFile, String existingFacilitiesFile) throws Exception {
		config = ConfigUtils.createConfig();
		config.facilities().setInputFile(inFacilitiesZHFile);
		scenario = ScenarioUtils.loadScenario(config);
		zhFacilities = ((ScenarioImpl) scenario).getActivityFacilities();
		
		Config existingFacilitiesConfig = ConfigUtils.createConfig(); 
		existingFacilitiesConfig.facilities().setInputFile(existingFacilitiesFile);
		Scenario existingFacilitiesScenario = ScenarioUtils.loadScenario(existingFacilitiesConfig);
		existingFacilities = ((ScenarioImpl) existingFacilitiesScenario).getActivityFacilities();
	}
	
	public EnterpriseFacilitiesCreator(ActivityFacilities zhFacilities, ActivityFacilities existingFacilities) throws Exception {
		config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		
		this.zhFacilities = zhFacilities;
		this.existingFacilities = existingFacilities;
	}
	
	/*
	 * Read the existing MATSim Switzerland facilities file. Create quad trees
	 * for the shopping facilities. We use them to copy their opening times.
	 */
	public void createShopQuadTrees() {	
		log.info("building shop quad trees...");
		
		shop_retail_gt2500sqmCapacityQuadTree = createQuadTree("shop_retail_gt2500sqm");
		shop_retail_get1000sqmCapacityMapQuadTree = createQuadTree("shop_retail_get1000sqm");
		shop_retail_get400sqmCapacityMapQuadTree = createQuadTree("shop_retail_get400sqm");
		shop_retail_get100sqmCapacityMapQuadTree = createQuadTree("shop_retail_get100sqm");
		shop_retail_lt100sqmCapacityMapQuadTree = createQuadTree("shop_retail_lt100sqm");
		shop_otherCapacityMapQuadTree = createQuadTree("shop_other");
		
		log.info("done.");
	}
	
	private QuadTree<Id> createQuadTree(String shopType) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (ActivityFacility facility : existingFacilities.getFacilities().values()) {
			/*
			 * The shop type is coded as String in the description of the facility.
			 */
			if (!((ActivityFacilityImpl) facility).getDesc().contains(shopType)) continue;
			
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info(shopType + " xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				
		QuadTree<Id> quadTree = new QuadTree<Id>(minx, miny, maxx, maxy);
		
		log.info("filling facilities quad tree...");
		for (ActivityFacility facility : existingFacilities.getFacilities().values()) {
			if (!((ActivityFacilityImpl) facility).getDesc().contains(shopType)) continue;
			
			quadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
		}
		
		return quadTree;
	}
	
	public void parseEnterpriseToBuildingTextFile(String enterpriseToBuildingTextFile) throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	
		fis = new FileInputStream(enterpriseToBuildingTextFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		Counter counter = new Counter("parsed enterprises to building mapping in ZH: ");
		enterpriseToBuildingMapping = new HashMap<Id, Id>();
		
		// skip first Line with the Header
		br.readLine();
				
		String line;
		while((line = br.readLine()) != null) {
			String[] cols = line.split(delimiter2);
			
			Id enterpriseId = scenario.createId(cols[1]);
			Id buildingId = scenario.createId("egid" + (int)Double.parseDouble(cols[cols.length - 1]));
			
			if ((int)Double.parseDouble(cols[cols.length - 1]) == 0) {
				log.warn("Invalid enterprise to building mapping - skipp entry: " + line);
				continue;
			}
			
			enterpriseToBuildingMapping.put(enterpriseId, buildingId);
			counter.incCounter();
		}
		
		counter.printCounter();
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public void parseEnterpriseCensusVAETextFile(String enterpriseCensusVAETextFile) throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
	
		fis = new FileInputStream(enterpriseCensusVAETextFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		Counter counter = new Counter("parsed enterprise capacities in ZH: ");
		enterpriseVAE = new HashMap<Id, Integer>();
		
		// skip first Line with the Header
		br.readLine();
				
		String line;
		while((line = br.readLine()) != null) {
			String[] cols = line.split(delimiter2);
			
			Id enterpriseId = scenario.createId(cols[0]);
			int vae = Integer.parseInt(cols[1]);
			
			enterpriseVAE.put(enterpriseId, vae);
			counter.incCounter();
		}
		
		counter.printCounter();
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public void parseEnterpriseTextFile(String enterpriseCensusTextFile) throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(enterpriseCensusTextFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		Counter counter = new Counter("parsed enterprises in ZH: ");
				
		int work_sector2Capacity = 0;
		int work_sector3Capacity = 0;
		int leisure_gastroCapacity = 0;
		int leisure_culturalCapacity = 0;
		int leisure_sportsCapacity = 0;
		int shop_retail_gt2500sqmCapacity = 0;
		int shop_retail_get1000sqmCapacity = 0;
		int shop_retail_get400sqmCapacity = 0;
		int shop_retail_get100sqmCapacity = 0;
		int shop_retail_lt100sqmCapacity = 0;
		int shop_otherCapacity = 0;
		int education_primaryCapacity = 0;
		int education_secondaryCapacity = 0;
		int education_kindergartenCapacity = 0;
		int education_higherCapacity = 0;
		int education_otherCapacity = 0;
		
		int work_sector2Facilities = 0;
		int work_sector3Facilities = 0;
		int leisure_gastroFacilities = 0;
		int leisure_culturalFacilities = 0;
		int leisure_sportsFacilities = 0;
		int shop_retail_gt2500sqmFacilities = 0;
		int shop_retail_get1000sqmFacilities = 0;
		int shop_retail_get400sqmFacilities = 0;
		int shop_retail_get100sqmFacilities = 0;
		int shop_retail_lt100sqmFacilities = 0;
		int shop_otherFacilities = 0;
		int education_primaryFacilities = 0;
		int education_secondaryFacilities = 0;
		int education_kindergartenFacilities = 0;
		int education_higherFacilities = 0;
		int education_otherFacilities = 0;
		
		work_sector2CapacityMap = new HashMap<Id, Integer>();
		work_sector3CapacityMap = new HashMap<Id, Integer>();
		leisure_gastroCapacityMap = new HashMap<Id, Integer>();
		leisure_culturalCapacityMap = new HashMap<Id, Integer>();
		leisure_sportsCapacityMap = new HashMap<Id, Integer>();
		education_kindergartenCapacityMap = new HashMap<Id, Integer>();
		education_primaryCapacityMap = new HashMap<Id, Integer>();
		education_secondaryCapacityMap = new HashMap<Id, Integer>();
		education_otherCapacityMap = new HashMap<Id, Integer>();
		education_higherCapacityMap = new HashMap<Id, Integer>();
		shop_retail_gt2500sqmCapacityMap = new HashMap<Id, Integer>();
		shop_retail_get1000sqmCapacityMap = new HashMap<Id, Integer>();
		shop_retail_get400sqmCapacityMap = new HashMap<Id, Integer>();
		shop_retail_get100sqmCapacityMap = new HashMap<Id, Integer>();
		shop_retail_lt100sqmCapacityMap = new HashMap<Id, Integer>();
		shop_otherCapacityMap = new HashMap<Id, Integer>();
		
		for (Facility facility : zhFacilities.getFacilities().values()) {
			work_sector2CapacityMap.put(facility.getId(), 0);
			work_sector3CapacityMap.put(facility.getId(), 0);
			leisure_gastroCapacityMap.put(facility.getId(), 0);
			leisure_culturalCapacityMap.put(facility.getId(), 0);
			leisure_sportsCapacityMap.put(facility.getId(), 0);
			education_kindergartenCapacityMap.put(facility.getId(), 0);
			education_primaryCapacityMap.put(facility.getId(), 0);
			education_secondaryCapacityMap.put(facility.getId(), 0);
			education_otherCapacityMap.put(facility.getId(), 0);
			education_higherCapacityMap.put(facility.getId(), 0);
			shop_retail_gt2500sqmCapacityMap.put(facility.getId(), 0);
			shop_retail_get1000sqmCapacityMap.put(facility.getId(), 0);
			shop_retail_get400sqmCapacityMap.put(facility.getId(), 0);
			shop_retail_get100sqmCapacityMap.put(facility.getId(), 0);
			shop_retail_lt100sqmCapacityMap.put(facility.getId(), 0);
			shop_otherCapacityMap.put(facility.getId(), 0);
		}
		
		// skip first Line with the Header
		br.readLine();
		
		String line;
		while((line = br.readLine()) != null) {
			String[] cols = line.split(delimiter2);
			
			// Skip the enterprise, if it is not located within Zurich city.
			if (!cols[22].equalsIgnoreCase(muncipalityId)) continue;
			
			/*
			 * Coding:
			 * 01..03 Land- und Forstwirtschaft, Jagd, Fischerei (Sektor 1)
			 * 04..43 Industrie (Sektor 2)
			 * 45..96 Dienstleistungen (Sektor 3)
			 */
			int nogaSection = Integer.parseInt(cols[1].replace("\"","").substring(0, 2));
			if (nogaSection <= 3) continue;
			
			EnterpriseData e = new EnterpriseData(cols);
			Id enterpriseId = scenario.createId(cols[0]);		
			Id facilityId = enterpriseToBuildingMapping.get(enterpriseId);
			
			if (facilityId == null) {
				log.warn("No mapping for enterprise Id " + enterpriseId.toString() + " found. Skip entry!");
				continue;
			}
						
			if (nogaSection >= 5 && nogaSection <= 43) {

				// get enterprise capacity
				int capacity = enterpriseVAE.get(enterpriseId);
				work_sector2Capacity += capacity;
				if (capacity == 0) log.warn("Capacity of 0 was found for enterprise census entry " + enterpriseId);
				
				int oldCapacity = work_sector2CapacityMap.get(facilityId);
				if (oldCapacity == 0) work_sector2Facilities++;
				work_sector2CapacityMap.put(facilityId, oldCapacity + capacity);
				
			} else if (nogaSection >= 45 && nogaSection <= 96) {

				// get enterprise capacity
				int capacity = enterpriseVAE.get(enterpriseId);
				work_sector3Capacity += capacity;
				if (capacity == 0) log.warn("Capacity of 0 was found for enterprise census entry " + enterpriseId);

				int oldCapacity = work_sector3CapacityMap.get(facilityId);
				if (oldCapacity == 0) work_sector3Facilities++;
				work_sector3CapacityMap.put(facilityId, oldCapacity + capacity);
				
				int cap = capacity * (10 + random.nextInt(10));
				
				if (NOGATypes.leisure_cultureNOGAs.contains(e.noga)) {
					leisure_culturalFacilities++;
					leisure_culturalCapacity += cap;
					leisure_culturalCapacityMap.put(facilityId, leisure_culturalCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.leisure_gastroNOGAs.contains(e.noga)) {
					leisure_gastroFacilities++;
					leisure_gastroCapacity += cap;
					leisure_gastroCapacityMap.put(facilityId, leisure_gastroCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.leisure_sportsNOGAs.contains(e.noga)) {
					leisure_sportsFacilities++;
					leisure_sportsCapacity += cap;
					leisure_sportsCapacityMap.put(facilityId, leisure_sportsCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.education_higherNOGAs.contains(e.noga)) {
					// adapt capacity
					cap = cap / 3;
					if (cap == 0) cap = 1;
					education_higherFacilities++;
					education_higherCapacity += cap;
					education_higherCapacityMap.put(facilityId, education_higherCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.education_kindergartenNOGAs.contains(e.noga)) {
					// adapt capacity
					cap = cap * 2;
					education_kindergartenFacilities++;
					education_kindergartenCapacity += cap;
					education_kindergartenCapacityMap.put(facilityId, education_kindergartenCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.education_primaryNOGAs.contains(e.noga)) {
					// adapt capacity
					cap = cap * 5;
					education_primaryFacilities++;
					education_primaryCapacity += cap;
					education_primaryCapacityMap.put(facilityId, education_primaryCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.education_secondaryNOGAs.contains(e.noga)) {
					education_secondaryFacilities++;
					education_secondaryCapacity += cap;
					education_secondaryCapacityMap.put(facilityId, education_secondaryCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.education_otherNOGAs.contains(e.noga)) {
					education_otherFacilities++;
					education_otherCapacity += cap;
					education_otherCapacityMap.put(facilityId, education_otherCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_otherNOGAs.contains(e.noga)) {
					shop_otherFacilities++;
					shop_otherCapacity += cap;
					shop_otherCapacityMap.put(facilityId, shop_otherCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_retail_lt100sqmNOGAs.contains(e.noga)) {
					shop_retail_lt100sqmFacilities++;
					shop_retail_lt100sqmCapacity += cap;
					shop_retail_lt100sqmCapacityMap.put(facilityId, shop_retail_lt100sqmCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_retail_get100sqmNOGAs.contains(e.noga)) {
					shop_retail_get100sqmFacilities++;
					shop_retail_get100sqmCapacity += cap;
					shop_retail_get100sqmCapacityMap.put(facilityId, shop_retail_get100sqmCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_retail_get400sqmNOGAs.contains(e.noga)) {
					shop_retail_get400sqmFacilities++;
					shop_retail_get400sqmCapacity += cap;
					shop_retail_get400sqmCapacityMap.put(facilityId, shop_retail_get400sqmCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_retail_get1000sqmNOGAs.contains(e.noga)) {
					shop_retail_get1000sqmFacilities++;
					shop_retail_get1000sqmCapacity += cap;
					shop_retail_get1000sqmCapacityMap.put(facilityId, shop_retail_get1000sqmCapacityMap.get(facilityId) + cap);
				} else if (NOGATypes.shop_retail_gt2500sqmNOGAs.contains(e.noga)) {
					shop_retail_gt2500sqmFacilities++;
					shop_retail_gt2500sqmCapacity += cap;
					shop_retail_gt2500sqmCapacityMap.put(facilityId, shop_retail_gt2500sqmCapacityMap.get(facilityId) + cap);
				}
			} else log.error("Unknown NOGA section: " + nogaSection);
			
			counter.incCounter();
		}
		counter.printCounter();

		log.info("---------------------------------------------------------");
		log.info("Created work sector 2 capacity " + work_sector2Capacity);
		log.info("Created work sector 3 capacity " + work_sector3Capacity);
		log.info("Created leisure cultural capacity " + leisure_culturalCapacity);
		log.info("Created leisure gastro capacity " + leisure_gastroCapacity);
		log.info("Created leisure sports capacity " + leisure_sportsCapacity);
		log.info("Created shop retail > 2500 m2 capacity " + shop_retail_gt2500sqmCapacity);
		log.info("Created shop retail >= 1000 m2 capacity " + shop_retail_get1000sqmCapacity);
		log.info("Created shop retail >= 400 m2 capacity " + shop_retail_get400sqmCapacity);
		log.info("Created shop retail >= 100 m2 capacity " + shop_retail_get100sqmCapacity);
		log.info("Created shop retail < 100 m2 capacity " + shop_retail_lt100sqmCapacity);
		log.info("Created shop other capacity " + shop_otherCapacity);
		log.info("Created education primary capacity " + education_primaryCapacity);
		log.info("Created education secondary capacity " + education_secondaryCapacity);
		log.info("Created education kindergarten capacity " + education_kindergartenCapacity);
		log.info("Created education higher capacity " + education_higherCapacity);
		log.info("Created education other capacity " + education_otherCapacity);
		log.info("---------------------------------------------------------");
		log.info("Created work sector 2 facilities " + work_sector2Facilities);
		log.info("Created work sector 3 facilities " + work_sector3Facilities);
		log.info("Created leisure cultural facilities " + leisure_culturalFacilities);
		log.info("Created leisure gastro facilities " + leisure_gastroFacilities);
		log.info("Created leisure sports facilities " + leisure_sportsFacilities);
		log.info("Created shop retail > 2500 m2 facilities " + shop_retail_gt2500sqmFacilities);
		log.info("Created shop retail >= 1000 m2 facilities " + shop_retail_get1000sqmFacilities);
		log.info("Created shop retail >= 400 m2 facilities " + shop_retail_get400sqmFacilities);
		log.info("Created shop retail >= 100 m2 facilities " + shop_retail_get100sqmFacilities);
		log.info("Created shop retail < 100 m2 facilities " + shop_retail_lt100sqmFacilities);
		log.info("Created shop other facilities " + shop_otherFacilities);
		log.info("Created education primary facilities " + education_primaryFacilities);
		log.info("Created education secondary facilities " + education_secondaryFacilities);
		log.info("Created education kindergarten facilities " + education_kindergartenFacilities);
		log.info("Created education higher facilities " + education_higherFacilities);
		log.info("Created education other facilities " + education_otherFacilities);
		log.info("---------------------------------------------------------");
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public void setOpeningTimes() {
		log.info("Setting opening times...");
				
		for (Facility facility : zhFacilities.getFacilities().values()) {
			
			int work_sector2Capacity = work_sector2CapacityMap.get(facility.getId());
			int work_sector3Capacity = work_sector3CapacityMap.get(facility.getId());
			int leisure_gastroCapacity = leisure_gastroCapacityMap.get(facility.getId());
			int leisure_culturalCapacity = leisure_culturalCapacityMap.get(facility.getId());
			int leisure_sportsCapacity = leisure_sportsCapacityMap.get(facility.getId());
			int shop_retail_gt2500sqmCapacity = shop_retail_gt2500sqmCapacityMap.get(facility.getId());
			int shop_retail_get1000sqmCapacity = shop_retail_get1000sqmCapacityMap.get(facility.getId());
			int shop_retail_get400sqmCapacity = shop_retail_get400sqmCapacityMap.get(facility.getId());
			int shop_retail_get100sqmCapacity = shop_retail_get100sqmCapacityMap.get(facility.getId());
			int shop_retail_lt100sqmCapacity = shop_retail_lt100sqmCapacityMap.get(facility.getId());
			int shop_otherCapacity = shop_otherCapacityMap.get(facility.getId());
			int education_primaryCapacity = education_primaryCapacityMap.get(facility.getId());
			int education_secondaryCapacity = education_secondaryCapacityMap.get(facility.getId());
			int education_kindergartenCapacity = education_kindergartenCapacityMap.get(facility.getId());
			int education_higherCapacity = education_higherCapacityMap.get(facility.getId());
			int education_otherCapacity = education_otherCapacityMap.get(facility.getId());
	
			if (work_sector2Capacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("work_sector2");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 7.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 7.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 7.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 7.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 7.0 * 3600.0, 18.0 * 3600));
				activityOption.setCapacity(work_sector2Capacity * 1.0);
			}
			
			/*
			 * We create an ActivityOption for work_sector3. Its opening time depends on the other activity types
			 * that can be performed in the same facility. We merge the opening times of all those activities to
			 * get the opening time for the work_sector3 ActivityOption (Merging is done automatically within
			 * the ActivityOptionImpl class). 
			 */
			ActivityOptionImpl work_sector3ActivityOption = null;
			if (work_sector3Capacity > 0) {
				work_sector3ActivityOption = ((ActivityFacilityImpl) facility).createActivityOption("work_sector3");
				work_sector3ActivityOption.setCapacity(work_sector3Capacity * 1.0);
			}
			
			if (shop_retail_gt2500sqmCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_retail_gt2500sqmCapacity);

				Id nearestShopId = shop_retail_gt2500sqmCapacityQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}			
			}
			if (shop_retail_get1000sqmCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_retail_get1000sqmCapacity);
				
				Id nearestShopId = shop_retail_get1000sqmCapacityMapQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}
			}
			if (shop_retail_get400sqmCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_retail_get400sqmCapacity);
				
				Id nearestShopId = shop_retail_get400sqmCapacityMapQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}
			}
			if (shop_retail_get100sqmCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_retail_get100sqmCapacity);
				
				Id nearestShopId = shop_retail_get100sqmCapacityMapQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}
			}
			if (shop_retail_lt100sqmCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_retail_lt100sqmCapacity);
				
				Id nearestShopId = shop_retail_lt100sqmCapacityMapQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}
			}
			if (shop_otherCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("shop");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("shop");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + shop_otherCapacity);
				
				Id nearestShopId = shop_otherCapacityMapQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY());
				ActivityFacility nearestShopFacility = existingFacilities.getFacilities().get(nearestShopId);
				
				ActivityOptionImpl nearestShopActivityOption = (ActivityOptionImpl) nearestShopFacility.getActivityOptions().get("shop");
				for (Collection<OpeningTime> openingTimes : nearestShopActivityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) activityOption.addOpeningTime(openingTime);
				}
			}
			if (leisure_gastroCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("leisure");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + leisure_gastroCapacity);
				
//				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure_gastro");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sat, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sun, 9.0 * 3600.0, 24.0 * 3600));
				activityOption.setCapacity(leisure_gastroCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}
			}
			if (leisure_culturalCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("leisure");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + leisure_culturalCapacity);
				
//				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure_cultural");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sat, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sun, 14.0 * 3600.0, 24.0 * 3600));
				activityOption.setCapacity(leisure_culturalCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}
			}
			if (leisure_sportsCapacity > 0) {
				ActivityOptionImpl activityOption = (ActivityOptionImpl) ((ActivityFacilityImpl) facility).getActivityOptions().get("leisure");
				if (activityOption == null) {
					activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure");
					activityOption.setCapacity(0.0);
				}
				activityOption.setCapacity(activityOption.getCapacity() + leisure_sportsCapacity);
				
//				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("leisure_sports");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 9.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 9.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 9.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 9.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 9.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sat, 9.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sun, 9.0 * 3600.0, 18.0 * 3600));
				activityOption.setCapacity(leisure_sportsCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}
			}
			if (education_primaryCapacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("education_primary");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.setCapacity(education_primaryCapacity * 1.0);
				
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 17.0 * 3600));
			}
			if (education_secondaryCapacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("education_secondary");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.setCapacity(education_secondaryCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}
			}
			if (education_kindergartenCapacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("education_kindergarten");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 13.5 * 3600.0, 17.0 * 3600));
				activityOption.setCapacity(education_kindergartenCapacity * 1.0);
				
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 17.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 17.0 * 3600));
			}
			if (education_higherCapacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("education_higher");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 7.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 7.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 7.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 7.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 7.0 * 3600.0, 22.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.sat, 8.0 * 3600.0, 12.0 * 3600));
				activityOption.setCapacity(education_higherCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}				
			}
			if (education_otherCapacity > 0) {
				ActivityOptionImpl activityOption = ((ActivityFacilityImpl) facility).createActivityOption("education_other");
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 8.0 * 3600.0, 18.0 * 3600));
				activityOption.setCapacity(education_otherCapacity * 1.0);
				
				for (Collection<OpeningTime> openingTimes : activityOption.getOpeningTimes().values()) {
					for (OpeningTime openingTime : openingTimes) work_sector3ActivityOption.addOpeningTime(openingTime);
				}
			}
			
			/*
			 * If the facility has a work_sector3 Activity Option but no other options which would define
			 * the opening times, we use 7:00 to 18:00 from Monday to Saturday. 
			 */
			if (work_sector3ActivityOption != null && work_sector3ActivityOption.getOpeningTimes().size() == 0) {
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.mon, 7.0 * 3600.0, 18.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.tue, 7.0 * 3600.0, 18.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.wed, 7.0 * 3600.0, 18.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.thu, 7.0 * 3600.0, 18.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.fri, 7.0 * 3600.0, 18.0 * 3600));
				work_sector3ActivityOption.addOpeningTime(new OpeningTimeImpl(DayType.sat, 7.0 * 3600.0, 18.0 * 3600));
			}
		}

		log.info("done.");
	}
	
	public void analyseFacilites() {
		log.info("Setting opening times...");
		
		Counter counter = new Counter("Facilities without activity option ");
		for (Facility facility : zhFacilities.getFacilities().values()) {
			if (((ActivityFacilityImpl) facility).getActivityOptions().size() == 0) counter.incCounter();
		}
		counter.printCounter();
		
		log.info("done.");
	}
	
	public void writeEmptyFacilitiesFile(String outEmptyFacilitiesZHFile) throws Exception {
		log.info("Writing empty facilities to file...");
		
		FileOutputStream fos = new FileOutputStream(outEmptyFacilitiesZHFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
		BufferedWriter bw = new BufferedWriter(osw);
		Counter counter = new Counter("Write empty buildings to file ");
		
		// write Header
		bw.write("egid" + delimiter + "x" + delimiter + "y" + "\n");

		for (Facility facility : zhFacilities.getFacilities().values()) {
			if (((ActivityFacilityImpl) facility).getActivityOptions().size() == 0) {
				StringBuffer sb = new StringBuffer();
				sb.append(facility.getId().toString().replace("egid", ""));
				sb.append(delimiter);
				sb.append(facility.getCoord().getX());
				sb.append(delimiter);
				sb.append(facility.getCoord().getY());
				sb.append("\n");
				
				bw.write(sb.toString());
				counter.incCounter();
			}
		}
		counter.printCounter();
		
		bw.close();
		osw.close();
		fos.close();
		
		log.info("done.");
	}
	
	public void writeFacilitiesFile(String outFacilitiesZHFile) {
		log.info("Setting opening times...");
		new FacilitiesWriter((ActivityFacilitiesImpl) zhFacilities).write(outFacilitiesZHFile);
		log.info("done.");
	}
	
	private static class EnterpriseData {	
		/*
		 * Structure of the data set:
		 * [0]	"EXTERN_LOCAL_ID"
		 * [1]	"NOGA_CD_2008"
		 * [2]	"LONG_NAME_DE"
		 * [3]	"EMPL_FULL_NOT_SWISS_WOMEN_NB"
		 * [4]	"EMPL_FULL_NB"
		 * [5]	"EMPL_PART1_SWISS_MEN_NB"
		 * [6]	"EMPL_PART1_SWISS_WOMEN_NB"
		 * [7]	"EMPL_PART1_NOT_SWISS_MEN_NB"
		 * [8]	"EMPL_PART1_NOT_SWISS_WOMEN_NB"
		 * [9]	"EMPL_PART1_NB"
		 * [10]	"EMPL_PART2_SWISS_MEN_NB"
		 * [11]	"EMPL_PART2_SWISS_WOMEN_NB"
		 * [12] "EMPL_PART2_NOT_SWISS_MEN_NB"
		 * [13]	"EMPL_PART2_NOT_SWISS_WOMEN_NB"
		 * [14]	"EMPL_PART2_NB"
		 * [15]	"EMPL_TOTAL_NB"
		 * [16]	"EMPL_APPRENTICE_NB"
		 * [17]	"BORDER_CROSSER_NB"
		 * [18]	"BORDER_CROSSER_MEN_NB"
		 * [19]	"BORDER_CROSSER_WOMEN_NB"
		 * [20]	"EMPLOY_SIZE_CLASS_CD_EPT_2008"
		 * [21]	"KIND_LOCAL_UNIT_CD"
		 * [22]	"MUNICIPALITY_CD"
		 * [23]	"METER_X"
		 * [24] "METER_Y"
		 * [25]	"MARKET_CD_2008"
		 * [26]	"PUBLIC_OR_PRIVATE_CD_2008"
		 * [27]	"AREASREGIONALPLANNING_CD"
		 */
		
		/*package*/ int id;
		/*package*/ int noga;
		/*package*/ int x;
		/*package*/ int y;
		
		public EnterpriseData(String[] cols) {
			id = parseInteger(cols[0]);
			noga = parseInteger(cols[1]);
			x = parseInteger(cols[23]);
			y = parseInteger(cols[24]);
		}
		
		private int parseInteger(String string) {
			if (string == null) return 0;
			string = string.replace("\"", "");
			if (string.trim().equals("")) return 0;
			else return Integer.valueOf(string);
		}
	}
}
