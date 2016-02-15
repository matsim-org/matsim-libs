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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs.LinkWeightMethod;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;
import playground.agarwalamit.analysis.userBenefits.MyUserBenefitsAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.utils.spatialAvg.LinkLineWeightUtil;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class KrasnojarskSpatialPlots {

	String runDir = "../../runs-svn/krasnojarsk/";
	String bau = runDir + "bau";
//	String policyName = "ei";
//	String policyCase = runDir + policyName;
	private final double countScaleFactor = 10;
	private final double gridSize = 250;
	private boolean isWritingGGPLOTData = false;
	private int noOfBins = 1;

	public static void main(String[] args) {
		KrasnojarskSpatialPlots plots = new KrasnojarskSpatialPlots();
		//      plots.writeCongestionToCells();
		plots.writeEmissionToCells();
		//		plots.writeUserWelfareToCells();
		//		plots.writePopulationDensityCountToCells();
		//		plots.writePersonTollToCells();
		//		plots.writeLinkTollToCells();
	}

//	public void writePopulationDensityCountToCells(){
//
//		SpatialDataInputs inputs = new SpatialDataInputs("point", bau);
//		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");
//
//		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/");
//
//		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.initialCasePlansFile,inputs.initialCaseNetworkFile);
//
//		for(Person p : sc.getPopulation().getPersons().values()){
//
//			Activity act  = sc.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
//
//			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					act = (Activity) pe;
//					break;
//				}
//			}
//			//			plot.processLocationForDensityCount(act,countScaleFactor);
//			plot.processHomeLocation(act, 1*countScaleFactor); // if want to interpolate
//		}
//
//		plot.writeRData("popDensity_interpolate",isWritingGGPLOTData);
//
//	}

//	public void writeUserWelfareToCells(){
//
//		Map<Id<Person>, Double> person_userWElfare_money_Bau = new HashMap<>();
//		Map<Id<Person>, Double> person_userWElfare_money_policy = new HashMap<>();
//
//		// setting of input data
//		SpatialDataInputs inputs = new SpatialDataInputs("point",bau,policyCase);
//		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");
//
//		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/");
//
//		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.initialCasePlansFile,inputs.initialCaseNetworkFile);
//
//		MyUserBenefitsAnalyzer userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
//		userBenefitsAnalyzer.init((ScenarioImpl)sc, WelfareMeasure.SELECTED, false);
//		userBenefitsAnalyzer.preProcessData();
//		userBenefitsAnalyzer.postProcessData();
//
//		person_userWElfare_money_Bau = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
//
//		if(inputs.isComparing){
//			userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
//			Scenario sc_policy = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.compareToCasePlans,inputs.compareToCaseNetwork);
//			userBenefitsAnalyzer.init((ScenarioImpl)sc_policy, WelfareMeasure.SELECTED, false);
//			userBenefitsAnalyzer.preProcessData();
//			userBenefitsAnalyzer.postProcessData();
//			person_userWElfare_money_policy = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
//		}
//
//		for(Person p : sc.getPopulation().getPersons().values()){
//			Id<Person> id = p.getId();
//
//
//			Activity act  = sc.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
//			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					act = (Activity) pe;
//					break;
//				}
//			}
//
//			if(inputs.isComparing){
//				double userWelfare_diff ;
//				userWelfare_diff = person_userWElfare_money_policy.get(id) - person_userWElfare_money_Bau.get(id);
//				plot.processHomeLocation(act, userWelfare_diff*countScaleFactor);
//
//			} else {
//				plot.processHomeLocation(act, countScaleFactor * person_userWElfare_money_Bau.get(id));
//			}
//		}
//
//		plot.writeRData("userWelfare_"+WelfareMeasure.SELECTED,isWritingGGPLOTData);
//
//	}

//	/**
//	 * This will write person toll to cell using point method.
//	 */
//	public void writePersonTollToCells(){
//		Map<Id<Person>, Double> personTollPolicy = new HashMap<>();
//
//		SpatialDataInputs inputs = new SpatialDataInputs("point",policyCase); //bau do not have toll
//		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");
//
//		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/");
//
//		Scenario scPolicy = LoadMyScenarios.loadScenarioFromOutputDir(policyCase);
//
//		personTollPolicy = getPersonIdToTollPayments(scPolicy);
//
//		for(Person p : scPolicy.getPopulation().getPersons().values()){
//			Id<Person> id = p.getId();
//
//			Activity act  = scPolicy.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
//			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					act = (Activity) pe;
//					break;
//				}
//			}
//
//			/*
//			 * personId2toll is stored negative in case of toll (==> showing that person has to pay). Since, here toll is calculated, so taking
//			 * negative of stored number. 
//			 */
//			double processableIntensity = 0;
//			if(personTollPolicy.containsKey(id)) processableIntensity = - countScaleFactor * personTollPolicy.get(id);
//			else processableIntensity =0;
//
//			plot.processHomeLocation(act, processableIntensity);
//		}
//		plot.writeRData("tollAtHomeLocation",isWritingGGPLOTData);
//	}

//	public void writeLinkTollToCells() {
//		Map<Id<Link>, Double> linkTollPolicy = new HashMap<>();
//
//		// setting of input data
//		SpatialDataInputs inputs = new SpatialDataInputs("line",bau,policyCase);
//		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");
//
//		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/");
//
//		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);
//
//		double processIntensity = 0;
//
//		if(inputs.isComparing){
//			Scenario scPolicy = LoadMyScenarios.loadScenarioFromOutputDir(policyCase);
//			LinkTollFromExternalCosts linktollsAnalyzer = new LinkTollFromExternalCosts();
//			linkTollPolicy = linktollsAnalyzer.getLinkToTotalExternalCost(scPolicy, policyName);
//		}
//
//		for(Link l : sc.getNetwork().getLinks().values()){
//
//			if(inputs.isComparing){
//				processIntensity = linkTollPolicy.get(l.getId());
//			} else {
//				processIntensity = 0; // for bau --> no toll
//			}
//
//			plot.processLink(l, processIntensity*countScaleFactor);
//
//		}
//		plot.writeRData("linkTolls",isWritingGGPLOTData);
//	}


	public void writeCongestionToCells(){
		Map<Double, Map<Id<Link>, Double>> linkDelaysBau = new HashMap<>();
		Map<Double, Map<Id<Link>, Double>> linkDelaysPolicy = new HashMap<>();

		// setting of input data
//		SpatialDataInputs inputs = new SpatialDataInputs("line",bau,policyCase);
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.point, bau);
		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/"+noOfBins+"timeBins");

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(inputs.initialCaseNetworkFile,inputs.initialCaseConfig);

		ExperiencedDelayAnalyzer delayAnalyzer = new ExperiencedDelayAnalyzer(inputs.initialCaseEventsFile, sc, noOfBins, sc.getConfig().qsim().getEndTime()); 
		delayAnalyzer.run();
		linkDelaysBau = delayAnalyzer.getTimeBin2LinkId2Delay();

		if(inputs.isComparing){
			Scenario scCompareTo = LoadMyScenarios.loadScenarioFromNetworkAndConfig(inputs.compareToCaseNetwork,inputs.compareToCaseConfig);
			delayAnalyzer = new ExperiencedDelayAnalyzer(inputs.compareToCaseEventsFile, scCompareTo, noOfBins, sc.getConfig().qsim().getEndTime());
			delayAnalyzer.run();
			linkDelaysPolicy = delayAnalyzer.getTimeBin2LinkId2Delay();
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
			plot.writeRData("delays_"+(int)time/3600+"h", isWritingGGPLOTData);
			SpatialDataInputs.LOG.info("Total delays from link emission map is "+sumDelays);

			double cellWeights =0;
			for(Point p: plot.getCellWeights().keySet()){
				cellWeights += plot.getCellWeights().get(p);
			}
			SpatialDataInputs.LOG.info("Total delays from cell weights  is "+cellWeights);
			plot.reset();
			sumDelays=0;
		}
	}


	public void writeEmissionToCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();

		// setting of input data
