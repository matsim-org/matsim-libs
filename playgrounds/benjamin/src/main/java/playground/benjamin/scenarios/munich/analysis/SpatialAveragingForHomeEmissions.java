/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveraging.java
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
package playground.benjamin.scenarios.munich.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;

import com.vividsolutions.jts.util.Assert;

/**
 * @author benjamin
 *
 */
public class SpatialAveragingForHomeEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingForHomeEmissions.class);

	private final String runNumber1 = "972";
	private final String runNumber2 = "973";
	private final String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
	private final String netFile2 = runDirectory2 + "output_network.xml.gz";
	private final String plansFile1 = runDirectory1 + "output_plans.xml.gz";
	private final String plansFile2 = runDirectory2 + "output_plans.xml.gz";
	private final String emissionFile1 = runDirectory1 + runNumber1 + ".emission.events.xml.gz";
	private final String emissionFile2 = runDirectory2 + runNumber2 + ".emission.events.xml.gz";

	private final String outFile1 = runDirectory2 + "emissions/" + runNumber2 + "-" + runNumber1 + ".emissionsTotalPerHomeLocation.txt";
	private final String outFile2 = runDirectory2 + "emissions/" + runNumber2 + "-" + runNumber1 + ".emissionsTotalPerHomeLocationSmoothed.txt";

	static final double xMin = 4452550.25;
	static final double xMax = 4479483.33;
	static final double yMin = 5324955.00;
	static final double yMax = 5345696.81;

	static int noOfXbins = 80;
	static int noOfYbins = 60;
	static int minimumNoOfPeopleInCell = 2;

	private void run() throws IOException{
		Scenario sc1 = loadScenario(netFile1, plansFile1);
		Scenario sc2 = loadScenario(netFile2, plansFile2);
		Population pop1 = sc1.getPopulation();
		Population pop2 = sc2.getPopulation();

		EmissionsPerPersonAggregator epa1 = new EmissionsPerPersonAggregator(pop1, emissionFile1);
		EmissionsPerPersonAggregator epa2 = new EmissionsPerPersonAggregator(pop2, emissionFile2);
		epa1.run();
		epa2.run();

		SortedSet<String> listOfPollutants = epa1.getListOfPollutants();
		Map<Id, Map<String, Double>> emissionsTotal1 = epa1.getTotalEmissions();
		Map<Id, Map<String, Double>> emissionsTotal2 = epa2.getTotalEmissions();


		Map<Id, Map<String, Double>> deltaEmissionsTotal = calcualateEmissionDifferences(emissionsTotal1, emissionsTotal2);
		EmissionWriter eWriter = new EmissionWriter();
		eWriter.writeHomeLocation2Emissions(pop1, listOfPollutants, deltaEmissionsTotal, outFile1);

		int[][] noOfPeopleInCell = new int[noOfXbins][noOfYbins];
		double[][] weightOfCell = new double[noOfXbins][noOfYbins];
		double[][] weightedValuesOfCell = new double[noOfXbins][noOfYbins];

		PersonFilter filter = new PersonFilter();
		for(Person person : pop1.getPersons().values()){
			boolean isPersonFromMiD = filter.isPersonFromMID(person);
			if(isPersonFromMiD){
				Id personId = person.getId();
				Coord homeCoord = getHomeCoord(person);
				double xHome = homeCoord.getX();
				double yHome = homeCoord.getY();

				Integer xbin = mapXCoordToBin(xHome);
				Integer ybin = mapYCoordToBin(yHome);
				if ( xbin != null && ybin != null ){

					noOfPeopleInCell[xbin][ybin] ++;

					for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
						for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
							Coord cellCentroid = findCellCentroid(xIndex, yIndex);
							double value = deltaEmissionsTotal.get(personId).get("CO2_TOTAL");
							// TODO: not distance between data points, but distance between
							// data point and cell centroid is used now; is the former to expensive?
							double weightOfPersonForCell = calculateWeightForOtherCell(xHome, yHome, cellCentroid.getX(), cellCentroid.getY());
							weightOfCell[xIndex][yIndex] += weightOfPersonForCell;
							weightedValuesOfCell[xIndex][yIndex] += weightOfPersonForCell * value;
						}
					}
				}
			}
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile2);
		writer.append("xCentroid \t yCentroid \t CO2_TOTAL \n");
		for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
				Coord cellCentroid = findCellCentroid(xIndex, yIndex);
				if(noOfPeopleInCell[xIndex][yIndex] > minimumNoOfPeopleInCell){
					double averageValue = weightedValuesOfCell[xIndex][yIndex] / weightOfCell[xIndex][yIndex];
					String outString = cellCentroid.getX() + "\t" + cellCentroid.getY() + "\t" + averageValue + "\n";
					writer.append(outString);
				}
			}
		}
		writer.close();
		logger.info("Finished writing output to " + outFile2);
	}

	private double calculateWeightForOtherCell(double x1, double y1, double x2, double y2) {
		double distance = Math.abs(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))); // TODO: need to check if distance > 0 ?!?
		return Math.exp((-distance * distance) / (1000. * 1000.)); // TODO: what is this normalization for?
	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin); // TODO: ???
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin); // TODO: ???
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
		if (yCoord <= yMin || yCoord >= yMax) return null; // yHome is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xHome is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Coord getHomeCoord(Person person) {
		Plan plan = person.getSelectedPlan();
		Activity homeAct = (Activity) plan.getPlanElements().get(0);
		Coord homeCoord = homeAct.getCoord();
		return homeCoord;
	}

	private Map<Id, Map<String, Double>> calcualateEmissionDifferences(Map<Id, Map<String, Double>> emissions1, Map<Id, Map<String, Double>> emissions2) {
		Map<Id, Map<String, Double>> delta = new HashMap<Id, Map<String, Double>>();

		for(Entry<Id, Map<String, Double>> entry : emissions1.entrySet()){
			Id personId = entry.getKey();
			Map<String, Double> emissionDifferenceMap = new HashMap<String, Double>();
			for(String pollutant : entry.getValue().keySet()){
				Double emissionsBefore = entry.getValue().get(pollutant);
				Double emissionsAfter = emissions2.get(personId).get(pollutant);
				Double emissionDifference = emissionsAfter - emissionsBefore;
				emissionDifferenceMap.put(pollutant, emissionDifference);
			}
			delta.put(personId, emissionDifferenceMap);
		}
		return delta;
	}

	private Scenario loadScenario(String netFile, String plansFile) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario) ;
		scenarioLoader.loadScenario() ;
		return scenario;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingForHomeEmissions().run();
	}
}
