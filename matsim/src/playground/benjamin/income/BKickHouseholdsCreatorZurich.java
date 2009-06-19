/* *********************************************************************** *
 * project: org.matsim.*
 * BKickHouseholdsCreator
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
package playground.benjamin.income;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.households.BasicHousehold;
import org.matsim.core.basic.v01.households.BasicHouseholdBuilder;
import org.matsim.core.basic.v01.households.BasicHouseholds;
import org.matsim.core.basic.v01.households.BasicIncome;
import org.matsim.core.basic.v01.households.HouseholdsWriterV1;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class BKickHouseholdsCreatorZurich {
	
	private static final Logger log = Logger.getLogger(BKickHouseholdsCreatorZurich.class);
	
	public BKickHouseholdsCreatorZurich() throws IOException{
		/*
		 * This file has the following attributes:
		 * the_geom
     * GMDEQNR
     * NR
     * NAME
     * GMDE
     * FLAECHE_HA
		 */
		String quartiereZurichShapeFile = DgPaths.WORKBASE + "externedaten/Schweiz/Gemeindegrenzen/quartiergrenzen2006/quart06_shp_070824/quart06.shp";
		
		String gemeindenKantonZurichShapeFile = DgPaths.WORKBASE + "externedaten/Schweiz/Gemeindegrenzen/gemeindegrenzen2008/g1g08_shp_080606/G1G08.shp";
		
		String plansZurichWoTransit = DgPaths.IVTCHBASE + "baseCase/plans/plans_all_zrh30km_10pct.xml.gz";
		
		String einkommenZurichTextfile = DgPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/einkommenKantonZurichPlainDataEditedFinalUTF8.txt";

		String householdsXmlFile = DgPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_10pct.xml.gz";
		
		String householdsTxtFile = DgPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_10pct.txt";
		
		String plansZurich = plansZurichWoTransit;
		

		Map<String, Double> gemeindeIncome = this.readMedianIncomeFile(einkommenZurichTextfile);
		FeatureSource fts = ShapeFileReader.readDataFile(gemeindenKantonZurichShapeFile);

		
		Scenario scenario = new ScenarioImpl();
		scenario.getConfig().network().setInputFile(DgPaths.IVTCHNET);
		scenario.getConfig().plans().setInputFile(plansZurich);
		ScenarioLoader loader = new ScenarioLoader(scenario);
		loader.loadScenario();
		
		BasicHouseholds households = ((ScenarioImpl) scenario).getHouseholds();
		
		
		IncomeCalculatorGesamtschweiz incomeCalcSchweiz = new IncomeCalculatorGesamtschweiz();
		IncomeCalculatorKantonZurich incomeCalcZurichKanton = new IncomeCalculatorKantonZurich();
		
		int personCounter = 0;
		
		BufferedWriter hhTxtWriter = IOUtils.getBufferedWriter(householdsTxtFile);
		hhTxtWriter.write("personId\thomeX\thomeY\tincomePerYear");
		hhTxtWriter.newLine();
		
		BasicPopulation<BasicPerson<BasicPlan>> pop = (BasicPopulation) scenario.getPopulation();
		for (BasicPerson<BasicPlan> p : pop.getPersons().values()){
			//create the households
	    BasicHouseholdBuilder b = households.getHouseholdBuilder();
	    BasicHousehold hh = b.createHousehold(p.getId());
	    hh.getMemberIds().add(p.getId());
	    households.getHouseholds().put(p.getId(), hh);

	    //calculate the income
	    Feature feature = this.getGemeindeFeatureForPerson(p, fts);
	    double income;
	    if (feature == null) {
	    	income = incomeCalcSchweiz.calculateIncome(43665);
	    }
	    else {
	    	String gemeindeNameFeature = (String) feature.getAttribute("NAME");
	    	gemeindeNameFeature = gemeindeNameFeature.trim().split(" ")[0];
	    	if (gemeindeIncome.containsKey(gemeindeNameFeature)){
	    		income = incomeCalcZurichKanton.calculateIncome(gemeindeIncome.get(gemeindeNameFeature));
	    	}
	    	else {
	    		income = incomeCalcSchweiz.calculateIncome(43665);
	    	}
	    }
	    hh.setIncome(b.createIncome(income, BasicIncome.IncomePeriod.year));
	    hh.getIncome().setCurrency("SFr");
	   
	    
	    StringBuffer sb = new StringBuffer();
	    sb.append(p.getId());
	    sb.append("\t");
	    Coord coord = ((BasicActivity)p.getPlans().get(0).getPlanElements().get(0)).getCoord();
	    sb.append(coord.getX());
	    sb.append("\t");
	    sb.append(coord.getY());
	    sb.append("\t");
	    sb.append(income);
	    hhTxtWriter.write(sb.toString());
	    hhTxtWriter.newLine();
	    
	    personCounter++;
	    if (personCounter % 100 == 0){
	    	log.info("processed " + personCounter + " persons...");
	    }
		}
		hhTxtWriter.flush();
		hhTxtWriter.close();
		HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(households);
    hhwriter.writeFile(householdsXmlFile);
		System.out.println("Households written!");
		
		
		//######################
		
