/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.scenario.ScenarioImpl;

import playground.agarwalamit.analysis.congestion.CongestionLinkAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;
import playground.agarwalamit.analysis.userBenefits.MyUserBenefitsAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class MunichSpatialPlots {

	String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";
	String bau = runDir+"/bau";
	String policyScenario = runDir+"/ei";
	private double countScaleFactor = 100;

	public static void main(String[] args) {
		MunichSpatialPlots plots = new MunichSpatialPlots();
//		plots.writeCongestionToCells();
//		plots.writeEmissionToCells();
		plots.writeUserWelfareToCells();
		plots.writePopulationDensityCountToCells();
	}

	public void writePopulationDensityCountToCells(){

		SpatialDataInputs inputs = new SpatialDataInputs("point",bau);
		inputs.setGridInfo(GridType.HEX, 500);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.initialCasePlansFile,inputs.initialCaseNetworkFile);

		for(Person p : sc.getPopulation().getPersons().values()){
			
			Activity act  = sc.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
			
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					act = (Activity) pe;
					break;
				}
			}
			plot.processLocationForDensityCount(act); //this is only for sample population, others are already 
		}

		plot.writeRData("popDensity");

	}

	public void writeUserWelfareToCells(){

		Map<Id<Person>, Double> person_userWElfare_money_Bau = new HashMap<>();
		Map<Id<Person>, Double> person_userWElfare_money_policy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("point",bau,policyScenario);
		inputs.setGridInfo(GridType.HEX, 500);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.initialCasePlansFile,inputs.initialCaseNetworkFile);

		MyUserBenefitsAnalyzer userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
		userBenefitsAnalyzer.init((ScenarioImpl)sc, WelfareMeasure.SELECTED, false);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();

		person_userWElfare_money_Bau = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();

		if(inputs.isComparing){
			userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
			Scenario sc_policy = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.compareToCasePlans,inputs.compareToCaseNetwork);
			userBenefitsAnalyzer.init((ScenarioImpl)sc_policy, WelfareMeasure.SELECTED, false);
			userBenefitsAnalyzer.preProcessData();
			userBenefitsAnalyzer.postProcessData();
			person_userWElfare_money_policy = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
		}

		for(Person p : sc.getPopulation().getPersons().values()){
			Id<Person> id = p.getId();

			if(inputs.isComparing){

				Activity act  = sc.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
					if(pe instanceof Activity){
						act = (Activity) pe;
						break;
					}
				}

				double userWelfare_diff ;

				userWelfare_diff = person_userWElfare_money_policy.get(id) - person_userWElfare_money_Bau.get(id);

				plot.processHomeLocation(act, userWelfare_diff*countScaleFactor);

			}
		}

		plot.writeRData("userWelfare_"+WelfareMeasure.SELECTED);

	}

	public void writeCongestionToCells(){
		Map<Double, Map<Id<Link>, Double>> linkDelaysBau = new HashMap<>();
		Map<Double, Map<Id<Link>, Double>> linkDelaysPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("line",bau);
		inputs.setGridInfo(GridType.HEX, 500);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

		CongestionLinkAnalyzer emsLnkAna = new CongestionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEventsFile, 1); 
		emsLnkAna.init(sc);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkDelaysBau = emsLnkAna.getCongestionPerLinkTimeInterval();

		if(inputs.isComparing){
			emsLnkAna = new CongestionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEventsFile, 1);
			emsLnkAna.init(LoadMyScenarios.loadScenarioFromNetwork(inputs.compareToCaseNetwork));
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkDelaysPolicy = emsLnkAna.getCongestionPerLinkTimeInterval();
		}
		double sumDelays =0;

		for(double time :linkDelaysBau.keySet()){
			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();

				if(plot.isInResearchArea(l)){

					double delays = 0;

					if(inputs.isComparing){

						double linkDelayBau =0;
						double linkDelayPolicy =0;

						if(linkDelaysBau.get(time).containsKey(id) && linkDelaysPolicy.get(time).containsKey(id)) {
							linkDelayBau = countScaleFactor * linkDelaysBau.get(time).get(id);
							linkDelayPolicy = countScaleFactor * linkDelaysPolicy.get(time).get(id);
						} else if(linkDelaysBau.get(time).containsKey(id)){
							linkDelayBau = countScaleFactor * linkDelaysBau.get(time).get(id);
						} else if(linkDelaysPolicy.get(time).containsKey(id)){
							linkDelayPolicy = countScaleFactor * linkDelaysPolicy.get(time).get(id);
						}
						delays = linkDelayPolicy - linkDelayBau;

					} else {

						if(linkDelaysBau.get(time).containsKey(id)) delays = countScaleFactor * linkDelaysBau.get(time).get(id);
						else delays =0;
					}

					plot.processLink(l,  delays);
					sumDelays += (delays);
				}
			}
		}

		plot.writeRData("delays");
		SpatialDataInputs.LOG.info("Total delays from link emission map is "+sumDelays);

		double cellWeights =0;
		for(Point p: plot.getCellWeights().keySet()){
			cellWeights += plot.getCellWeights().get(p);
		}
		SpatialDataInputs.LOG.info("Total delays from cell weights  is "+cellWeights);

	}


	public void writeEmissionToCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("line",bau);
		inputs.setGridInfo(GridType.HEX, 500);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		// set bounding box, smoothing radius and targetCRS if different.
		//		inputs.setTargetCRS(MGC.getCRS("EPSG:20004"));
		//		inputs.setBoundingBox(4452550.25, 4479483.33, 5324955.00, 5345696.81);
		//		inputs.setSmoothingRadius(500.);

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEmissionEventsFile, 1);
		emsLnkAna.init();
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

		if(inputs.isComparing){
			emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEmissionEventsFile, 1);
			emsLnkAna.init();
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkEmissionsPolicy = emsLnkAna.getLink2TotalEmissions();
		}


		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);
		double sumEmission =0;

		for(double time :linkEmissionsBau.keySet()){
			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();

				if(plot.isInResearchArea(l)){

					double emiss = 0;

					if(inputs.isComparing){

						double linkEmissionBau =0;
						double linkEmissionPolicy =0;

						if(linkEmissionsBau.get(time).containsKey(id) && linkEmissionsPolicy.get(time).containsKey(id)) {
							linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
							linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
						} else if(linkEmissionsBau.get(time).containsKey(id)){
							linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
						} else if(linkEmissionsPolicy.get(time).containsKey(id)){
							linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
						}
						emiss = linkEmissionPolicy - linkEmissionBau;

					} else {

						if(linkEmissionsBau.get(time).containsKey(id)) emiss = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
						else emiss =0;
					}

					plot.processLink(l,  emiss);
					sumEmission += (emiss);
				}
			}
		}

		plot.writeRData("NO2");
		SpatialDataInputs.LOG.info("Total NO2 emissions from link emission map is "+sumEmission);

		double cellWeights =0;
		for(Point p: plot.getCellWeights().keySet()){
			cellWeights += plot.getCellWeights().get(p);
		}
		SpatialDataInputs.LOG.info("Total NO2 emissions from cell weights  is "+cellWeights);

	}

}
