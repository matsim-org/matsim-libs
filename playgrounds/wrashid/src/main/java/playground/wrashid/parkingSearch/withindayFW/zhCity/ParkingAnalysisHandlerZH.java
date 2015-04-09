/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.zhCity;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.Pair;
import org.matsim.core.controler.Controler;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;

import java.util.*;

public class ParkingAnalysisHandlerZH extends ParkingAnalysisHandler {

	protected static final Logger log = Logger.getLogger(ParkingAnalysisHandlerZH.class);

	private Set<String> selectedParkings;
	private double[] sumOfOccupancyCountsOfSelectedParkings;
	private final ParkingInfrastructure parkingInfrastructure;

	private double countsScalingFactor;

	public ParkingAnalysisHandlerZH(Controler controler, ParkingInfrastructure parkingInfrastructure) {
		this.controler = controler;
		this.parkingInfrastructure = parkingInfrastructure;
		initializeParkingCounts(controler);
	}

	public void initializeParkingCounts(Controler controler) {
		String baseFolder = null;
		countsScalingFactor = Double.parseDouble(controler.getConfig().findParam("parking", "countsScalingFactor"));

		if (ParkingHerbieControler.isRunningOnServer) {
			baseFolder = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/counts/";
		} else {
			baseFolder = "H:/data/experiments/TRBAug2011/parkings/counts/";
		}
		Matrix countsMatrix = GeneralLib.readStringMatrix(baseFolder + "parkingGarageCountsCityZH27-April-2011.txt", "\t");

		HashMap<String, Double[]> occupancyOfAllSelectedParkings = SingleDayGarageParkingsCount
				.getOccupancyOfAllSelectedParkings(countsMatrix);

		selectedParkings = occupancyOfAllSelectedParkings.keySet();

		sumOfOccupancyCountsOfSelectedParkings = new double[96];

		for (String parkingName : selectedParkings) {
			Double[] occupancyBins = occupancyOfAllSelectedParkings.get(parkingName);

			if (occupancyBins == null) {
				DebugLib.stopSystemAndReportInconsistency();
			}

			for (int i = 0; i < 96; i++) {
				sumOfOccupancyCountsOfSelectedParkings[i] += countsScalingFactor * occupancyBins[i];
			}
		}
	}

	// TODO: remove parkingInfrasturcutre variable (use
	// this.parkingInfrastructure instead)
	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(ParkingOccupancyStats parkingOccupancy,
			ParkingInfrastructure parkingInfrastructure, int iteration) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyCountsComparison.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyCountsComparison.txt");

		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		int[] sumSimulatedParkingCapacities = new int[96];
		int[] sumRealParkingCapacities = new int[96];
		int numberOfColumns = 4;
		for (String parkingName : selectedParkings) {
			Id<PParking> parkingId = Id.create(mappingOfParkingNameToParkingId.get(parkingName), PParking.class);
			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancy.parkingOccupancies.get(parkingId);

			if (parkingOccupancyBins == null) {
				continue;
			}

			int[] occupancy = parkingOccupancyBins.getOccupancy();
			for (int i = 0; i < 96; i++) {
				sumOfSelectedParkingSimulatedCounts[i] += occupancy[i];
				sumSimulatedParkingCapacities[i] += parkingInfrastructure.getFacilityCapacities().get(parkingId);
				sumRealParkingCapacities[i] += countsScalingFactor
						* SingleDayGarageParkingsCount.getParkingCapacities().get(parkingName);
			}
		}

		double matrix[][] = new double[96][numberOfColumns];

		for (int i = 0; i < 96; i++) {
			matrix[i][0] = sumOfSelectedParkingSimulatedCounts[i];
			matrix[i][1] = sumOfOccupancyCountsOfSelectedParkings[i];
			matrix[i][2] = sumSimulatedParkingCapacities[i];
			matrix[i][3] = sumRealParkingCapacities[i];
		}

