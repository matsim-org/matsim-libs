/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

/**
 * 
 */

package playground.artemc.analysis;

import org.apache.log4j.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.charts.XYLineChart;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ihab
 * @author artemc
 *
 */

public class AnalysisControlerListener implements StartupListener, IterationEndsListener {
	private static final Logger log = Logger.getLogger(AnalysisControlerListener.class);

	private final ScenarioImpl scenario;
	private MoneyEventHandler moneyHandler = new MoneyEventHandler();
	private TripAnalysisHandler tripAnalysisHandler;
	private ScheduleDelayCostHandler  scheduleDelayCostHandler;

	public TripAnalysisHandler getTripAnalysisHandler() {
		return tripAnalysisHandler;
	}

	private Map<Integer, Double> it2userBenefits_logsum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_logsum = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_logsum = new TreeMap<Integer, Integer>();

	private Map<Integer, Double> it2userBenefits_selected = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_selected = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_selected = new TreeMap<Integer, Integer>();

	private Map<Integer, Double> it2tollSum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2stuckEvents = new TreeMap<Integer, Integer>();
	private Map<Integer, Double> it2totalTravelTimeAllModes = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2totalTravelTimeCarMode = new TreeMap<Integer, Double>();

	private Map<Integer, Double> it2carLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2ptLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2walkLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2transitWalkLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2busLegs = new TreeMap<Integer, Double>();

	private Map<Integer, HashMap<String, Double>> ttc_morning =  new TreeMap<Integer,  HashMap<String, Double>>();
	private Map<Integer, HashMap<String, Double>> scd_morning =  new TreeMap<Integer,  HashMap<String, Double>>();
	private Map<Integer, HashMap<String, Double>> ttc_evening =  new TreeMap<Integer,  HashMap<String, Double>>();
	private Map<Integer, HashMap<String, Double>> scd_evening =  new TreeMap<Integer,  HashMap<String, Double>>();

	private HashMap<String, Double> currentTTC_morning;
	private HashMap<String, Double> currentSCD_morning;
	private HashMap<String, Double> currentTTC_evening;
	private HashMap<String, Double> currentSCD_evening;

	HashMap<String, ArrayList<Double>> ttcMorningByMode;
	HashMap<String, ArrayList<Double>> ttcEveningByMode;
	HashMap<String, ArrayList<Double>> sdcMorningByMode;
	HashMap<String, ArrayList<Double>> sdcEveningByMode;

	private HashMap<String, ArrayList<Id<Person>>> person2mode = new HashMap<String, ArrayList<Id<Person>>>();


