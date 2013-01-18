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

public class Controler {
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk", ""));
	public static ArrayList<String> frequency = new ArrayList<String>(Arrays.asList("VeryOften", "Often", "OnceAWhile", "Seldom", "Never", "NULL", ""));	
	private TreeMap<Id, EstimationPerson> population = new TreeMap<Id, EstimationPerson>();
	private final static Logger log = Logger.getLogger(Controler.class);
	
	public static void main(String[] args) {
		Controler c = new Controler();
		String personFile = args[0];
		String personShopsFile = args[1];
		String addedShopsFile = args[2];
		String outdir = args[3];
		c.run(personFile, personShopsFile, addedShopsFile, outdir);
	}
	
	public void run(String personFile, String personShopsFile, String addedShopsFile, String outdir) {
		SurveyReader reader = new SurveyReader(this.population);
		reader.readDumpedPersons(personFile);
		log.info(this.population.size() + " persons created");
		reader.readDumpedPersonShops(personShopsFile);	
		
		SurveyCleaner cleaner = new SurveyCleaner();
		cleaner.clean(this.population);
		
		SurveyAnalyzer analyzer = new SurveyAnalyzer(this.population, outdir);
		analyzer.analyze();
		log.info("finished .......................................");
	}
}
