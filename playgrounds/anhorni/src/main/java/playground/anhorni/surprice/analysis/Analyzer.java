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

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.surprice.Surprice;
import playground.anhorni.utils.Utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;

public class Analyzer {
	private Scenario scenario = null; 
	private Config config = null;
	private final static Logger log = Logger.getLogger(Analyzer.class);
	private double ttAvg[] = new double[8]; 
	private double tdAvg[] = new double[8]; 
	private double tolltdAvg[] = new double[8];
	private double utilitiesAvg[] = new double[8];
	private double nTripsAvg[] = new double[8];
	 	
	private TreeMap<String, Bins> modeBins = new TreeMap<String, Bins>();
	
	private Bins utilityBins;
	private Bins ttBins;
	private Bins tdBins;
	private Bins tolltdBins;
	
	private ObjectAttributes incomes;
	
	private SupriceBoxPlot boxPlotRelative = new SupriceBoxPlot("Utilities", "Day", "Utility", 0.0, 0.0);
	private SupriceBoxPlot boxPlotAbsolute = new SupriceBoxPlot("Utilities", "Day", "Utility", -100.0, 400.0);
	private SupriceBoxPlot boxPlotTravelTimes = new SupriceBoxPlot("Travel Times", "Day", "tt", 0, 2400.0);
	private SupriceBoxPlot boxPlotTravelDistancesCar = new SupriceBoxPlot("Travel Distances Car", "Day", "td", 0.0, 3500.0);
	
	private SupriceBoxPlot boxPlotTravelDistancesTolledPerIncome = new SupriceBoxPlot("Tolled Travel Distances", "Income", "tolltd", 0.0, 3500.0);
	private SupriceBoxPlot boxPlotTravelDistancesCarPerIncome = new SupriceBoxPlot("Travel Distances Car", "Income", "td", 0.0, 3500.0);	
	private SupriceBoxPlot boxPlotTravelTimesCarPerIncome = new SupriceBoxPlot("Travel Times Car", "Income", "tt",0, 2400.0);
	private SupriceBoxPlot boxPlotTravelTimesPtPerIncome = new SupriceBoxPlot("Travel Times Pt", "Income", "tt", 0, 2400.0);
	
	private TreeMap<Id, Double> tolltdPerAgent;
	private TreeMap<Id, Double> ttPerAgent;
	
	private String outPath;
	
	private int finalIterations[] = null;
		
	public static void main (final String[] args) {		
		if (args.length != 2) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		String configFile = args[0];
		String incomesFile = args[1];
		Analyzer analyzer = new Analyzer();
		analyzer.init(configFile, incomesFile);
		analyzer.run();	
	}
	
	public void setFinalIterations(int finalIterations[]) {
		this.finalIterations = finalIterations;
	}
		
	public void init(String configFile, String incomesFile) {
		log.info("config file: " + configFile);
		log.info("incomes file: " + incomesFile);
		this.incomes = new ObjectAttributes();
			
		ObjectAttributesXmlReader incomesReader = new ObjectAttributesXmlReader(this.incomes);
		incomesReader.parse(incomesFile);
		
		this.config = ConfigUtils.loadConfig(configFile);
		this.scenario = ScenarioUtils.createScenario(config);
		
		if (this.finalIterations == null) {
			this.finalIterations = new int[7];
			int lastIterationConfig = this.scenario.getConfig().controler().getLastIteration();
			for (int i = 0; i < 7; i++) {
				this.finalIterations[i] = lastIterationConfig;
	 		}
		}
	}
		
	public void run() {
		log.info("Starting analysis ============================================================= ");
		outPath = config.controler().getOutputDirectory();
		this.analyze();
		log.info("=================== Finished analyses ====================");
	}
		
