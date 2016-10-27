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

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
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
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs.LinkWeightMethod;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;
import playground.agarwalamit.analysis.userBenefits.MyUserBenefitsAnalyzer;
import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author amit
 */

public class MunichSpatialPlots {

	private final String runDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/";
	private final String bau = runDir+"/bau";
	private final String policyName = "ei";
	private final String policyCase = runDir+"/"+policyName;
	private final double countScaleFactor = 100;
	private static double gridSize ;
	private static double smoothingRadius ;
	private final boolean isWritingGGPLOTData = true;
	private final int noOfBins = 1;

	private static double xMin=4452550.25;
	private static double xMax=4479483.33;
	private static double yMin=5324955.00;
	private static double yMax=5345696.81;

	private final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");

	private static final String SHAPE_FILE_CITY = "../../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	private static final String SHAPE_FILE_MMA = "../../../../repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";
	private static final boolean isCityArea = false;
	private static String shapeFile;

	public static void main(String[] args) {
		//city area only
		if(isCityArea) {
			ReferencedEnvelope re = GeometryUtils.getBoundingBox(SHAPE_FILE_CITY);
			xMin = re.getMinX();
			xMax = re.getMaxX();
			yMin = re.getMinY();
			yMax = re.getMaxY();
			gridSize = 500;
			smoothingRadius = 500;
			shapeFile = SHAPE_FILE_CITY;
		} else {//metropolitan area
			ReferencedEnvelope re = GeometryUtils.getBoundingBox(SHAPE_FILE_MMA);
			xMin = re.getMinX();
			xMax = re.getMaxX();
			yMin = re.getMinY();
			yMax = re.getMaxY();
			gridSize = 1500;
			smoothingRadius = 2000;
			shapeFile = SHAPE_FILE_MMA;
		}
		MunichSpatialPlots plots = new MunichSpatialPlots();
		//		plots.writeCongestionToCells();
		plots.writeEmissionToCells();
		//		plots.writeUserWelfareToCells();
		//		plots.writePopulationDensityCountToCells();
//		plots.writePersonTollToCells();
		//		plots.writeLinkTollToCells();
	}

