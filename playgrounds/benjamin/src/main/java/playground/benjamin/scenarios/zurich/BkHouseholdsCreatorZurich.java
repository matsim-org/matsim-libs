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
package playground.benjamin.scenarios.zurich;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.benjamin.BkPaths;


/**
 * @author dgrether
 *
 */
public class BkHouseholdsCreatorZurich {

	private static final Logger log = Logger.getLogger(BkHouseholdsCreatorZurich.class);

	public BkHouseholdsCreatorZurich() throws IOException{
		/*
		 * This file has the following attributes:
		 * the_geom
     * GMDEQNR
     * NR
     * NAME
     * GMDE
     * FLAECHE_HA
		 */
		String quartiereZurichShapeFile = BkPaths.DATA + "fgvsp01/externedaten/Schweiz/Gemeindegrenzen/quartiergrenzen2006/quart06_shp_070824/quart06.shp";

		String gemeindenKantonZurichShapeFile = BkPaths.DATA + "fgvsp01/externedaten/Schweiz/Gemeindegrenzen/gemeindegrenzen2008/g1g08_shp_080606/G1G08.shp";

		String plansZurichWoTransit = BkPaths.IVTCHBASE + "baseCase/plans/plans_all_zrh30km_10pct.xml.gz";

		String plansZurichWTransit = BkPaths.IVTCHBASE + "baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";


		String einkommenZurichTextfile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/einkommenKantonZurichPlainDataEditedFinalUTF8.txt";

		String householdsWTransitXmlFile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";

		String householdsWTransitTxtFile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_transitincl_10pct.txt";

		String householdsWoTransitXmlFile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_10pct.xml.gz";

		String householdsWoTransitTxtFile = BkPaths.SHAREDSVN + "studies/dgrether/einkommenSchweiz/households_all_zrh30km_10pct.txt";


		String plansZurich = plansZurichWoTransit;

    String householdsTxtFile = householdsWoTransitTxtFile;

    String householdsXmlFile = householdsWoTransitXmlFile;


		Map<String, Double> gemeindeIncome = this.readMedianIncomeFile(einkommenZurichTextfile);
		SimpleFeatureSource fts = ShapeFileReader.readDataFile(gemeindenKantonZurichShapeFile);


		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(BkPaths.IVTCHNET);
		scenario.getConfig().plans().setInputFile(plansZurich);
		scenario.getConfig().scenario().setUseHouseholds(true);
		ScenarioUtils.loadScenario(scenario);

		Households households = (scenario).getHouseholds();


		IncomeCalculatorGesamtschweiz incomeCalcSchweiz = new IncomeCalculatorGesamtschweiz();
		IncomeCalculatorKantonZurich incomeCalcZurichKanton = new IncomeCalculatorKantonZurich();

		int personCounter = 0;

		BufferedWriter hhTxtWriter = IOUtils.getBufferedWriter(householdsTxtFile);
		hhTxtWriter.write("personId\thomeX\thomeY\tincomePerYear");
		hhTxtWriter.newLine();

		Population pop = scenario.getPopulation();
		for (Person p : pop.getPersons().values()){
			//create the households
			HouseholdsFactory b = households.getFactory();
			Household hh = b.createHousehold(Id.create(p.getId().toString(), Household.class));
	    hh.getMemberIds().add(p.getId());
	    households.getHouseholds().put(hh.getId(), hh);

	    double income;
	    // transit persons get the median income without any distribution
	    if (1000000000  <  Integer.parseInt(p.getId().toString())) {
	    	income = 43665.0;
	    }
	    // all swiss inhabitants have ids smaller than 1 000 000 000
	    else {
	    	//calculate the income
	    	SimpleFeature feature = this.getGemeindeFeatureForPerson(p, fts);
	    	if (feature == null) {
	    		income = incomeCalcSchweiz.calculateIncome(43665.0);
	    	}
	    	else {
	    		String gemeindeNameFeature = (String) feature.getAttribute("NAME");
	    		gemeindeNameFeature = gemeindeNameFeature.trim().split(" ")[0];
	    		if (gemeindeIncome.containsKey(gemeindeNameFeature)){
	    			income = incomeCalcZurichKanton.calculateIncome(gemeindeIncome.get(gemeindeNameFeature));
	    		}
	    		else  {
	    			income = incomeCalcSchweiz.calculateIncome(43665.0);
	    		}
	    	}
	    }

	    hh.setIncome(b.createIncome(income, Income.IncomePeriod.year));
	    hh.getIncome().setCurrency("SFr");


	    StringBuffer sb = new StringBuffer();
	    sb.append(p.getId());
	    sb.append("\t");
	    Coord coord = ((Activity)p.getPlans().get(0).getPlanElements().get(0)).getCoord();
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
		HouseholdsWriterV10 hhwriter = new HouseholdsWriterV10(households);
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


	private Map<String, SimpleFeature> createFeaturesByName(SimpleFeatureSource fts) throws IOException {
		Map<String, SimpleFeature> featuresByName = new HashMap<String, SimpleFeature>();
		//Iterator to iterate over the features from the shape file
		SimpleFeatureIterator it = fts.getFeatures().features();
		System.out.println("Found feature for Gemeinden: " );
		int ii = 0;
		while (it.hasNext()) {
			SimpleFeature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number
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
		it.close();
		return featuresByName;
	}





	private void checkGemeindenForPop(Population population, SimpleFeatureSource fsource) throws IOException {
		Plan plan;
		int personsWithGemeinde = 0;

		for (Person p : population.getPersons().values()) {
			plan = p.getPlans().get(0);
			if (plan == null) {
				throw new IllegalStateException("Person " + p.getId() + " has no plans");
			}
			Coord coord = PopulationUtils.getFirstActivity( ((Plan) plan) ).getCoord();
			SimpleFeatureIterator it = fsource.getFeatures().features();
			SimpleFeature f = null;
			while (it.hasNext()) {
				f = it.next();
				if (((Geometry) f.getDefaultGeometry()).contains(MGC.coord2Point(coord))){
					personsWithGemeinde++;
					break;
				}
				f = null;
			}
			it.close();
			if (f == null) {
				log.error("No Quartier found for person " + p.getId());
			}
		}//end for persons
		log.info("Persons with Gemeinde: " + personsWithGemeinde + " Population total: " + population.getPersons().size());
	}


	private SimpleFeature getGemeindeFeatureForPerson(Person person, SimpleFeatureSource fsource) throws IOException {
		Plan plan;
		plan = person.getPlans().get(0);
		if (plan == null) {
			throw new IllegalStateException("Person " + person.getId() + " has no plans");
		}
		Coord coord = ((Activity)plan.getPlanElements().get(0)).getCoord();
		SimpleFeatureIterator fIt = fsource.getFeatures().features();
		SimpleFeature f = null;
		while (fIt.hasNext()) {
			f = fIt.next();
			if (((Geometry) f.getDefaultGeometry()).contains(MGC.coord2Point(coord))){
				return f;
			}
			f = null;
		}
		fIt.close();
		if (f == null) {
			log.error("No Feature found for person " + person.getId());
		}
		return null;
	}

	private static void findFeatureForGemeinde(Map<String, Double> gemeindeIncome, Map<String, SimpleFeature> featuresByName){
		int gemeindeNotFound = 0;
		for (Entry<String, Double> entry : gemeindeIncome.entrySet()){
//			System.out.println("Gemeinde: " + entry.getKey());
			String gemeindeName = entry.getKey();
			SimpleFeature ft = featuresByName.get(gemeindeName);
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
		new BkHouseholdsCreatorZurich();
	}
}
