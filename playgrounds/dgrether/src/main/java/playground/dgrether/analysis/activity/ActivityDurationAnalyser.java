/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.roadpricing.RoadPricingScheme;


/**
 * @author dgrether
 *
 */
public class ActivityDurationAnalyser {

	private static final String runsbase = "/Volumes/data/work/cvsRep/vsp-cvs/runs/";

	private static final String studybase = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/";

	private static final String network = studybase + "network/ivtch-osm.xml";

	//ersa runs plans files
//	private static final String plansfile1 = runsbase + "run583/run583.it800.plans.xml.gz";
//
//	private static final String plansfile2 = runsbase + "run585/run585.it800.plans.xml.gz";

	//early departure studies plan files
//	private static final String plansfile1 = runsbase + "run495/it.500/500.plans.xml.gz";

//	private static final String plansfile2 = runsbase + "run499/it.500/500.plans.xml.gz";
//	no early departure studies plan files
	private static final String plansfile1 = runsbase + "run639/it.1000/1000.plans.xml.gz";

	private static final String plansfile2 = runsbase + "run640/it.1000/1000.plans.xml.gz";


	private static final String[] plansFiles = {plansfile1, plansfile2}; //

	private static final String configfile = studybase + "configEarlyDeparture.xml";

	private static final String roadpricingfile = studybase + "roadpricing/zurichCityArea/zrhCA_dt_rp200_an.xml";

	private final Config config;

	private RoadPricingScheme roadPricingScheme;

	public ActivityDurationAnalyser() {
		this.config = ConfigUtils.loadConfig(configfile);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(this.config);
		//reading network
//		NetworkLayer net = scenario.getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
		reader.readFile(network);

		//reading road pricing scheme for filtering
//		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(net);
//		try {
//			tollReader.parse(roadpricingfile);
//			roadPricingScheme = tollReader.getScheme();
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		//reading plans, filter and calculate activity durations
		for (String file : plansFiles) {
			Population plans = scenario.getPopulation();
			PopulationReader plansParser = new PopulationReader(scenario);
			plansParser.readFile(file);
			ActivityDurationCounter adc = new ActivityDurationCounter();
			System.out.println("Handling plans: " + file);
			for (Person person : plans.getPersons().values()) {
//				if (!RoadPricingUtilities.hasActInTollArea(person.getSelectedPlan(), this.roadPricingScheme)){
//					continue;
//				}
      	adc.handlePlan(person.getSelectedPlan());
      }

			calculateActivityDurations(adc.getTypeActivityMap());
			calculateActivityDurations(adc.getSimpleTypeActivityMap());
		}
	}

	private void calculateActivityDurations(final Map<String, List<Activity>> typeActivityMap) {
		System.out.println("Calculating activity durations...");
		System.out.println("activity type \t number of activities \t absolute duration \t average duration" );
		for (List<Activity> actList : typeActivityMap.values()) {
			double durations = 0.0;
			double dur, startTime, endTime;
//			System.out.println("Processing activity type: " + actList.get(0).getType());
			for (Activity act : actList) {
				dur = act.getMaximumDuration();
				ActivityParams actParams = this.config.planCalcScore().getActivityParams(act.getType());
				if (!(Double.isInfinite(dur) || Double.isNaN(dur))) {
					if (act.getStartTime() < actParams.getOpeningTime()) {
						startTime = actParams.getOpeningTime();
					}
					else {
						startTime = act.getStartTime();
					}

					if (act.getEndTime() > actParams.getClosingTime()) {
						endTime = actParams.getClosingTime();
					}
					else {
						endTime = act.getEndTime();
					}
					if (Double.isInfinite(endTime) || Double.isNaN(endTime)) {
						endTime = 24.0 * 3600.0;
					}
					if (Double.isInfinite(startTime) || Double.isNaN(startTime)) {
						startTime = 0.0;
					}

					durations += (endTime - startTime);
				}
			}
			if (actList.size() != 0) {
				System.out.println(actList.get(0).getType() + "\t" + actList.size() + "\t" + durations + "\t" + durations / actList.size());
			}
			else {
				System.out.println(actList.get(0).getType() + "\t" + actList.size() + "\t" + durations + "\t" + 0);
			}
		}

	}

	/**
	 *
	 * @author dgrether
	 *
	 */
	private class ActivityDurationCounter  {

		private final Map<String, List<Activity>> typeActivityMap;
		private final Map<String, List<Activity>> simpleTypeActivityMap;

		ActivityDurationCounter() {
			this.typeActivityMap = new HashMap<String, List<Activity>>();
			this.simpleTypeActivityMap = new HashMap<String, List<Activity>>();
		}

		public void handlePlan(final Plan plan) {
//			System.out.println("handling plan " + typeActivityMap);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity) pe;
//				System.out.println("handling act: " + activity.getType());
					List<Activity> acts = this.typeActivityMap.get(activity.getType());
					List<Activity> acts2 = this.simpleTypeActivityMap.get(activity.getType().substring(0,1));
					if (acts == null) {
						acts = new ArrayList<Activity>();
						this.typeActivityMap.put(activity.getType(), acts);
					}
					if (acts2 == null) {
						acts2 = new ArrayList<Activity>();
						this.simpleTypeActivityMap.put(activity.getType().substring(0,1), acts2);
					}
					acts.add(activity);
					acts2.add(activity);
				}
			}
		}

		public Map<String, List<Activity>> getTypeActivityMap() {
			return this.typeActivityMap;
		}

		public Map<String, List<Activity>> getSimpleTypeActivityMap() {
			return this.simpleTypeActivityMap;
		}
	}


	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		new ActivityDurationAnalyser();
	}

}