	public void analyze() {		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		new FacilitiesReaderMatsimV1(scenario).readFile(config.facilities().getInputFile());
		
		ArrayList<Double> utilitiesRelative = new ArrayList<Double>();
		ArrayList<Double> utilitiesAbsolute = new ArrayList<Double>();
						
		for (String day : Surprice.days) {
			log.info("Analyzing " + day + " --------------------------------------------");
			String plansFilePath = outPath + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
			populationReader.readFile(plansFilePath);						
			
			String eventsfile = outPath + "/" + day + "/ITERS/it." + finalIterations[Surprice.days.indexOf(day)] +
					"/" + day + "." + finalIterations[Surprice.days.indexOf(day)] + ".events.xml.gz";
			
			this.analyzeDay(eventsfile, day, config, utilitiesRelative, utilitiesAbsolute);
		
			this.scenario.getPopulation().getPersons().clear();
		}	
		this.write();
	}
	
	private void analyzeDay(String eventsfile, String day, Config config, 
			ArrayList<Double> utilitiesRelative, ArrayList<Double> utilitiesAbsolute) {
		
		this.utilityBins = new Bins(1, Surprice.numberOfIncomeCategories, day + ".utilitiesPerIncome");
		this.ttBins = new Bins(1, Surprice.numberOfIncomeCategories, day + ".ttPerIncome");
		this.tdBins = new Bins(1, Surprice.numberOfIncomeCategories, day + ".tdPerIncome");
		this.tolltdBins = new Bins(1, Surprice.numberOfIncomeCategories, day + ".tolltdPerIncome");	
		
		this.modeBins.put("car", new Bins(1, Surprice.numberOfIncomeCategories, day + ".carPerIncome"));
		this.modeBins.put("pt", new Bins(1, Surprice.numberOfIncomeCategories, day + ".ptPerIncome"));
		this.modeBins.put("bike", new Bins(1, Surprice.numberOfIncomeCategories, day + ".bikePerIncome"));
		this.modeBins.put("walk", new Bins(1, Surprice.numberOfIncomeCategories, day + ".walkPerIncome"));
		
		log.info("	analyzing travel distances ...");
		TravelDistanceCalculator tdCalculator = new TravelDistanceCalculator(this.scenario.getNetwork(), this.tdBins, this.incomes, day);			
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			tdCalculator.run(person.getSelectedPlan());
		}
		this.tdAvg[Surprice.days.indexOf(day)] = tdCalculator.getAverageTripLength();
		this.boxPlotTravelDistancesCar.addValuesPerCategory(tdCalculator.getTravelDistances(), day, "Travel Distances Car");
		
		log.info("	analyzing utilities ...");
		this.computeUtilities(utilitiesRelative, day, "rel");
		boxPlotRelative.addValuesPerCategory(utilitiesRelative, day, "Utilities");
		
		this.utilitiesAvg[Surprice.days.indexOf(day)] = this.computeUtilities(utilitiesAbsolute, day, "abs");
		boxPlotAbsolute.addValuesPerCategory(utilitiesAbsolute, day, "Utilities");	
		
		log.info("	analyzing travel times ...");
		EventsManager events = EventsUtils.createEventsManager();
		TravelTimeCalculator ttCalculator = new TravelTimeCalculator(this.ttBins, this.incomes);
		events.addHandler(ttCalculator);
		
