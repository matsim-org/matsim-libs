/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.config.Config;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Plans;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

import playground.dgrether.roadpricing.RoadPricingUtilities;


/**
 * @author dgrether
 *
 */
public class ActivityDurationAnalyser {

	private static final String runsbase = "/Volumes/data/work/cvsRep/vsp-cvs/runs/";
	
	private static final String studybase = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/";
	
	private static final String network = studybase + "network/ivtch-osm.xml";		
	
	private static final String plansfile1 = runsbase + "run583/run583.it800.plans.xml.gz";
	
	private static final String plansfile2 = runsbase + "run585/run585.it800.plans.xml.gz";
	
	private static final String[] plansFiles = {plansfile1, plansfile2}; //
	
	private static final String configfile = studybase + "configEarlyDeparture.xml";
	
	private static final String roadpricingfile = studybase + "roadpricing/zurichCityArea/zrhCA_dt_rp200_an.xml";
	
	private Config config;

	private RoadPricingScheme roadPricingScheme;
	
	public ActivityDurationAnalyser() {
		NetworkLayer net = new NetworkLayer();
		MatsimNetworkReader reader = new MatsimNetworkReader(net);
		reader.readFile(network);

		config = Gbl.createConfig(new String[] {configfile});
//		config = Gbl.createConfig(null);
		Gbl.getWorld().setNetworkLayer((NetworkLayer) net);

		
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(net);
		try {
			tollReader.parse(roadpricingfile);
			roadPricingScheme = tollReader.getScheme();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		for (String file : plansFiles) {
			Plans plans = new Plans(Plans.NO_STREAMING);
			MatsimPlansReader plansParser = new MatsimPlansReader(plans);
			plansParser.readFile(file);	
			ActivityDurationCounter adc = new ActivityDurationCounter();
			System.out.println("Handling plans: " + file);
			for (Person person : plans) {
				if (!RoadPricingUtilities.hasActInTollArea(person.getSelectedPlan(), this.roadPricingScheme)){
					continue;
				}
      	adc.handlePlan(person.getSelectedPlan());
      }
			
			calculateActivityDurations(adc.getTypeActivityMap());
			calculateActivityDurations(adc.getSimpleTypeActivityMap());
			
			
		}
	}
	
	private void calculateActivityDurations(Map<String, List<Act>> typeActivityMap) {
		System.out.println("Calculating activity durations...");
		System.out.println("activity type \t number of activities \t absolute duration \t average duration" );
		for (List<Act> actList : typeActivityMap.values()) {
			double durations = 0.0;
			double dur, startTime, endTime;
//			System.out.println("Processing activity type: " + actList.get(0).getType());
			for (Act act : actList) {
				dur = act.getDur();
				ActivityParams actParams = config.charyparNagelScoring().getActivityParams(act.getType());
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

	private class ActivityDurationCounter  {

		private Map<String, List<Act>> typeActivityMap; 
		private Map<String, List<Act>> simpleTypeActivityMap; 
		ActivityDurationCounter() {
			typeActivityMap = new HashMap<String, List<Act>>();
			simpleTypeActivityMap = new HashMap<String, List<Act>>();
		}

		public void handlePlan(Plan plan) {
//			System.out.println("handling plan " + typeActivityMap);
			for (Iterator it =  plan.getIteratorAct(); it.hasNext();) {
				Act activity = (Act) it.next();
//				System.out.println("handling act: " + activity.getType());
				List<Act> acts = typeActivityMap.get(activity.getType());
				List<Act> acts2 = simpleTypeActivityMap.get(activity.getType().substring(0,1));
				if (acts == null) {
					acts = new ArrayList<Act>();
					typeActivityMap.put(activity.getType(), acts);
				}
				if (acts2 == null) {
					acts2 = new ArrayList<Act>();
					simpleTypeActivityMap.put(activity.getType().substring(0,1), acts2);
				}
				acts.add(activity);
				acts2.add(activity);
			}
		}
		
		
		public Map<String, List<Act>> getTypeActivityMap() {
			return this.typeActivityMap;
		}

		
		public Map<String, List<Act>> getSimpleTypeActivityMap() {
			return simpleTypeActivityMap;
		}
	};
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ActivityDurationAnalyser();
	}

}