	public AnalysisControlerListener(ScenarioImpl scenario){
		this.scenario = scenario;
		this.tripAnalysisHandler = new TripAnalysisHandler(scenario);
		this.scheduleDelayCostHandler = new ScheduleDelayCostHandler();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(moneyHandler);
		event.getControler().getEvents().addHandler(tripAnalysisHandler);
		this.scheduleDelayCostHandler.setControler(event.getControler());
		event.getControler().getEvents().addHandler(scheduleDelayCostHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		addMonetaryExpensesToPlanAttributes(event);
		runCalculation(event);
		writeWelfareAnalysis(event);
		writeTripAnalysis(event);
		writeTimeCostAnalysis(event);
	}

	private void addMonetaryExpensesToPlanAttributes(IterationEndsEvent event){
		for(Person person:event.getControler().getScenario().getPopulation().getPersons().values()){
			Double monetaryPayments = moneyHandler.getPersonId2amount().get(person.getId());
			if(monetaryPayments==null) {
				monetaryPayments=0.0;
			}
			person.getSelectedPlan().getCustomAttributes().put("toll",monetaryPayments.toString());
			event.getControler().getScenario().getPopulation().getPersonAttributes().putAttribute(person.getId().toString(),"selectedPlanToll",monetaryPayments.toString());
		}
	}

	private void runCalculation(IterationEndsEvent  event){
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.LOGSUM, false);
        this.it2userBenefits_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.calculateUtility_money(event.getControler().getScenario().getPopulation()));
		this.it2invalidPersons_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getInvalidPlans());

		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.SELECTED, false);
        this.it2userBenefits_selected.put(event.getIteration(), userBenefitsCalculator_selected.calculateUtility_money(event.getControler().getScenario().getPopulation()));
		this.it2invalidPersons_selected.put(event.getIteration(), userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_selected.put(event.getIteration(), userBenefitsCalculator_selected.getInvalidPlans());

		double tollSum = 0.0;
		for(Person person:event.getControler().getScenario().getPopulation().getPersons().values()){
			Double monetaryPayments = moneyHandler.getPersonId2amount().get(person.getId());
			if(monetaryPayments==null) {
				monetaryPayments=0.0;
			}
			tollSum = tollSum + monetaryPayments;
		}

		this.it2tollSum.put(event.getIteration(), (-1) * tollSum);
		this.it2stuckEvents.put(event.getIteration(), this.tripAnalysisHandler.getAgentStuckEvents());
		this.it2totalTravelTimeAllModes.put(event.getIteration(), this.tripAnalysisHandler.getTotalTravelTimeAllModes());
		this.it2totalTravelTimeCarMode.put(event.getIteration(), this.tripAnalysisHandler.getTotalTravelTimeCarMode());
		this.it2carLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getModeLegCount().get("car"));
		this.it2ptLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getModeLegCount().get("pt"));
		this.it2walkLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getModeLegCount().get("walk"));
		this.it2transitWalkLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getModeLegCount().get("transit_walk"));
		//		this.it2carLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getCarLegs());
		//		this.it2ptLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getPtLegs());
		//		this.it2walkLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getWalkLegs());
		//		this.it2transitWalkLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getTransitWalkLegs());
		//		this.it2busLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getBusLegs());
		//	


		this.currentTTC_morning  =  new HashMap<String, Double>();
		this.currentSCD_morning  =  new HashMap<String, Double>();
		this.currentTTC_evening  =  new HashMap<String, Double>();
		this.currentSCD_evening  =  new HashMap<String, Double>();

		this.ttcMorningByMode = new HashMap<String, ArrayList<Double>>();
		this.ttcEveningByMode = new HashMap<String, ArrayList<Double>>();
		this.sdcMorningByMode = new HashMap<String, ArrayList<Double>>();
		this.sdcEveningByMode = new HashMap<String, ArrayList<Double>>();

		for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
			ttcMorningByMode.put(mode, new ArrayList<Double>());
			ttcEveningByMode.put(mode, new ArrayList<Double>());
			sdcMorningByMode.put(mode, new ArrayList<Double>());
			sdcEveningByMode.put(mode, new ArrayList<Double>());
		}

        for(Id<Person> id: event.getControler().getScenario().getPopulation().getPersons().keySet()){

			if(!this.scheduleDelayCostHandler.getStuckedAgents().contains(id)){
				ttcMorningByMode.get(this.scheduleDelayCostHandler.getModes().get(id).get(0)).add(this.scheduleDelayCostHandler.getTTC_morning().get(id));
				ttcEveningByMode.get(this.scheduleDelayCostHandler.getModes().get(id).get(1)).add(this.scheduleDelayCostHandler.getTTC_evening().get(id));
				sdcMorningByMode.get(this.scheduleDelayCostHandler.getModes().get(id).get(0)).add(this.scheduleDelayCostHandler.getSDC_morning().get(id));
				sdcEveningByMode.get(this.scheduleDelayCostHandler.getModes().get(id).get(1)).add(this.scheduleDelayCostHandler.getSDC_evening().get(id));

				//				System.out.println(id.toString());
				//				System.out.println("ttc_morning: "+this.scheduleDelayCostHandler.getModes().get(id).get(0)+","+this.scheduleDelayCostHandler.getTTC_morning().get(id));
				//				System.out.println("ttc_evening: "+this.scheduleDelayCostHandler.getModes().get(id).get(1)+","+this.scheduleDelayCostHandler.getTTC_evening().get(id));
				//				System.out.println("sdc_morning: "+this.scheduleDelayCostHandler.getModes().get(id).get(0)+","+this.scheduleDelayCostHandler.getSDC_morning().get(id));
				//				System.out.println("sdc_eveningg: "+this.scheduleDelayCostHandler.getModes().get(id).get(1)+","+this.scheduleDelayCostHandler.getSDC_evening().get(id));
				//				System.out.println();
			}
			else{
				log.warn("Stucked Agents are not considered! Agent stucked: "+id.toString());
			}
		}

		MeanCalculator meanCalculator = new MeanCalculator();
		for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
			this.currentTTC_morning.put(mode, meanCalculator.getMean(ttcMorningByMode.get(mode)));
			this.currentTTC_evening.put(mode, meanCalculator.getMean(ttcEveningByMode.get(mode)));	
			this.currentSCD_morning.put(mode, meanCalculator.getMean(sdcMorningByMode.get(mode)));
			this.currentSCD_evening.put(mode, meanCalculator.getMean(sdcEveningByMode.get(mode)));
		}

		this.ttc_morning.put(event.getIteration(), this.currentTTC_morning);
		this.scd_morning.put(event.getIteration(), this.currentSCD_morning);
		this.ttc_evening.put(event.getIteration(), this.currentTTC_evening);
		this.scd_evening.put(event.getIteration(), this.currentSCD_evening);

	}

	private void writeWelfareAnalysis(IterationEndsEvent event) {	

		String welfareFilePath = this.scenario.getConfig().controler().getOutputDirectory() + "/welfareAnalysis.csv";
		File welfareFile = new File(welfareFilePath);

		try {
			BufferedWriter welfareWriter = new BufferedWriter(new FileWriter(welfareFile));
			welfareWriter.write("Iteration;" +
					"User Benefits (LogSum);Number of Invalid Persons (LogSum);Number of Invalid Plans (LogSum);" +
					"User Benefits (Selected);Number of Invalid Persons (Selected);Number of Invalid Plans (Selected);" +
					"Total Monetary Payments;Welfare (LogSum);Welfare (Selected);Total Travel Time All Modes (sec);Total Travel Time Car Mode (sec);Avg Travel Time Per Car Trip (sec);Number of Agent Stuck Events");

			welfareWriter.newLine();
			for (Integer it : this.it2userBenefits_selected.keySet()){
				welfareWriter.write(it + ";" + this.it2userBenefits_logsum.get(it) + ";" + this.it2invalidPersons_logsum.get(it) + ";" + this.it2invalidPlans_logsum.get(it)
						+ ";" + this.it2userBenefits_selected.get(it) + ";" + this.it2invalidPersons_selected.get(it) + ";" + this.it2invalidPlans_selected.get(it)
						+ ";" + this.it2tollSum.get(it)
						+ ";" + (this.it2userBenefits_logsum.get(it) + this.it2tollSum.get(it))
						+ ";" + (this.it2userBenefits_selected.get(it) + this.it2tollSum.get(it))
						+ ";" + this.it2totalTravelTimeAllModes.get(it)
						+ ";" + this.it2totalTravelTimeCarMode.get(it)
						+ ";" + (this.it2totalTravelTimeCarMode.get(it) / this.it2carLegs.get(it))
						+ ";" + this.it2stuckEvents.get(it)
						);
				welfareWriter.newLine();

			}
			welfareWriter.close();
			log.info("Welfare analysis Output written to " + welfareFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ##################################################

		writeGraph("userBenefits_selected", "Monetary Units", it2userBenefits_selected);
		writeGraph("userBenefits_logsum", "Monetary Units", it2userBenefits_logsum);
		writeGraph("tollSum", "Monetary Units", it2tollSum);
		writeGraph("totalTravelTimeAllModes", "Seconds", it2totalTravelTimeAllModes);
		writeGraph("totalTravelTimeCarMode", "Seconds", it2totalTravelTimeCarMode);
		writeGraph("carTrips", "Number of Car Trips", it2carLegs);
		writeGraph("ptTrips", "Number of Pt Trips", it2ptLegs);
		writeGraph("walkTrips", "Number of Walk Trips", it2walkLegs);
		writeGraph("transitWalkTrips", "Number of Transit Walk Leg", it2transitWalkLegs);
		writeGraph("busVehicleTrips", "Number of Bus Vehicle Trips", it2busLegs);

		writeGraphSum("welfare_logsum", "Monetary Units", it2userBenefits_logsum, it2tollSum);
		writeGraphSum("welfare_selected", "Monetary Units", it2userBenefits_selected, it2tollSum);

		writeGraphDiv("avgTripTravelTimeCar", "Seconds", it2totalTravelTimeCarMode, it2carLegs);
	}

	private void writeTripAnalysis(IterationEndsEvent event){

		String tripsFilePath = this.scenario.getConfig().controler().getOutputDirectory() + "/tripsAnalysis.csv";
		File tripsFile = new File(tripsFilePath);

		try {
			BufferedWriter tripsWriter = new BufferedWriter(new FileWriter(tripsFile));

			tripsWriter.write("Car Trips;Pt Trips;Walk Trips; TrasitWalk Legs");

			tripsWriter.newLine();

			for (Integer it : this.it2userBenefits_selected.keySet()){
				tripsWriter.write(it 
						+ ";" + this.it2carLegs.get(it)
						+ ";" + this.it2ptLegs.get(it)
						+ ";" + this.it2walkLegs.get(it)
						+ ";" + this.it2transitWalkLegs.get(it)
						);

				tripsWriter.newLine();
			}
			tripsWriter.close();
			log.info("Trip analysis Output written to " + tripsFilePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeTimeCostAnalysis(IterationEndsEvent event){

		String timeCostFilePath = this.scenario.getConfig().controler().getOutputDirectory() + "/timeCost.csv";
		File timeCostFile = new File( timeCostFilePath);

		try {
			BufferedWriter timeCostWriter = new BufferedWriter(new FileWriter(timeCostFile));

			String header="It;";
			for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
				header=header+"TTC_morning_"+mode+";";
				header=header+"SCD_morning_"+mode+";";
			}

			for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
				header=header+"TTC_evening_"+mode+";";
				header=header+"SCD_evening_"+mode+";";
			}

			timeCostWriter.write(header);
			timeCostWriter.newLine();

			for (Integer it: this.scd_morning.keySet()){
				String data=it+";";

				for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
					data=data+ttc_morning.get(it).get(mode)+";";
					data=data+scd_morning.get(it).get(mode)+";";
				}

				for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
					data=data+ttc_evening.get(it).get(mode)+";";
					data=data+scd_evening.get(it).get(mode)+";";
				}

				timeCostWriter.write(data);
				timeCostWriter.newLine();
			}
			timeCostWriter.close();
			log.info("Tinme cost analysis Output written to " + timeCostFilePath);


			//Write Time Cost Summary File for the Last Iteration		
			if(event.getIteration()==event.getControler().getScenario().getConfig().controler().getLastIteration()){
				String timeCostSummaryFilePath = this.scenario.getConfig().controler().getOutputDirectory() + "/timeCostSummary.it"+event.getIteration()+".csv";
				File timeCostSummaryFile = new File( timeCostSummaryFilePath);

				BufferedWriter timeCostSummaryWriter = new BufferedWriter(new FileWriter(timeCostSummaryFile));
				timeCostSummaryWriter.write("Mode;TTC_Total;SDC_total;TTC_morning;TTC_evening;SDC_morning;SDC_evening;Total_Journeys");
				timeCostSummaryWriter.newLine();

				for(String mode:this.scheduleDelayCostHandler.getUsedModes()){
					String data=mode+";";

					double sumTTCMorning =0.0;
					double sumTTCEvening =0.0;
					double sumSDCMorning =0.0;
					double sumSDCEvening =0.0;
					double sumTTCTotal =0.0;
					double sumSDCTotal = 0.0;
					int sumModeJourneys = 0;

					for(int i=0;i<ttcMorningByMode.get(mode).size();i++){
						sumTTCMorning += ttcMorningByMode.get(mode).get(i);
					}

					for(int i=0;i<ttcEveningByMode.get(mode).size();i++){
						sumTTCEvening += ttcEveningByMode.get(mode).get(i);
					}
					for(int i=0;i<sdcMorningByMode.get(mode).size();i++){
						sumSDCMorning += sdcMorningByMode.get(mode).get(i);
					}
					for(int i=0;i<sdcEveningByMode.get(mode).size();i++){
						sumSDCEvening += sdcEveningByMode.get(mode).get(i);
					}
					sumTTCTotal = sumTTCMorning + sumTTCEvening;
					sumSDCTotal = sumSDCMorning + sumSDCEvening;
					sumModeJourneys = ttcMorningByMode.get(mode).size() + ttcEveningByMode.get(mode).size();

					data += sumTTCTotal+";"+sumSDCTotal+";"+sumTTCMorning+";"+sumTTCEvening+";"+sumSDCMorning+";"+sumSDCEvening+";"+sumModeJourneys +";";

					timeCostSummaryWriter.write(data);
					timeCostSummaryWriter.newLine();	
				}
				timeCostSummaryWriter.close();
				log.info("Time cost summary for the last iteration was written to " + timeCostSummaryFilePath);

			}


			//Write Person Time Cost File for the last Iteration
			if(event.getIteration()==event.getControler().getScenario().getConfig().controler().getLastIteration()){
				String timeCostPersonFilePath = this.scenario.getConfig().controler().getOutputDirectory() + "/timeCostPerPerson.it"+event.getIteration()+".csv";
				File timeCostPersonFile = new File(timeCostPersonFilePath);

				BufferedWriter timeCostPersonWriter = new BufferedWriter(new FileWriter(timeCostPersonFile));
				timeCostPersonWriter.write("Id;TTC_Total;SDC_total;TTC_morning;TTC_evening;SDC_morning;SDC_evening;");
				timeCostPersonWriter.newLine();

				double sumTTCTotal =0.0;
				double sumSDCTotal = 0.0;

                for(Id<Person> id: event.getControler().getScenario().getPopulation().getPersons().keySet()){
					if(!this.scheduleDelayCostHandler.getStuckedAgents().contains(id)){
						sumTTCTotal = this.scheduleDelayCostHandler.getTTC_morning().get(id) + this.scheduleDelayCostHandler.getTTC_evening().get(id);
						sumSDCTotal = this.scheduleDelayCostHandler.getSDC_morning().get(id) + this.scheduleDelayCostHandler.getSDC_evening().get(id);

						String data=id+";";
						data+=sumTTCTotal+";";
						data+=sumSDCTotal+";";
						
						data+=this.scheduleDelayCostHandler.getTTC_morning().get(id)+";";
						data+=this.scheduleDelayCostHandler.getTTC_evening().get(id)+";";
						data+=this.scheduleDelayCostHandler.getSDC_morning().get(id)+";";
						data+=this.scheduleDelayCostHandler.getSDC_evening().get(id)+";";

						timeCostPersonWriter.write(data);
						timeCostPersonWriter.newLine();
					}
				}
				timeCostPersonWriter.close();
				log.info("Time cost per person for the last iteration was written to " + timeCostPersonFilePath);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	private void writeGraphSum(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) + (it2Double2.get(iteration));
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

	private void writeGraphDiv(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) / (it2Double2.get(iteration));
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

	private void writeGraph(String name, String yLabel, Map<Integer, Double> it2Double) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double.size()];
		double[] yValues = new double[it2Double.size()];
		int counter = 0;
		for (Integer iteration : it2Double.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = it2Double.get(iteration);
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

}