	public void writePopulationDensityCountToCells(){

		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.point,bau);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
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
			plot.processLocationForDensityCount(act,countScaleFactor);
			//			plot.processHomeLocation(act, 1*countScaleFactor); // if want to interpolate
		}

		plot.writeRData("popDensity_interpolate",isWritingGGPLOTData);

	}

	public void writeUserWelfareToCells(){

		Map<Id<Person>, Double> personUserWelfareMoneyBau = new HashMap<>();
		Map<Id<Person>, Double> personUserWelfareMoneyPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.point,bau,policyCase);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.initialCasePlansFile,inputs.initialCaseNetworkFile);

		MyUserBenefitsAnalyzer userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
		userBenefitsAnalyzer.init((MutableScenario)sc, WelfareMeasure.SELECTED, false);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();

		personUserWelfareMoneyBau = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();

		if(inputs.isComparing){
			userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
			Scenario scPolicy = LoadMyScenarios.loadScenarioFromPlansAndNetwork(inputs.compareToCasePlans,inputs.compareToCaseNetwork);
			userBenefitsAnalyzer.init((MutableScenario)scPolicy, WelfareMeasure.SELECTED, false);
			userBenefitsAnalyzer.preProcessData();
			userBenefitsAnalyzer.postProcessData();
			personUserWelfareMoneyPolicy = userBenefitsAnalyzer.getPersonId2MonetarizedUserWelfare();
		}

		for(Person p : sc.getPopulation().getPersons().values()){
			Id<Person> id = p.getId();

			Activity act  = sc.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					act = (Activity) pe;
					break;
				}
			}

			if(inputs.isComparing){
				double userWelfareDiff ;
				userWelfareDiff = personUserWelfareMoneyPolicy.get(id) - personUserWelfareMoneyBau.get(id);
				plot.processHomeLocation(act, userWelfareDiff*countScaleFactor);

			} else {
				plot.processHomeLocation(act, countScaleFactor * personUserWelfareMoneyBau.get(id));
			}
		}

		plot.writeRData("userWelfare_"+WelfareMeasure.SELECTED,isWritingGGPLOTData);

	}

	/**
	 * This will write person toll to cell using point method.
	 */
	public void writePersonTollToCells(){
		Map<Id<Person>, Double> personTollPolicy = new HashMap<>();

		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.point,policyCase); //bau do not have toll
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario scPolicy = LoadMyScenarios.loadScenarioFromOutputDir(policyCase);

		personTollPolicy = getPersonIdToTollPayments(scPolicy);

		for(Person p : scPolicy.getPopulation().getPersons().values()){
			Id<Person> id = p.getId();

			Activity act  = scPolicy.getPopulation().getFactory().createActivityFromLinkId("NA", Id.createLinkId("NA"));
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					act = (Activity) pe;
					break;
				}
			}

			/*
			 * personId2toll is stored negative in case of toll (==> showing that person has to pay). Since, here toll is calculated, so taking
			 * negative of stored number. 
			 */
			double processableIntensity = 0;
			if(personTollPolicy.containsKey(id)) processableIntensity = - countScaleFactor * personTollPolicy.get(id);
			else processableIntensity =0;

			plot.processHomeLocation(act, processableIntensity);
		}
		plot.writeRData("tollAtHomeLocation",isWritingGGPLOTData);
	}

	public void writeLinkTollToCells() {
		Map<Id<Link>, Double> linkTollPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line,bau,policyCase);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

		double processIntensity = 0;

		if(inputs.isComparing){
			Scenario scPolicy = LoadMyScenarios.loadScenarioFromOutputDir(policyCase);
			LinkTollFromExternalCosts linktollsAnalyzer = new LinkTollFromExternalCosts();
			linkTollPolicy = linktollsAnalyzer.getLinkToTotalExternalCost(scPolicy, policyName);
		}

		for(Link l : sc.getNetwork().getLinks().values()){

			if(inputs.isComparing){
				processIntensity = linkTollPolicy.get(l.getId());
			} else {
				processIntensity = 0; // for bau --> no toll
			}

			plot.processLink(l, processIntensity*countScaleFactor);

		}
		plot.writeRData("linkTolls",isWritingGGPLOTData);
	}


	public void writeCongestionToCells(){
		Map<Double, Map<Id<Link>, Double>> linkDelaysBau = new HashMap<>();
		Map<Double, Map<Id<Link>, Double>> linkDelaysPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line,bau,policyCase);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
		inputs.setShapeFile("../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/");

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(inputs.initialCaseNetworkFile,inputs.initialCaseConfig);

		ExperiencedDelayAnalyzer delayAnalyzer = new ExperiencedDelayAnalyzer(inputs.initialCaseEventsFile, sc, noOfBins); 
		delayAnalyzer.run();
		linkDelaysBau = delayAnalyzer.getTimeBin2LinkId2Delay();

		if(inputs.isComparing){
			Scenario scCompareTo =LoadMyScenarios.loadScenarioFromNetworkAndConfig(inputs.compareToCaseNetwork,inputs.compareToCaseConfig); 
			delayAnalyzer = new ExperiencedDelayAnalyzer(inputs.compareToCaseEventsFile, scCompareTo, noOfBins);
			delayAnalyzer.run();
			linkDelaysPolicy = delayAnalyzer.getTimeBin2LinkId2Delay();
		}

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
				}
			}
			plot.writeRData("delays_"+(int)time/3600+"h", isWritingGGPLOTData);
			plot.reset();
		}
	}


	public void writeEmissionToCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs(LinkWeightMethod.line,bau, policyCase);
		inputs.setBoundingBox(xMin, xMax, yMin, yMax);
		inputs.setTargetCRS(targetCRS);
		inputs.setGridInfo(GridType.HEX, gridSize);
		inputs.setSmoothingRadius(smoothingRadius);
		inputs.setShapeFile(shapeFile);

//		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/");
		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/", true);

		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEmissionEventsFile, noOfBins);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

		if(inputs.isComparing){
			emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEmissionEventsFile, noOfBins);
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkEmissionsPolicy = emsLnkAna.getLink2TotalEmissions();
		}

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

		EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
		writer.openWriter(runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/"+"viaData_NO2_"+GridType.HEX+"_"+gridSize+"_"+smoothingRadius+"_line_"+policyName+"_diff.txt");

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
					
				}
			}
			writer.writeData(time, plot.getCellWeights());
			//			plot.writeRData("NO2_"+(int)time/3600+"h",isWritingGGPLOTData);
			plot.reset();
		}
		writer.closeWriter();
	}

	private Map<Id<Person>, Double> getPersonIdToTollPayments (final Scenario sc){

		MonetaryPaymentsAnalyzer paymentsAnalzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalzer.init((MutableScenario)sc);
		paymentsAnalzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}

		int lastIteration = sc.getConfig().controler().getLastIteration();

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(sc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");

		paymentsAnalzer.postProcessData();
		return paymentsAnalzer.getPersonId2amount();
	}

	private class EmissionTimebinDataWriter{

		BufferedWriter writer;
		public void openWriter (final String outputFile){
			writer = IOUtils.getBufferedWriter(outputFile);
			try {
				writer.write("timebin\t centroidX \t centroidY \t weight \n");
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void writeData(final double timebin, final Map<Point,Double> cellWeights){
			try {
				for(Point p : cellWeights.keySet()){
					writer.write(timebin+"\t"+p.getCentroid().getX()+"\t"+p.getCentroid().getY()+"\t"+cellWeights.get(p)+"\n");
				}
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}

		public void closeWriter (){
			try {
				writer.close();	
			} catch (Exception e) {
				throw new RuntimeException("Data is not written to file. Reason "+e);
			}
		}
	}
}