//		checkGemeindenForPop(scenario.getPopulation(), fts);
		
//		findFeatureForGemeinde(gemeindeIncome, createFeaturesByName(fts));
	}

	private Map<String, Double> readMedianIncomeFile(String filename) throws FileNotFoundException, IOException{
		Map<String, Double> gemeindeIncome = new HashMap<String, Double>();
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine();	
		while (line != null) {
//			System.out.println(line);
			String[] columns = line.split("\t");
			int incomeIndex = 1;
			gemeindeIncome.put(columns[0].trim().split(" ")[0], Double.parseDouble(columns[incomeIndex].replace(",", ".")) * 1000);
//			System.out.println(columns[0] + " " + gemeindeIncome.get(columns[0]));
			line = reader.readLine();
		}
		return gemeindeIncome;
	}
	

	private Map<String, Feature> createFeaturesByName(FeatureSource fts) throws IOException {
		Map<String, Feature> featuresByName = new HashMap<String, Feature>();
		//Iterator to iterate over the features from the shape file
		Iterator<Feature> it = fts.getFeatures().iterator();
		System.out.println("Found feature for Gemeinden: " );
		int ii = 0;
		while (it.hasNext()) {
			Feature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
			//of other attributes
			String ftname = (String) ft.getAttribute("NAME");
			if (ii < 5){
				System.out.print(ftname + "\t");
				ii++;
			}
			else {
				System.out.println();
				ii = 0;
			}
			featuresByName.put(ftname.trim().split(" ")[0], ft);
		}
		return featuresByName;
	}
	
	
	
	
	
	private void checkGemeindenForPop(Population population, FeatureSource fsource) throws IOException {
		Plan plan;
		int personsWithGemeinde = 0;
		
		for (Person p : population.getPersons().values()) {
			plan = p.getPlans().get(0);
			if (plan == null) {
				throw new IllegalStateException("Person " + p.getId() + " has no plans");
			}
			Coord coord = plan.getFirstActivity().getCoord();
			Iterator<Feature> it = fsource.getFeatures().iterator();
			Feature f = null;
			while (it.hasNext()) {
				f = it.next();
				if (f.getDefaultGeometry().contains(MGC.coord2Point(coord))){
					personsWithGemeinde++;
					break;
				}
				f = null;
			}
			if (f == null) {
				log.error("No Quartier found for person " + p.getId());
			}
		}//end for persons
		log.info("Persons with Gemeinde: " + personsWithGemeinde + " Population total: " + population.getPersons().size());
	}

	
	private Feature getGemeindeFeatureForPerson(BasicPerson<BasicPlan> person, FeatureSource fsource) throws IOException {
		BasicPlan plan;
		plan = person.getPlans().get(0);
		if (plan == null) {
			throw new IllegalStateException("Person " + person.getId() + " has no plans");
		}
		Coord coord = ((BasicActivity)plan.getPlanElements().get(0)).getCoord();
		Iterator<Feature> it = fsource.getFeatures().iterator();
		Feature f = null;
		while (it.hasNext()) {
			f = it.next();
			if (f.getDefaultGeometry().contains(MGC.coord2Point(coord))){
				return f;
			}
			f = null;
		}
		if (f == null) {
			log.error("No Feature found for person " + person.getId());
		}
		return null;
	}
	
	private static void findFeatureForGemeinde(Map<String, Double> gemeindeIncome, Map<String, Feature> featuresByName){
		int gemeindeNotFound = 0;
		for (Entry<String, Double> entry : gemeindeIncome.entrySet()){
//			System.out.println("Gemeinde: " + entry.getKey());
			String gemeindeName = entry.getKey();
			Feature ft = featuresByName.get(gemeindeName);
			if (ft == null) {
				String searchName = null;
//				if (gemeindeName.indexOf("ü") != -1) {
//					searchName = gemeindeName.replace("ü", "");
//					searchName = searchName.concat(" (ZH)");
//				}
				

				if (searchName != null) {
					System.out.println("Trying " + searchName + " instead of " + gemeindeName);
					ft = featuresByName.get(searchName);

				}
				
				if (ft == null) {
					gemeindeNotFound++;
					System.err.println("Can not find feature for " + entry.getKey());
				}
				else {
					System.out.println("Found feature with searchName " + searchName);
				}
			}
		}
		System.err.println("Gemeinden nicht gefunden: " + gemeindeNotFound);
		System.err.println("Gemeinden mit Einkommen: "+ gemeindeIncome.size());
	}

	
	public static void main(String[] args) throws IOException {
		new BKickHouseholdsCreatorZurich();
	}
}
