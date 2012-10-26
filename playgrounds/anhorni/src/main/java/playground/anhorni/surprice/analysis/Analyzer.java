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

package playground.anhorni.surprice.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.CalcAverageTolledTripLength;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.anhorni.surprice.Surprice;

public class Analyzer {
	private ScenarioImpl scenario = null; 
	private Config config = null;
	private final static Logger log = Logger.getLogger(Analyzer.class);
	private double ttAvg[] = new double[8]; 
	private double tdAvg[] = new double[8]; 
	private double tolltdAvg[] = new double[8];
	private double utilitiesAvg[] = new double[8];
	
	private SupriceBoxPlot boxPlotRelative = new SupriceBoxPlot("Utilities", "Day", "Utility");
	private SupriceBoxPlot boxPlotAbsolute = new SupriceBoxPlot("Utilities", "Day", "Utility");
	private SupriceBoxPlot boxPlotTravelTimes = new SupriceBoxPlot("Travel Times", "Day", "tt");
	private SupriceBoxPlot boxPlotTravelDistancesCar = new SupriceBoxPlot("Travel Distances Car", "Day", "td");
	
	public static void main (final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		Analyzer analyzer = new Analyzer();
		String configFile = args[0];
		analyzer.init(configFile);
		analyzer.run();	
	}
	
	public void init(String configFile) {
		this.config = ConfigUtils.loadConfig(configFile);
		this.init(config);
	}
	
	public void init(Config config) {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
	}
	
	public void run() {
		String outPath = config.controler().getOutputDirectory();
		this.analyze(outPath);
		log.info("=================== Finished analyses ====================");
	}
		
	public void analyze(String outPath) {		
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());		
		new FacilitiesReaderMatsimV1(scenario).readFile(config.facilities().getInputFile());
		
		ArrayList<Double> utilitiesRelative = new ArrayList<Double>();
		ArrayList<Double> utilitiesAbsolute = new ArrayList<Double>();
						
		for (String day : Surprice.days) {
			log.info("Analyzing " + day + " --------------------------------------------");
			String plansFilePath = outPath + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
			populationReader.readFile(plansFilePath);						
			
			String eventsfile = outPath + "/" + day + "/ITERS/it." + this.config.controler().getLastIteration() + "/" + day + "." + this.config.controler().getLastIteration() + ".events.xml.gz";
			this.analyzeDay(eventsfile, day, config, utilitiesRelative,utilitiesAbsolute);
		
			this.scenario.getPopulation().getPersons().clear();
		}	
		this.write(outPath);
	}
	
	private void analyzeDay(String eventsfile, String day, Config config, 
			ArrayList<Double> utilitiesRelative, ArrayList<Double> utilitiesAbsolute) {
		
		TravelDistanceCalculator tdCalculator = new TravelDistanceCalculator(this.scenario.getNetwork());			
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			tdCalculator.run(person.getSelectedPlan());
		}
		this.tdAvg[Surprice.days.indexOf(day)] = tdCalculator.getAverageTripLength();
		this.boxPlotTravelDistancesCar.addValuesPerDay(tdCalculator.getTravelDistances(), day, "Travel Distances Car");
		
		this.computeUtilities(utilitiesRelative, day, "rel");
		boxPlotRelative.addValuesPerDay(utilitiesRelative, day, "Utilities");
		
		this.utilitiesAvg[Surprice.days.indexOf(day)] = this.computeUtilities(utilitiesAbsolute, day, "abs");
		boxPlotAbsolute.addValuesPerDay(utilitiesAbsolute, day, "Utilities");	
		
		
		EventsManager events = EventsUtils.createEventsManager();
		
		TravelTimeCalculator ttCalculator = new TravelTimeCalculator();
		events.addHandler(ttCalculator);

		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl(); //(RoadPricingSchemeImpl)this.scenario.getScenarioElement(RoadPricingScheme.class);
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);		
		try {
			log.info("parsing " + config.roadpricing().getTollLinksFile());
			rpReader.parse(config.roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		CalcAverageTolledTripLength tollCalculator = new CalcAverageTolledTripLength(this.scenario.getNetwork(), scheme);
		events.addHandler(tollCalculator);
		
		new MatsimEventsReader(events).readFile(eventsfile);
				
		this.ttAvg[Surprice.days.indexOf(day)] = ttCalculator.getAverageTripDuration();
		this.tolltdAvg[Surprice.days.indexOf(day)] = tollCalculator.getAverageTripLength();	
		this.boxPlotTravelTimes.addValuesPerDay(ttCalculator.getTravelTimes(), day, "Travel Times");
	}
	
	public void writeBoxPlots(String outPath) {			
		this.boxPlotRelative.createChart();
		this.boxPlotRelative.saveAsPng(outPath + "/utilitiesRelative.png", 800, 600);
		
		this.boxPlotAbsolute.createChart();
		this.boxPlotAbsolute.saveAsPng(outPath + "/utilitiesAbsolute.png", 800, 600);
		
		this.boxPlotTravelTimes.createChart();
		this.boxPlotTravelTimes.saveAsPng(outPath + "/traveltimes.png", 800, 600);
		
		this.boxPlotTravelDistancesCar.createChart();
		this.boxPlotTravelDistancesCar.saveAsPng(outPath + "/traveldistances.png", 800, 600);
	}
			
	private double computeUtilities(ArrayList<Double> utilities, String day, String type) {		
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
		return avgUtility;
	}
			
	private void write(String outPath) {
		
		this.writeBoxPlots(outPath);
		
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt")); 
			
			bufferedWriter.write("tt\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			String line = "tt\t";
			double avgTT = 0.0;
			for (String day : Surprice.days) {	
				double tt = this.ttAvg[Surprice.days.indexOf(day)];
				line += formatter.format(tt) + "\t";			
				avgTT += tt / Surprice.days.size();
			}	
			line += formatter.format(avgTT) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("td\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "td\t";
			double avgTD = 0.0;
			for (String day : Surprice.days) {	
				double td = this.tdAvg[Surprice.days.indexOf(day)];
				line += formatter.format(td) + "\t";			
				avgTD += td / Surprice.days.size();
			}	
			line += formatter.format(avgTD) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("tolltd\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "tolltd\t";
			double avgTollTD = 0.0;
			for (String day : Surprice.days) {	
				double tolltd = this.tolltdAvg[Surprice.days.indexOf(day)];
				line += formatter.format(tolltd) + "\t";			
				avgTollTD += tolltd / Surprice.days.size();
			}	
			line += formatter.format(avgTollTD) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("utility\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "utility\t";
			double avgUtility = 0.0;
			for (String day : Surprice.days) {	
				double utility = this.utilitiesAvg[Surprice.days.indexOf(day)];
				line += formatter.format(utility) + "\t";			
				avgUtility += utility / Surprice.days.size();
			}	
			line += formatter.format(avgUtility) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