		log.info("	analyzing toll travel distances ...");
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl(); //(RoadPricingSchemeImpl)this.scenario.getScenarioElement(RoadPricingScheme.class);
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);		
		try {
            log.info("parsing " + ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile());
            rpReader.parse(ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		TolledTripLengthCalculator tollCalculator = new TolledTripLengthCalculator(this.scenario.getNetwork(), scheme, this.tolltdBins, this.incomes);
		events.addHandler(tollCalculator);
		
		new MatsimEventsReader(events).readFile(eventsfile);				
		this.ttAvg[Surprice.days.indexOf(day)] = ttCalculator.getAverageTripDuration();
		this.tolltdAvg[Surprice.days.indexOf(day)] = tollCalculator.getAverageTripLength();	
		this.boxPlotTravelTimes.addValuesPerCategory(ttCalculator.getTravelTimes(), day, "Travel Times");
		this.tolltdPerAgent = tollCalculator.getTollDistancesAgents();
		this.ttPerAgent = ttCalculator.getTTPerAgent();
		this.nTripsAvg[Surprice.days.indexOf(day)] = ttCalculator.getTravelTimes().size() / this.scenario.getPopulation().getPersons().size();
		
		this.computeModesPerIncome();
				
		for (int i = 0; i < Surprice.numberOfIncomeCategories; i++) {
			this.boxPlotTravelTimesCarPerIncome.addValuesPerCategory(ttCalculator.getCarPerIncome().get(i), Integer.toString(i), "tt");
			if (ttCalculator.getPTPerIncome().size() > 0) {
				this.boxPlotTravelTimesPtPerIncome.addValuesPerCategory(ttCalculator.getPTPerIncome().get(i), Integer.toString(i), "tt");
			}
			this.boxPlotTravelDistancesCarPerIncome.addValuesPerCategory(tdCalculator.getCar().get(i), Integer.toString(i), "tt");
			if (tollCalculator.getTollDistancesPerIncome().get(i) != null) {
				this.boxPlotTravelDistancesTolledPerIncome.addValuesPerCategory(tollCalculator.getTollDistancesPerIncome().get(i), Integer.toString(i), "tt");
			} else {
				ArrayList<Double> zeroArray = new ArrayList<Double>();
				zeroArray.add(0.0);
				this.boxPlotTravelDistancesTolledPerIncome.addValuesPerCategory(zeroArray, Integer.toString(i), "tt");
			}
		}		
		this.writeDaily(day, ttCalculator, tollCalculator);
	}
	
	private void writeDaily(String day, TravelTimeCalculator ttCalculator, TolledTripLengthCalculator tollCalculator) {
		this.utilityBins.plotBinnedDistribution(outPath + "/" + day + "/", "income", "");
		this.ttBins.plotBinnedDistribution(outPath + "/" + day + "/", "income", "");
		this.tdBins.plotBinnedDistribution(outPath + "/" + day + "/", "income", "");
		this.tolltdBins.plotBinnedDistribution(outPath + "/" + day + "/", "income", "");
		
		for (Bins bins : this.modeBins.values()) {
			bins.plotBinnedDistribution(outPath + "/" + day + "/", "income", "");
		}
		this.boxPlotTravelTimesCarPerIncome.createChart();
		this.boxPlotTravelTimesCarPerIncome.saveAsPng(outPath + "/" + day + "/" + day + ".ttCarPerIncome_BP.png", 800, 600);
		
		this.boxPlotTravelTimesPtPerIncome.createChart();
		this.boxPlotTravelTimesPtPerIncome.saveAsPng(outPath + "/" + day + "/" + day + ".ttPtPerIncome_BP.png", 800, 600);
		
		this.boxPlotTravelDistancesCarPerIncome.createChart();
		this.boxPlotTravelDistancesCarPerIncome.saveAsPng(outPath + "/" + day + "/" + day + ".tdCarPerIncome_BP.png", 800, 600);
				
		this.boxPlotTravelDistancesTolledPerIncome.createChart();
		this.boxPlotTravelDistancesTolledPerIncome.saveAsPng(outPath + "/" + day + "/" + day + ".tolltdPerIncome_BP.png", 800, 600);
		
		this.writeAgents(day);
		
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/" + day + "/" + day + ".summary_tt.txt")); 
			bufferedWriter.write("incClass\tavg\tmedian\tstdDev\tCV\n");			
			for (int i = 0; i < Surprice.numberOfIncomeCategories; i++) {
				String line = i + "\t";
				line += formatter.format(Utils.mean(ttCalculator.getTTPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.median(ttCalculator.getTTPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.getStdDev(ttCalculator.getTTPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.getStdDev(ttCalculator.getTTPerIncome().get(i)) /
						Utils.mean(ttCalculator.getTTPerIncome().get(i))) + "\n";
				bufferedWriter.append(line);
			}					
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/" + day + "/" + day + ".summary_cartt.txt")); 
			bufferedWriter.write("incClass\tavg\tmedian\tstdDev\tCV\n");			
			for (int i = 0; i < Surprice.numberOfIncomeCategories; i++) {
				String line = i + "\t";
				line += formatter.format(Utils.mean(ttCalculator.getCarPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.median(ttCalculator.getCarPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.getStdDev(ttCalculator.getCarPerIncome().get(i))) + "\t";
				line += formatter.format(Utils.getStdDev(ttCalculator.getCarPerIncome().get(i)) /
						Utils.mean(ttCalculator.getCarPerIncome().get(i))) + "\n";
				bufferedWriter.append(line);
			}					
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/" + day + "/" + day + ".summary_tolltd.txt")); 
			bufferedWriter.write("incClass\tavg\tmedian\tstdDev\tCV\n");
			for (int i = 0; i < Surprice.numberOfIncomeCategories; i++) {
				String line = i + "\t";
				if (tollCalculator.getTollDistancesPerIncome().get(i) != null) {
					line += formatter.format(Utils.mean(tollCalculator.getTollDistancesPerIncome().get(i)))  + "\t";
					line += formatter.format(Utils.median(tollCalculator.getTollDistancesPerIncome().get(i)))  + "\t";
					line += formatter.format(Utils.getStdDev(tollCalculator.getTollDistancesPerIncome().get(i)))  + "\t";
					line += formatter.format(Utils.getStdDev(tollCalculator.getTollDistancesPerIncome().get(i)) /
							Utils.mean(tollCalculator.getTollDistancesPerIncome().get(i))) + "\n";
				}
				else {
					line += "------- no values ---------------";
				}
				bufferedWriter.append(line);
			}			
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeAgents(String day) {
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(attributes);
		attributesReader.parse(outPath + "/" + day + "/" + day + ".perAgent.txt");
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {					
			attributes.putAttribute(person.getId().toString(), day + ".tt", this.ttPerAgent.get(person.getId()));
			attributes.putAttribute(person.getId().toString(), day + ".td", person.getCustomAttributes().get(day + ".td"));	
			
			if (this.tolltdPerAgent.get(person.getId()) != null) {
				attributes.putAttribute(person.getId().toString(), day + ".tolltd", this.tolltdPerAgent.get(person.getId()));
			}
			else {
				attributes.putAttribute(person.getId().toString(), day + ".tolltd", 0.0);
			}
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(attributes);
		attributesWriter.writeFile(outPath + "/" + day + "/" + day + ".perAgent.txt");
		
	}
	
	private void writePlots() {			
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
				
				double income = (Double)this.incomes.getAttribute(person.getId().toString(), "income") * 8.0;
				this.utilityBins.addVal(income, person.getSelectedPlan().getScore());
			}
		}
		return avgUtility;
	}
	
	private void computeModesPerIncome() {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			double income = (Double)this.incomes.getAttribute(person.getId().toString(), "income") * 8.0;
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					String mode = leg.getMode();
					Bins bins = this.modeBins.get(mode);
					bins.addVal(income, 1.0);
				}
			}
		}
	}
	
			
	private void write() {		
		this.writePlots();		
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt")); 
			
			bufferedWriter.write("avgNbrTrips\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			String line = "avgNbrTrips\t";
			double avgNbrTrips = 0.0;
			for (String day : Surprice.days) {	
				double n = this.nTripsAvg[Surprice.days.indexOf(day)];
				line += formatter.format(n) + "\t";			
				avgNbrTrips += n / Surprice.days.size();
			}	
			line += formatter.format(avgNbrTrips) + "\n";
			bufferedWriter.append(line);
			bufferedWriter.newLine();
			
			bufferedWriter.write("tt\tmon\ttue\twed\tthu\tfri\tsat\tsun\tavg\n");
			line = "tt\t";
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