		String title = "Parking Garage Counts Comparison";
		String xLabel = "time (15min-bin)";
		String yLabel = "# of occupied parkings";
		String[] seriesLabels = new String[numberOfColumns];
		seriesLabels[0] = "simulated counts";
		seriesLabels[1] = "real counts";
		seriesLabels[2] = "simulated max. capacity";
		seriesLabels[3] = "real max. capacity";
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) 4;
		}

		GeneralLib.writeGraphic(iterationFilenamePng, matrix, title, xLabel, yLabel, seriesLabels, xValues);

		String txtFileHeader = seriesLabels[0];

		for (int i = 1; i < numberOfColumns; i++) {
			txtFileHeader += "\t" + seriesLabels[i];
		}
		GeneralLib.writeMatrix(matrix, iterationFilenameTxt, txtFileHeader);
	}

	private void writeOutGraphParkingTypeOccupancies(ParkingOccupancyStats parkingOccupancy, int iteration) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyByParkingTypes.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(iteration,
				"parkingOccupancyByParkingTypes.txt");

		int numberOfColumns = 4;
		double matrix[][] = new double[96][numberOfColumns];

		for (Id parkingId : parkingOccupancy.parkingOccupancies.keySet()) {
			int graphIndex = -1;
			if (parkingId.toString().startsWith("gp")) {
				graphIndex = 0;
			} else if (parkingId.toString().startsWith("privateParkings")) {
				graphIndex = 1;
			} else if (parkingId.toString().startsWith("publicPOutsideCityZH")) {
				graphIndex = 2;
			} else if (parkingId.toString().startsWith("stp")) {
				graphIndex = 3;
			} else {
				DebugLib.stopSystemAndReportInconsistency("parking type (Id) unknown: " + parkingId);
			}

			int[] occupancy = parkingOccupancy.parkingOccupancies.get(parkingId).getOccupancy();
			for (int i = 0; i < 96; i++) {
				matrix[i][graphIndex] += occupancy[i];
			}
		}

		String title = "ParkingTypeOccupancies";
		String xLabel = "time (15min-bin)";
		String yLabel = "# of occupied parkings";
		String[] seriesLabels = new String[numberOfColumns];
		seriesLabels[0] = "garageParkings";
		seriesLabels[1] = "privateParkings";
		seriesLabels[2] = "publicParkingsOutsideCityZH";
		seriesLabels[3] = "streetParkings";
		double[] xValues = new double[96];

		for (int i = 0; i < 96; i++) {
			xValues[i] = i / (double) numberOfColumns;
		}

		GeneralLib.writeGraphic(iterationFilenamePng, matrix, title, xLabel, yLabel, seriesLabels, xValues);

		String txtFileHeader = seriesLabels[0];
		for (int i = 1; i < numberOfColumns; i++) {
			txtFileHeader += "\t" + seriesLabels[i];
		}
		GeneralLib.writeMatrix(matrix, iterationFilenameTxt, txtFileHeader);
	}

	@Override
	public void updateParkingOccupancyStatistics(ParkingOccupancyStats parkingOccupancy,
			IntegerValueHashMap<Id> facilityCapacities, int iteration) {
		super.updateParkingOccupancyStatistics(parkingOccupancy, facilityCapacities, iteration);
		writeOutGraphComparingSumOfSelectedParkingsToCounts(parkingOccupancy, parkingInfrastructure, iteration);
		writeOutGraphParkingTypeOccupancies(parkingOccupancy, iteration);

		logPeakUsageOfParkingTypes(parkingOccupancy, parkingInfrastructure, iteration);

		if (GlobalParkingSearchParams.getScenarioId() == 2) {
			ParkingCostOptimizerZH parkingCostCalculator = (ParkingCostOptimizerZH) this.parkingInfrastructure
					.getParkingCostCalculator();

			parkingCostCalculator.logParkingPriceStats(controler, iteration);
			parkingCostCalculator.updatePrices(parkingOccupancy);
		}

		if (GlobalParkingSearchParams.writeDetailedOutput(iteration)) {
			writeUnusedParking(parkingOccupancy, parkingInfrastructure, iteration);
		}

	}

	private void writeUnusedParking(ParkingOccupancyStats parkingOccupancy, ParkingInfrastructure parkingInfrastructure2, int iteration) {
		IntegerValueHashMap<Id> unusedCapacityStreetParking = new IntegerValueHashMap<Id>();
		IntegerValueHashMap<Id> unusedCapacityGarageParking = new IntegerValueHashMap<Id>();
		IntegerValueHashMap<Id> unusedCapacityPrivateParking = new IntegerValueHashMap<Id>();

		IntegerValueHashMap<Id> facilityCapacities = parkingInfrastructure.getFacilityCapacities();
		for (Id parkingId : facilityCapacities.getKeySet()) {
			String parkingIdString = parkingId.toString();
			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancy.parkingOccupancies.get(parkingId);
			
			int peakOccupancy=0;
			if (parkingOccupancyBins!=null){
				peakOccupancy = parkingOccupancyBins.getPeakOccupanyOfDay();
			}
			
			int unusedParkingCapacity = facilityCapacities.get(parkingId) - peakOccupancy;

			if (parkingIdString.contains("stp")) {
				unusedCapacityStreetParking.set(parkingId, unusedParkingCapacity);
			} else if (parkingIdString.contains("gp")) {
				unusedCapacityGarageParking.set(parkingId, unusedParkingCapacity);
			} else if (parkingIdString.contains("private")) {
				unusedCapacityPrivateParking.set(parkingId, unusedParkingCapacity);
			}
		}
		
		writeUnusedParkingHistogram("StreetParking", unusedCapacityStreetParking, iteration);
		writeUnusedParkingHistogram("GarageParking", unusedCapacityGarageParking, iteration);
		writeUnusedParkingHistogram("PrivateParking", unusedCapacityPrivateParking, iteration);
	}

	private void writeUnusedParkingHistogram(String parkingType, IntegerValueHashMap<Id> unusedParkingCapacity, int iteration) {
		double[] values = Collections.convertIntegerCollectionToDoubleArray(unusedParkingCapacity.values());
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"unusedParkingHistogramm"+ parkingType + ".png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram "+ parkingType + " Unused Parking - It." + iteration, "number of unused Parking",
				"number of parking facilities");
		
	}

	// log peak usage of garage and street parking defined as [peak usage of
	// each parking during day]/[number of parking of that type]
	// this can be used during calibration to find out, how saturated a certain
	// parking type is
	// TODO: remove parkingInfrasturcutre variable (use
	// this.parkingInfrastructure instead)
	private void logPeakUsageOfParkingTypes(ParkingOccupancyStats parkingOccupancy, ParkingInfrastructure parkingInfrastructure, int iteration) {

		int numberOfStreetParking = 0;
		int numberOfGarageParking = 0;
		int numberOfPrivateParking = 0;

		IntegerValueHashMap<Id> facilityCapacities = parkingInfrastructure.getFacilityCapacities();
		for (Id parkingId : facilityCapacities.getKeySet()) {
			if (parkingId.toString().contains("stp")) {
				numberOfStreetParking += facilityCapacities.get(parkingId);
			} else if (parkingId.toString().contains("gp")) {
				numberOfGarageParking += facilityCapacities.get(parkingId);
			} else if (parkingId.toString().contains("private")) {
				numberOfPrivateParking += facilityCapacities.get(parkingId);
			}
		}

		IntegerValueHashMap<Id> peakUsageOfParking = new IntegerValueHashMap<Id>();

		for (Id parkingId : parkingOccupancy.parkingOccupancies.keySet()) {
			if (parkingId.toString().contains("stp") || parkingId.toString().contains("gp")
					|| parkingId.toString().contains("private")) {

				int peakOccupancy = parkingOccupancy.parkingOccupancies.get(parkingId).getPeakOccupanyOfDay();

				// remove over usage of parking
				// if (peakOccupancy>facilityCapacities.get(parkingId)){
				// peakOccupancy=facilityCapacities.get(parkingId);
				// }

				// although, this can result in peak usage of more than 1 in the
				// output, but this
				// is more precise!

				peakUsageOfParking.set(parkingId, peakOccupancy);
			}
		}

		int numberOfPeakStreetParking = 0;
		int numberOfPeakGarageParking = 0;
		int numberOfPeakPrivateParking = 0;

		for (Id parkingId : peakUsageOfParking.getKeySet()) {
			if (parkingId.toString().contains("stp")) {
				numberOfPeakStreetParking += peakUsageOfParking.get(parkingId);
			} else if (parkingId.toString().contains("gp")) {
				numberOfPeakGarageParking += peakUsageOfParking.get(parkingId);
			} else if (parkingId.toString().contains("private")) {
				numberOfPeakPrivateParking += peakUsageOfParking.get(parkingId);
			}
		}
		log.info("iteration-" + iteration);
		log.info("peak usage street parking:" + numberOfPeakStreetParking / 1.0 / numberOfStreetParking + " - ["
				+ numberOfPeakStreetParking + "]");
		log.info("peak usage garage parking:" + numberOfPeakGarageParking / 1.0 / numberOfGarageParking + " - ["
				+ numberOfPeakGarageParking + "]");
		log.info("peak usage private parking:" + numberOfPeakPrivateParking / 1.0 / numberOfPrivateParking + " - ["
				+ numberOfPeakPrivateParking + "]");

		// remove zero occupancy parking (probably has to do with population
		// scaling artifects)
		for (Id parkingId : facilityCapacities.getKeySet()) {
			if (parkingId.toString().contains("stp")) {
				if (peakUsageOfParking.get(parkingId) == 0) {
					numberOfStreetParking -= facilityCapacities.get(parkingId);
				}
			} else if (parkingId.toString().contains("gp")) {
				if (peakUsageOfParking.get(parkingId) == 0) {
					numberOfGarageParking -= facilityCapacities.get(parkingId);
				}
			} else if (parkingId.toString().contains("private")) {
				if (peakUsageOfParking.get(parkingId) == 0) {
					numberOfPrivateParking -= facilityCapacities.get(parkingId);
				}
			}
		}

		log.info("peak usage street parking (not used parking removed):" + numberOfPeakStreetParking / 1.0
				/ numberOfStreetParking + " - [" + numberOfPeakStreetParking + "]");
		log.info("peak usage garage parking (not used parking removed):" + numberOfPeakGarageParking / 1.0
				/ numberOfGarageParking + " - [" + numberOfPeakGarageParking + "]");
		log.info("peak usage private parking (not used parking removed):" + numberOfPeakPrivateParking / 1.0
				/ numberOfPrivateParking + " - [" + numberOfPeakPrivateParking + "]");

	}

	@Override
	public void processParkingWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog, int iteration) {
		
		LinkedList<Double> streetParkingWalkLegTimes = new LinkedList<Double>();
		LinkedList<Double> garageParkingWalkLegTimes = new LinkedList<Double>();

		for (Id personnId : parkingWalkTimesLog.getKeySet()) {
			for (Pair<Id, Double> pair : parkingWalkTimesLog.get(personnId)) {
				Id parkingFacilityId = pair.getFistValue();
				double walkingTime = pair.getSecondValue();

				if (parkingFacilityId.toString().contains("stp")) {
					// need to count the leg twice
					streetParkingWalkLegTimes.add(walkingTime / 2);
					streetParkingWalkLegTimes.add(walkingTime / 2);
				} else if (parkingFacilityId.toString().contains("gp")) {
					garageParkingWalkLegTimes.add(walkingTime / 2);
					garageParkingWalkLegTimes.add(walkingTime / 2);
				}
			}
		}

		double[] values = Collections.convertDoubleCollectionToArray(streetParkingWalkLegTimes);

		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"walkingDistanceHistogrammStp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Street Parking Walk Time - It." + iteration, "walk time [min]",
				"number of walk legs");

		//TODO: attention error can occur here, if you have totally strange scenario and values are empty
		// => holds also for the other histograms! => perhaps handle it directly in 
		// the function GeneralLib.generateHistogram
		
		values = Collections.convertDoubleCollectionToArray(garageParkingWalkLegTimes);

		fileName = controler.getControlerIO().getIterationFilename(iteration,
				"walkingDistanceHistogrammGp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Garage Parking Walk Time - It." + iteration, "walk time [min]",
				"number of walk legs");

		if (GlobalParkingSearchParams.writeDetailedOutput(iteration)) {
			writeWalkTimes(parkingWalkTimesLog, iteration);
		}
	}

	private void writeWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog, int iteration) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog = parkingWalkTimesLog;

		String headerLine = "personId\tparkingFacilityId\tbothWayWalkTimeInMinutes";
		String outputFileName = "walkTimes.txt";

		writeParkingLog(inputLog, headerLine, outputFileName, iteration);
	}

	private void writeParkingLog(LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog, String headerLine, String outputFileName, int iteration) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(headerLine);

		for (Id<Person> personnId : inputLog.getKeySet()) {
			for (Pair<Id, Double> pair : inputLog.get(personnId)) {
				Id parkingFacilityId = pair.getFistValue();
				double value = pair.getSecondValue();

				StringBuffer stringBuffer = new StringBuffer();

				stringBuffer.append(personnId);
				stringBuffer.append("\t");
				stringBuffer.append(parkingFacilityId);
				stringBuffer.append("\t");
				stringBuffer.append(value);
				list.add(stringBuffer.toString());
			}
		}

		String fileName = controler.getControlerIO().getIterationFilename(iteration, outputFileName);
		GeneralLib.writeList(list, fileName);
	}

	@Override
	public void processParkingSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog, int iteration) {

		LinkedList<Double> streetParkingSearchTimes = new LinkedList<Double>();
		LinkedList<Double> garageParkingSearchTimes = new LinkedList<Double>();

		for (Id personnId : parkingSearchTimeLog.getKeySet()) {
			for (Pair<Id, Double> pair : parkingSearchTimeLog.get(personnId)) {
				Id parkingFacilityId = pair.getFistValue();
				double searchTime = pair.getSecondValue();

				if (parkingFacilityId.toString().contains("stp")) {
					streetParkingSearchTimes.add(searchTime);
				} else if (parkingFacilityId.toString().contains("gp")) {
					garageParkingSearchTimes.add(searchTime);
				}
			}
		}

		double[] values = Collections.convertDoubleCollectionToArray(streetParkingSearchTimes);

		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"searchTimeHistogrammStp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Street Parking Search Time - It." + iteration, "search time [min]",
				"number of parking searches");

		values = Collections.convertDoubleCollectionToArray(garageParkingSearchTimes);

		fileName = controler.getControlerIO().getIterationFilename(iteration, "searchTimeHistogrammGp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Garage Parking Search Time - It." + iteration, "search time [min]",
				"number of parking searches");

		if (GlobalParkingSearchParams.writeDetailedOutput(iteration)) {
			writeSearchTimes(parkingSearchTimeLog, iteration);
		}
	}

	private void writeSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog, int iteration) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog = parkingSearchTimeLog;

		String headerLine = "personId\tparkingFacilityId\tsearchTimeInMinutes";
		String outputFileName = "searchTimes.txt";

		writeParkingLog(inputLog, headerLine, outputFileName, iteration);
	}

	@Override
	public void processParkingCost(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog, int iteration) {
		LinkedList<Double> streetParkingCost = new LinkedList<Double>();
		LinkedList<Double> garageParkingCost = new LinkedList<Double>();

		for (Id personnId : parkingCostLog.getKeySet()) {
			for (Pair<Id, Double> pair : parkingCostLog.get(personnId)) {
				Id parkingFacilityId = pair.getFistValue();
				double cost = pair.getSecondValue();

				if (parkingFacilityId.toString().contains("stp")) {
					streetParkingCost.add(cost);
				} else if (parkingFacilityId.toString().contains("gp")) {
					garageParkingCost.add(cost);
				}
			}
		}

		double[] values = Collections.convertDoubleCollectionToArray(streetParkingCost);

		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"parkingCostHistogrammStp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Street Parking Cost - It." + iteration, "cost [chf]",
				"number of parking operations");

		values = Collections.convertDoubleCollectionToArray(garageParkingCost);

		fileName = controler.getControlerIO().getIterationFilename(iteration, "parkingCostHistogrammGp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Garage Parking Cost - It." + iteration, "cost [chf]",
				"number of parking operations");

		if (GlobalParkingSearchParams.writeDetailedOutput(iteration)) {
			writeParkingCostDetails(parkingCostLog, iteration);
		}

	}

	private void writeParkingCostDetails(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog, int iteration) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog = parkingCostLog;

		String headerLine = "personId\tparkingFacilityId\tcost[chf]";
		String outputFileName = "parkingCostLog.txt";

		writeParkingLog(inputLog, headerLine, outputFileName, iteration);
	}

	@Override
	public void printShareOfCarUsers() {
        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
		int numberOfPerson = persons.size();
		int numberOfCarUsers = 0;
		for (Person person : persons.values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;

					if (leg.getMode().equals(TransportMode.car)) {
						numberOfCarUsers++;
						break;
					}

				}
			}
		}

		log.info("share of car users:" + numberOfCarUsers / 1.0 / numberOfPerson);
	}

}