//		SpatialDataInputs inputs = new SpatialDataInputs("line",bau, policyCase);
//		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.point,bau);
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line,bau);
		inputs.setGridInfo(GridType.HEX, gridSize);
//		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		// set bounding box, smoothing radius and targetCRS if different.
		inputs.setTargetCRS(MGC.getCRS("EPSG:32646"));
//		inputs.setBoundingBox(458823.0, 544945.0, 6190162.0, 6248014.0);
//		inputs.setBoundingBox(480236.0, 511199.0, 6197894.0, 6216748.0);
//		inputs.setBoundingBox(483772.0, 504220.0, 6198650.0, 6213566.0);
		inputs.setBoundingBox(483895.0, 502066.0, 6202213.0, 6212963.0);
		//		inputs.setSmoothingRadius(500.);

		SpatialInterpolation plot = new SpatialInterpolation(inputs, bau + "/analysis/spatialPlots/"+noOfBins+"timeBins");

		String emissionEventsFile = bau+"/emission.events.offline-10.xml.gz";
		String networkFile = bau+"/network.xml";
		
		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), emissionEventsFile, noOfBins);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

		if(inputs.isComparing){
			emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEmissionEventsFile, noOfBins);
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkEmissionsPolicy = emsLnkAna.getLink2TotalEmissions();
		}

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
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
			plot.writeRData("NO2_"+(int)time/3600+"h",isWritingGGPLOTData);
			SpatialDataInputs.LOG.info("Total NO2 emissions from link emission map is "+sumEmission);

			double cellWeights =0;
			for(Point p: plot.getCellWeights().keySet()){
				cellWeights += plot.getCellWeights().get(p);
			}
			SpatialDataInputs.LOG.info("Total NO2 emissions from cell weights  is "+cellWeights);
			plot.reset();
			sumEmission=0;
		}
	}

//	private Map<Id<Person>, Double> getPersonIdToTollPayments (Scenario sc){
//
//		MonetaryPaymentsAnalyzer paymentsAnalzer = new MonetaryPaymentsAnalyzer();
//		paymentsAnalzer.init((ScenarioImpl)sc);
//		paymentsAnalzer.preProcessData();
//
//		EventsManager events = EventsUtils.createEventsManager();
//		List<EventHandler> handler = paymentsAnalzer.getEventHandler();
//
//		for(EventHandler eh : handler){
//			events.addHandler(eh);
//		}
//
//		int lastIteration = sc.getConfig().controler().getLastIteration();
//
//		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile(sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");
//
//		paymentsAnalzer.postProcessData();
//		return paymentsAnalzer.getPersonId2amount();
//	}

}
