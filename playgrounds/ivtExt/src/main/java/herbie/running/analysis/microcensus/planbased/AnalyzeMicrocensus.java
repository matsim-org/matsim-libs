/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package herbie.running.analysis.microcensus.planbased;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.filters.population.PersonIntersectAreaFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import utils.Bins;

/**
 * Based on Balmers conversion of the MC to MATSim plans
 *
 * @author anhorni
 */

public class AnalyzeMicrocensus {

	private final static Logger log = Logger.getLogger(AnalyzeMicrocensus.class);
	private String outputFolder="";
	
	private Bins ch_distanceDistribution;
	private Bins ch_distanceDistributionHomeBased;
	private Bins ch_distanceDistributionHomeBasedRoundTrip;
	private Bins ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery;
	private Bins ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery;
	
	private Bins zh_distanceDistribution;
	
	String type ="";
		
	private MutableScenario scenarioCH;
	private MutableScenario scenarioZH;
	private String mode = null;
		
	public static void main(final String[] args) {
		String [] arguments = args;
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		if(arguments.length ==1) arguments = analyzer.initArgs(args[0]);
		analyzer.run(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);	
		log.info("Analysis finished -----------------------------------------");
	}
	
	private String[] initArgs(String args)
	{
		String [] arguments = new String[5];
		Config config = new Config();
    	ConfigReader configReader = new ConfigReader(config);
    	configReader.readFile(args);
		
    	arguments[0] = config.findParam("analysis", "mode");
    	arguments[1] = config.findParam("analysis", "type");
    	arguments[2] = config.findParam("analysis", "plansFilePath");
    	arguments[3] = config.findParam("analysis", "networkfilePath");
    	arguments[4] = config.findParam("analysis", "outputFolder");
    	
		return arguments;
	}
	
	public void run(String mode,  String type, String plansFilePath, String networkfilePath, String outputFolder) {
		this.outputFolder = outputFolder;
		this.type = type;
		this.init(mode, plansFilePath, networkfilePath);
		this.runAnalysisCH();	
		
		this.dilutedZH();
		this.runAnalysisZH();
	}
	
	private void init(String mode, String plansFilePath, String networkfilePath) {
		scenarioCH = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.mode = mode;
		this.ch_distanceDistribution = new Bins(500.0, 40000.0, type + "_trips_mc_" + this.mode);
		this.ch_distanceDistributionHomeBased = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_" + this.mode);
		this.ch_distanceDistributionHomeBasedRoundTrip = new Bins(500.0, 40000.0, type +  "_trips_mc_home-based_round-trip_" + this.mode);
		
		this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_grocery_" + this.mode);
		this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery = new Bins(500.0, 40000.0, "s_trips_mc_home-based_round-trip_nongrocery_" + this.mode);
		
		this.zh_distanceDistribution = new Bins(500.0, 40000.0, type + "_zh_trips_mc_" + this.mode);
		
		PopulationReader populationReader = new PopulationReader(this.scenarioCH);
		populationReader.readFile(plansFilePath);
		
		scenarioZH = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenarioZH.getNetwork()).readFile(networkfilePath);
	}
	
	private void dilutedZH() {
		double aoiRadius = 30000.0;
		final Coord aoiCenter = new Coord(683518.0, 246836.0);
		
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(null, null, this.scenarioZH.getNetwork());
		filter.setAlternativeAOI(aoiCenter, aoiRadius);
		
		for (Person person : this.scenarioCH.getPopulation().getPersons().values()) {
			if (filter.judge(person)) {
				this.scenarioZH.getPopulation().addPerson(person);
			}
		}
	}
	
	private void runAnalysisZH() {
		for (Person p : this.scenarioZH.getPopulation().getPersons().values()) {
			Plan plan = (Plan) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (act.getType().startsWith(this.type) || this.type.equals("allTypes")) {
						final Activity act1 = act;
						if (PopulationUtils.getPreviousLeg(plan, act1).getMode().equals(this.mode)) {
							Activity previousAct = (Activity) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = CoordUtils.calcEuclideanDistance(previousAct.getCoord(), act.getCoord());
							zh_distanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						}
					}
				}
			}
		}	
		this.zh_distanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m", "#");		
	}
	
	private void runAnalysisCH() {
		for (Person p : this.scenarioCH.getPopulation().getPersons().values()) {
			Plan plan = (Plan) p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (act.getType().startsWith(this.type) || this.type.equals("allTypes")) {
						final Activity act3 = act;
						if (PopulationUtils.getPreviousLeg(plan, act3).getMode().equals(this.mode)) {
							Activity previousAct = (Activity) (plan.getPlanElements().get(plan.getPlanElements().indexOf(act) - 2));
							double distance = CoordUtils.calcEuclideanDistance(previousAct.getCoord(), act.getCoord());
							ch_distanceDistribution.addVal(distance, p.getSelectedPlan().getScore());
						
							if (previousAct.getType().equals("h")) {
								ch_distanceDistributionHomeBased.addVal(distance, p.getSelectedPlan().getScore());	
								
								//check the subsequent activities
								Activity actTmp = act;
								final Activity act1 = actTmp;
								String nextType = PopulationUtils.getNextActivity(plan, PopulationUtils.getNextLeg(plan, act1)).getType();
								while (nextType.startsWith(this.type)  || this.type.equals("allTypes")) {
									final Activity act2 = actTmp;
									actTmp = PopulationUtils.getNextActivity(plan, PopulationUtils.getNextLeg(plan, act2));
									nextType = actTmp.getType();
								}
								if (nextType.equals("h")) {
									this.ch_distanceDistributionHomeBasedRoundTrip.addVal(distance, p.getSelectedPlan().getScore());
									
									if (act.getType().equals("sg")) {
										this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
									else if (act.getType().startsWith("s") && !(act.getType().equals("sg"))){
										this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.addVal(distance, p.getSelectedPlan().getScore());
									}
								}
							}
						}
					}
				}
			}
		}	
		this.ch_distanceDistribution.plotBinnedDistribution(this.outputFolder, "m", "m", "#");
		this.ch_distanceDistributionHomeBased.plotBinnedDistribution(this.outputFolder, "m", "m", "#");
		this.ch_distanceDistributionHomeBasedRoundTrip.plotBinnedDistribution(this.outputFolder, "m", "m", "#");
		if (type.startsWith("s")) {
			this.ch_shoppingDistanceDistributionHomeBasedRoundTripGrocery.plotBinnedDistribution(this.outputFolder, "m", "m", "#");
			this.ch_shoppingDistanceDistributionHomeBasedRoundTripNonGrocery.plotBinnedDistribution(this.outputFolder, "m", "m", "#");
		}		
	}
}
