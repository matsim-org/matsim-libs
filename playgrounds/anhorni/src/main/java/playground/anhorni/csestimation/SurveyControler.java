/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.csestimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.QuadTree;

public class SurveyControler {
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk", ""));
	public static ArrayList<String> frequency = new ArrayList<String>(Arrays.asList("VeryOften", "Often", "OnceAWhile", "Seldom", "Never", "NULL", ""));	
	private TreeMap<Id<Person>, EstimationPerson> population = new TreeMap<Id<Person>, EstimationPerson>();
	private final static Logger log = Logger.getLogger(SurveyControler.class);
	
	public static void main(String[] args) {
		SurveyControler c = new SurveyControler();
		String personFile = args[0];
		String personShopsFile = args[1];
		String addedShopsFile = args[2];
		String shopsFile = args[3];
		String outdir = args[4];	
		c.run(personFile, personShopsFile, addedShopsFile, shopsFile, outdir);
	}
	
	public void run(String personFile, String personShopsFile, String addedShopsFile, String shopsFile, String outdir) {
		SurveyReader reader = new SurveyReader(this.population);
		reader.readDumpedPersons(personFile);
		log.info(this.population.size() + " persons created");
		reader.readDumpedPersonShops(personShopsFile);	
				
		SurveyAnalyzer analyzer = new SurveyAnalyzer(this.population, outdir);
		SurveyCleaner cleaner = new SurveyCleaner();
		cleaner.clean(population);
		analyzer.analyzeVariableBinSize();
		

		// create and analyze home sets
		UniversalChoiceSetReader ucsReader = new UniversalChoiceSetReader();
		TreeMap<Id<Location>, ShopLocation> ucs = ucsReader.readUniversalCS(shopsFile);
		QuadTree<Location> shopQuadTree = Utils.buildLocationQuadTree(ucs);
		
		for (EstimationPerson person : this.population.values()) {
			person.createHomeSet(shopQuadTree);
		}		

		// cleaned
		this.population = cleaner.removeNonAgeNonIncomePersons(this.population);
		analyzer.setPopulation(this.population);
		log.info("population size " + this.population.size());
		analyzer.analyzeVariableBinSize();		
		analyzer.analyzeHomeSets("cleaned");
		
		analyzer.setUcs(ucs);
		analyzer.analyzeArea();
		analyzer.analyzePS();
		
		log.info("finished .......................................");
	}
}
