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

package playground.anhorni.surprice;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import playground.anhorni.surprice.analysis.SupriceBoxPlot;
import playground.anhorni.surprice.preprocess.Zone;

public class UtilityAnalyzer {
	
	private List<Zone> zones = new Vector<Zone>();
	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private SupriceBoxPlot boxPlotRelative = new SupriceBoxPlot("Utilities", "Day", "Utility", 0.0, 0.0);
	private SupriceBoxPlot boxPlotAbsolute = new SupriceBoxPlot("Utilities", "Day", "Utility", 0.0, 0.0);
	
	private final static Logger log = Logger.getLogger(UtilityAnalyzer.class);
	
	public static void main (final String[] args) {		
		if (args.length != 2) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		
		UtilityAnalyzer analyzer = new UtilityAnalyzer();
		
		String configFile = args[0];
		String configCreateFile = args[1];
		
		Config config = ConfigUtils.loadConfig(configFile);
		String outPath = config.controler().getOutputDirectory();
		
		Config configCreate = ConfigUtils.loadConfig(configCreateFile);
		double sideLength = Double.parseDouble(configCreate.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		
		analyzer.analyze(config, outPath, sideLength);
		
		log.info("=================== Finished analyses ====================");
	}
		
	public void analyze(Config config, String outPath, double sideLength) {
		this.initZones(sideLength);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		new FacilitiesReaderMatsimV1(scenario).readFile(config.facilities().getInputFile());
		
		TreeMap<String, Utilities> utilitiesPerZone = new TreeMap<String, Utilities>();
		ArrayList<Double> utilitiesRelative = new ArrayList<Double>();
		ArrayList<Double> utilitiesAbsolute = new ArrayList<Double>();
		
		for (String day : Surprice.days) {
			String plansFilePath = outPath + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
			populationReader.readFile(plansFilePath);

			this.computeZoneUtilities(utilitiesPerZone, day);
			this.computeUtilities(utilitiesRelative, day, "rel");
			this.boxPlotRelative.addValuesPerCategory(utilitiesRelative, day, "Utilities");
			
			this.computeUtilities(utilitiesAbsolute, day, "abs");
			this.boxPlotAbsolute.addValuesPerCategory(utilitiesAbsolute, day, "Utilities");
						
			this.scenario.getPopulation().getPersons().clear();
		}	
		this.write(outPath, utilitiesPerZone);
		this.boxPlotRelative.createChart();
		this.boxPlotRelative.saveAsPng(outPath + "/utilitiesRelative.png", 400, 300);
		
		this.boxPlotAbsolute.createChart();
		this.boxPlotAbsolute.saveAsPng(outPath + "/utilitiesAbsolute.png", 400, 300);
	}
	
	private void computeUtilities(ArrayList<Double> utilities, String day, String type) {		
		double avgUtility = 0.0;
		int n = 0;
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			avgUtility += person.getSelectedPlan().getScore();
			n++;
		}
		avgUtility /= n;
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			if (type.equals("rel")) {
				utilities.add(person.getSelectedPlan().getScore() / avgUtility);
			} else {
				utilities.add(person.getSelectedPlan().getScore());
			}
		}		
	}
		
	private void computeZoneUtilities(TreeMap<String, Utilities> utilitiesPerZone, String day) {		
		for (Zone zone : this.zones) {
			if (utilitiesPerZone.get(zone.getName()) == null) {
				Utilities utilitiesPerDay = new Utilities();
				utilitiesPerZone.put(zone.getName(), utilitiesPerDay);
			}
			double avgUtility = 0.0;
			int n = 0;
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				Activity homeAct = (Activity)person.getSelectedPlan().getPlanElements().get(0);
				if (zone.inZone(homeAct.getCoord())) {
					avgUtility += person.getSelectedPlan().getScore();
					n++;
				}
			}
			avgUtility /= n;			
			utilitiesPerZone.get(zone.getName()).setUtilityPerDay(day, avgUtility);			
		}
	}
	
	private void write(String outPath, TreeMap<String, Utilities> utilitiesPerZone) {
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt")); 
			bufferedWriter.write("Zone\tmon\ttue\twed\tthu\tfri\tsat\tsun\n");
			for (Zone zone : this.zones) {
				Utilities uPerZone = utilitiesPerZone.get(zone.getName());
				
				String line = zone.getName();
				
				for (String day : Surprice.days) {
					line += "\t" + formatter.format(uPerZone.getUtilityPerDay(day));
				}			
				bufferedWriter.append(line);
				bufferedWriter.newLine();
			}			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initZones(double sideLength) {
		this.zones.add(new Zone("centerZone", (Coord) new Coord(sideLength / 2.0 - 500.0, sideLength / 2.0 + 500.0), 1000.0, 1000.0));
		this.zones.add(new Zone("topLeftZone", (Coord) new Coord(0.0, sideLength), 1000.0, 1000.0));
		this.zones.add(new Zone("bottomLeftZone", (Coord) new Coord(0.0, 1000.0), 1000.0, 1000.0));
		this.zones.add(new Zone("bottomRightZone", (Coord) new Coord(sideLength - 1000.0, 0.0), 1000.0, 1000.0));
	}
}
