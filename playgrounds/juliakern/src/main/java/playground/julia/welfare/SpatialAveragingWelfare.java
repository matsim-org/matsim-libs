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

import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;

/**
 * @author julia, benjamin
 *
 */
public class SpatialAveragingWelfare {
	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);

	final double scalingFactor = 100.;
	private final static String runNumber1 = "baseCase";
	private final static String runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
//	private final static String runNumber2 = "zone30";
//	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
	private final static String runNumber2 = "pricing";
	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_pricing_newCode/";
	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String plansFile1 = runDirectory1 + "output_plans.xml.gz";
	private final String plansFile2 = runDirectory2 + "output_plans.xml.gz";
	
//	final double scalingFactor = 10.;
//	private final static String runNumber1 = "981";
//	private final static String runNumber2 = "983";
//	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
//	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
//	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

//	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
//	private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private final String plansFile1 = runDirectory1 + runNumber1 + ".output_plans.xml";
//	private final String plansFile2 = runDirectory2 + runNumber2 + ".output_plans.xml.gz";

	Collection<SimpleFeature> featuresInMunich;
	Network network;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;
	
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 1000.;
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final double area_in_smoothing_circle_sqkm = (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) / (1000. * 1000.);
	final boolean compareToBaseCase = true;

	String outPathStub = runDirectory1 + "analysis/spatialAveraging/welfare/";

	private void run() throws IOException{
		this.featuresInMunich = ShapeFileReader.getAllFeatures(munichShapeFile);

		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		
		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
		mpr.readFile(plansFile1);
		
		Config config = scenario.getConfig();
		Population pop = scenario.getPopulation();
		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM);
		ubc.calculateUtility_money(pop);
		Map<Id, Double> personId2Utility = ubc.getPersonId2MonetizedUtility();
		logger.info("There were " + ubc.getNoValidPlanCnt() + " persons without any valid plan.");
		
		double [][] weightsBaseCase = calculateWeights(personId2Utility, pop);
		double [][] normalizedWeightsBaseCase = normalizeArray(weightsBaseCase);
		
		double [][] userBenefitsBaseCase = fillUserBenefits(personId2Utility, pop);
		double [][] normalizedUserBenefitsBaseCase = normalizeArray(userBenefitsBaseCase);
		
		double [][] averageUserBenefitsBaseCase = calculateAverage(normalizedUserBenefitsBaseCase, normalizedWeightsBaseCase);
		
		writeRoutput(normalizedUserBenefitsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefits.txt");
		writeRoutput(averageUserBenefitsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefitsAverage.txt");
//		writeRoutput(normalizedWeightsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "NormalizedWeightsBaseCase.txt");
		
		// 
		if(compareToBaseCase){
			Scenario scenario2 = loadScenario(netFile1);
			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
			mpr2.readFile(plansFile2);
			
			Config config2 = scenario2.getConfig();
			Population pop2 = scenario2.getPopulation();
			UserBenefitsCalculator ubc2 = new UserBenefitsCalculator(config2, WelfareMeasure.LOGSUM);
			ubc2.calculateUtility_money(pop2);
			Map<Id, Double> personId2Utility2 = ubc2.getPersonId2MonetizedUtility();
			logger.info("There were " + ubc2.getNoValidPlanCnt() + " persons without any valid plan.");
			
			double [][] weightsPolicyCase = calculateWeights(personId2Utility2, pop2);
			double [][] normalizedWeightsPolicyCase = normalizeArray(weightsPolicyCase);
			
			double [][] userBenefitsPolicyCase = fillUserBenefits(personId2Utility2, pop2);
			double [][] normalizedUserBenefitsPolicyCase = normalizeArray(userBenefitsPolicyCase);
			
			double [][] averageUserBenefitsPolicyCase = calculateAverage(normalizedUserBenefitsPolicyCase, normalizedWeightsPolicyCase);
			
			// calculate differences base case <-> policy case
			double [][] normalizedUserBenefitDifferences = calculateDifferences(normalizedUserBenefitsPolicyCase, normalizedUserBenefitsBaseCase);
			double [][] normalizedAverageUserBenefitDifferences = calculateDifferences(averageUserBenefitsPolicyCase, averageUserBenefitsBaseCase);
//			double [][] normalizedWeightsDifferences = calculateDifferences(normalizedWeightsPolicyCase, normalizedWeightsBaseCase);
			
			writeRoutput(normalizedUserBenefitDifferences, outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefitsDifferences.txt");
			writeRoutput(normalizedAverageUserBenefitDifferences, outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefitsAverageDifferences.txt");
//			writeRoutput(normalizedWeightsDifferences, outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput_" + "WeightsDifferences.txt"); // should be zero
		}
	}
	
	private double[][] normalizeArray(double[][] array) {
		double [][] normalizedArray = new double[noOfXbins][noOfYbins];
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				normalizedArray[xIndex][yIndex] = array[xIndex][yIndex] / area_in_smoothing_circle_sqkm;
			}
		}
		return normalizedArray;
	}

	private double[][] calculateDifferences(double[][] normalizedArrayPolicyCase, double[][] normalizedArrayBaseCase) {
		double [][] diff = new double[noOfXbins][noOfYbins];
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				diff[xIndex][yIndex]= normalizedArrayPolicyCase[xIndex][yIndex] - normalizedArrayBaseCase[xIndex][yIndex];
			}
		}
		return diff;
	}

	private double[][] calculateAverage(double[][] userBenefits, double[][] weights) {
		double[][] average = new double [noOfXbins][noOfYbins];
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				if(weights[xIndex][yIndex] > 0){
					average[xIndex][yIndex]= userBenefits[xIndex][yIndex] / weights[xIndex][yIndex];
				} else {
					throw new RuntimeException("Weights for " + xIndex + "," + yIndex + "is undefined. Aborting ...");
				}
			}
		}
		return average;
	}

	private void writeRoutput(double[][] results, String outputPathForR) {
		try {
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString = "\t";

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
			throw new RuntimeException("Failed writing output for R. Reason: " + e);
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
	}

	private double [][] calculateWeights(Map<Id, Double> personId2Utility, Population pop){
		double[][] weights = new double[noOfXbins][noOfYbins];

		for(Id personId : personId2Utility.keySet()){ 
			Person person = pop.getPersons().get(personId);
			Coord homeCoord = findHomeCoord(person);
			if (isInResearchArea(homeCoord)){
				double personCount = 1.0;
				// one person stands for this.scalingFactor persons
				double scaledPersonCount = this.scalingFactor * personCount;
				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						double weightOfPersonForCell = calculateWeightOfPersonForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
						weights[xIndex][yIndex] += weightOfPersonForCell * scaledPersonCount;
					}
				}
			}
		}
		return weights;
	}
	
	private double [][] fillUserBenefits(Map<Id, Double> personId2Utility, Population pop){
		double[][] scaledWeightedBenefits = new double [noOfXbins][noOfYbins];

		for(Id personId : personId2Utility.keySet()){ 
			Person person = pop.getPersons().get(personId);
			Coord homeCoord = findHomeCoord(person);
			if (isInResearchArea(homeCoord)){
				double benefitOfPerson = personId2Utility.get(personId);
				// one person stands for this.scalingFactor persons; thus, that person earns the sum of these person's benefits (additivity required!)
				double scaledBenefitOfPerson = this.scalingFactor * benefitOfPerson;

				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						double weightOfPersonForCell = calculateWeightOfPersonForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
						scaledWeightedBenefits[xIndex][yIndex] += weightOfPersonForCell * scaledBenefitOfPerson;
					}
				}
			} else {
				// do nothing...
			}
		}
		return scaledWeightedBenefits;
	}
	
	private Coord findHomeCoord(Person person) {
		Plan plan = person.getSelectedPlan();
		Coord homeCoord = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if(pe instanceof Activity){
				Activity act =  ((Activity) pe);
				if(act.getType().equals("home")){
					homeCoord = act.getCoord();
					break;
				} else if(act.getType().equals("pvHome")){
					homeCoord = act.getCoord();
					break;
				} else if(act.getType().equals("gvHome")){
					homeCoord = act.getCoord();
					break;
				}
			}
		}
		if(homeCoord == null){
			throw new RuntimeException("Person " + person.getId() + " has no homeCoord. Aborting...");
		} else {
			return homeCoord;
		}
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

	private double calculateWeightOfPersonForCell(double x1, double y1, double x2, double y2) {
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