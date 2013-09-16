/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.julia.welfare;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.vsp.analysis.modules.userBenefits.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;

/**
 * @author benjamin, julia
 *
 */
public class SpatialAveragingWelfare {
	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);

//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "baseCase";
//	private final static String runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
////	private final static String runNumber2 = "zone30";
////	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
//	private final static String runNumber2 = "pricing";
//	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_pricing_newCode/";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
//	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + lastIteration2 + ".emission.events.xml.gz";
	
	final double scalingFactor = 10.; //TODO scalingFactor????
	private final static String runNumber1 = "981";
	private final static String runNumber2 = "983";
	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private final String plansFile1 = runDirectory1 + runNumber1 + ".output_plans.xml";
	private final String plansFile2 = runDirectory2 + runNumber2 + ".output_plans.xml.gz";

	Network network;
	Collection<SimpleFeature> featuresInMunich;
	double simulationEndTime;
	String outPathStub;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;
	
	final double area_in_smoothing_circle_sqkm = (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) / (1000. * 1000.);

	final int noOfTimeBins = 1;
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final boolean compareToBaseCase = true;

	private double[][] weights = new double[noOfXbins][noOfYbins];

	private void run() throws IOException{
		
		//initialize scenario, set outpath, read plans file
		this.simulationEndTime = getEndTime(configFile1);
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		
		this.featuresInMunich = ShapeFileReader.getAllFeatures(munichShapeFile);
		Config config = scenario.getConfig();
		
		outPathStub = runDirectory1 + runNumber1 + "." + lastIteration1;
		String outPath = outPathStub + "_Routput_" + "UserBenefits.txt";
		
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.readFile(plansFile1);
		logger.info("Finished reading the plans file " + plansFile1);
		
		// create population and calculate user benefit for base case
		Population pop = scenario.getPopulation();
		
		logger.info("Starting user benefits calculation.");
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM);
		ubc.calculateUtility_money(pop);
		
		Map<Id, Double> personId2Utility = ubc.getPersonId2Utility();
		logger.info("Finished user benefits calculation.");
		
		double [][] userBenefits = fillUserBenefits(personId2Utility, pop);
		logger.info("Finished averaging.");
		double [][] average = calculateAverage(userBenefits, weights);
		
		// write results to text files
		writeRoutput(userBenefits, outPath + ".txt");
		writeRoutput(weights, outPathStub+"_Routput_"+"UserBenefitWeights.txt");
		writeRoutput(average, outPathStub+"_Routput_"+"AverageUserBenefits.txt");
		
		// 
		if(compareToBaseCase){
			// initialize scenario for comparison case
			Scenario scenario2 = loadScenario(netFile1);
			Config config2 = scenario2.getConfig();
			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
			mpr2.readFile(plansFile2);
			logger.info("Finished reading the plans file " + plansFile2);
			
			// create population and calculate user benefit for comparison case
			Population pop2 = scenario2.getPopulation();
			
			logger.info("Starting user benefits calculation.");
			UserBenefitsCalculator ubc2 = new UserBenefitsCalculator(config2, WelfareMeasure.LOGSUM);
			ubc2.calculateUtility_money(pop2);
			
			Map<Id, Double> personId2Utility2 = ubc2.getPersonId2Utility();
			System.out.println("Finished user benefits calculation.");
			
			// calculate differences base case <-> comparison case
			Map<Id, Double> absoluteDifferences = new HashMap<Id, Double>();
			for(Id id: personId2Utility.keySet()){
				if (personId2Utility2.containsKey(id)) {
					absoluteDifferences.put
					(id, personId2Utility2.get(id)- personId2Utility.get(id));
				}
				else{
					logger.warn("Person " + id.toString() +" does not appear in the second scenario.");
				}
			}
			double [][] userBenefitDifferences = fillUserBenefits(absoluteDifferences, pop);
			average = calculateAverage(userBenefitDifferences, weights);
			
			// set output paths and write results to text files
			String outPath2 = outPathStub + "_Routput_" + "UserBenefitsDifferences.txt";
			String outPath3 = outPathStub + "_Routput_" + "UserBenefitsCountsDifferences.txt";
			String outPath4 = outPathStub + "_Routput_" + "UserBenefitsAverageDifferences.txt";
			writeRoutput(userBenefitDifferences, outPath2);
			writeRoutput(weights, outPath3);
			writeRoutput(average, outPath4);
		}
		


	}
	
	private double[][] calculateAverage(double[][] userBenefits,
			double[][] weights2) {
		double[][] average = new double [noOfXbins][noOfYbins];
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				if(weights2[xIndex][yIndex]>0){
					average[xIndex][yIndex]= userBenefits[xIndex][yIndex]/weights2[xIndex][yIndex];
				}
			}
		}
		return average;
	}

	private void writeRoutput(double[][] results, String outputPathForR) {
		try {
			logger.info(outputPathForR);
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString = "\t";
			logger.info("routput 2");
			
			//x-coordinates as first row
			for(int xIndex = 0; xIndex < results.length; xIndex++){
				valueString += findBinCenterX(xIndex) + "\t";
			}
			buffW.write(valueString);
			buffW.newLine();
			valueString = new String();
			
			for(int yIndex = 0; yIndex < results[0].length; yIndex++){
				//y-coordinates as first column
				valueString += findBinCenterY(yIndex) + "\t";
				//table contents
				for(int xIndex = 0; xIndex < results.length; xIndex++){ 
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						if(isInMunich(cellCentroid)){
							valueString += Double.toString(results[xIndex][yIndex]) + "\t"; 
						} else {
							valueString += "NA" + "\t";
						}
				}
				buffW.write(valueString);
				buffW.newLine();
				valueString = new String();
			}
		buffW.close();	
		} catch (IOException e) {
			throw new RuntimeException("Failed writing output for R.");
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
	}

	private double [][] fillUserBenefits(Map<Id, Double> personId2Utility, Population pop){
		double[][] weightedBenefits = new double [noOfXbins][noOfYbins];
		for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
				weightedBenefits[xIndex][yIndex]=0.0;
				weights[xIndex][yIndex]=.0;
			}
		}
		int count =1;
		for(Id personId : personId2Utility.keySet()){ // das hier braucht so lange. 
			
				if(count%1000==0)logger.info("count = " + count);
				Person person = pop.getPersons().get(personId);
				Plan plan = person.getSelectedPlan();
				Coord homeCoord = new CoordImpl(.0, .0);
				for (PlanElement planElement : plan.getPlanElements()) {
						Activity activity = (Activity) planElement;
						homeCoord = activity.getCoord();
						if(homeCoord!=null)	break;
				}
				if ((homeCoord!=null) && (isInResearchArea(homeCoord))) {
					weightedBenefits = calculatePersonalInfluencePerBin(
							weightedBenefits, personId2Utility.get(personId),
							homeCoord);
				}
				
				count++;
		}
		
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				weightedBenefits[xIndex][yIndex]=weightedBenefits[xIndex][yIndex]*scalingFactor/area_in_smoothing_circle_sqkm;
				weights[xIndex][yIndex]= weights[xIndex][yIndex]*scalingFactor/area_in_smoothing_circle_sqkm;
			}
		}
		return weightedBenefits;
	}

	
	private double[][] calculatePersonalInfluencePerBin(
			double[][] weightedBenefits, Double value, Coord homeCoord) {
		
			for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
					Coord cellCentroid = findCellCentroid(xIndex, yIndex);
					double weightForCurrentBin = calculateWeightOfLinkForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
					if (value != null) {
						weightedBenefits[xIndex][yIndex] += weightForCurrentBin * value;
						weights[xIndex][yIndex]+= weightForCurrentBin;
					}else{
						logger.warn("current value is null");
					}
					
				}
			}
		return weightedBenefits;
	}
	
	private boolean isInMunich(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInMunich){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}

	private boolean isInResearchArea(Coord linkCoord) {
		Double xLink = linkCoord.getX();
		Double yLink = linkCoord.getY();
		
		if(xLink > xMin && xLink < xMax){
			if(yLink > yMin && yLink < yMax){
				return true;
			}
		}
		return false;
	}

	private double calculateWeightOfLinkForCell(double x1, double y1, double x2, double y2) {
		double distanceSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
		return Math.exp((-distanceSquared) / (smoothinRadiusSquared_m));
	}
	
	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin);
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin);
		Assert.equals(mapXCoordToBin(xBinCenter), xIndex);
		return xBinCenter ;
	}

	private Coord findCellCentroid(int xIndex, int yIndex) {
		double xCentroid = findBinCenterX(xIndex);
		double yCentroid = findBinCenterY(yIndex);
		Coord cellCentroid = new CoordImpl(xCentroid, yCentroid);
		return cellCentroid;
	}

	private Integer mapYCoordToBin(double yCoord) {
		if (yCoord <= yMin || yCoord >= yMax) return null; // yCoord is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.getQSimConfigGroup().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	private static Integer getLastIteration(String configFile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingWelfare().run();
	}
}