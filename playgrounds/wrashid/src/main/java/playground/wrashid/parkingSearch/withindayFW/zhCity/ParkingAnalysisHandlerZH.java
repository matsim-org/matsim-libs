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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.Pair;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;

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
		StringMatrix countsMatrix = GeneralLib.readStringMatrix(baseFolder + "parkingGarageCountsCityZH27-April-2011.txt", "\t");

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

	// TODO: remove parkingInfrasturcutre variable (use this.parkingInfrastructure instead)
	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(ParkingOccupancyStats parkingOccupancy, ParkingInfrastructure parkingInfrastructure) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.txt");

		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		int[] sumSimulatedParkingCapacities = new int[96];
		int[] sumRealParkingCapacities = new int[96];
		int numberOfColumns = 4;
		for (String parkingName : selectedParkings) {
			IdImpl parkingId = new IdImpl(
					mappingOfParkingNameToParkingId.get(parkingName));
			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancy.parkingOccupancies.get(parkingId);

			
			
			if (parkingOccupancyBins == null) {
				continue;
			}

			int[] occupancy = parkingOccupancyBins.getOccupancy();
			for (int i = 0; i < 96; i++) {
				sumOfSelectedParkingSimulatedCounts[i] += occupancy[i];
				sumSimulatedParkingCapacities[i] += parkingInfrastructure.getFacilityCapacities().get(parkingId);
				sumRealParkingCapacities[i] += countsScalingFactor * SingleDayGarageParkingsCount.getParkingCapacities().get(parkingName);
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

	private void writeOutGraphParkingTypeOccupancies(ParkingOccupancyStats parkingOccupancy) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyByParkingTypes.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
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
			ParkingInfrastructure parkingInfrastructure) {
		super.updateParkingOccupancyStatistics(parkingOccupancy, parkingInfrastructure);
		writeOutGraphComparingSumOfSelectedParkingsToCounts(parkingOccupancy, parkingInfrastructure);
		writeOutGraphParkingTypeOccupancies(parkingOccupancy);

		logPeakUsageOfParkingTypes(parkingOccupancy, parkingInfrastructure);

		if (GlobalParkingSearchParams.getScenarioId() == 2) {
			ParkingCostOptimizerZH parkingCostCalculator = (ParkingCostOptimizerZH) this.parkingInfrastructure
					.getParkingCostCalculator();

			parkingCostCalculator.logParkingPriceStats(controler);
			parkingCostCalculator.updatePrices(parkingOccupancy);
		}

	}

	// log peak usage of garage and street parking defined as [peak usage of
	// each parking during day]/[number of parking of that type]
	// this can be used during calibration to find out, how saturated a certain
	// parking type is
	// TODO: remove parkingInfrasturcutre variable (use this.parkingInfrastructure instead)
	private void logPeakUsageOfParkingTypes(ParkingOccupancyStats parkingOccupancy, ParkingInfrastructure parkingInfrastructure) {

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

				int peakOccupancy=parkingOccupancy.parkingOccupancies.get(parkingId).getPeakOccupanyOfDay();
				
				// remove over usage of parking
				//if (peakOccupancy>facilityCapacities.get(parkingId)){
				//	peakOccupancy=facilityCapacities.get(parkingId);
				//}
				
				// although, this can result in peak usage of more than 1 in the output, but this
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
		log.info("iteration-" + controler.getIterationNumber());
		log.info("peak usage street parking:" + numberOfPeakStreetParking/1.0/numberOfStreetParking + " - [" + numberOfPeakStreetParking + "]");
		log.info("peak usage garage parking:" + numberOfPeakGarageParking/1.0/numberOfGarageParking + " - [" + numberOfPeakGarageParking + "]");
		log.info("peak usage private parking:" + numberOfPeakPrivateParking/1.0/numberOfPrivateParking + " - [" + numberOfPeakPrivateParking + "]");
		
		// remove zero occupancy parking (probably has to do with population scaling artifects)
		for (Id parkingId : facilityCapacities.getKeySet()) {
			if (parkingId.toString().contains("stp")) {
				if (peakUsageOfParking.get(parkingId)==0){
					numberOfStreetParking-=facilityCapacities.get(parkingId);
				}
			} else if (parkingId.toString().contains("gp")) {
				if (peakUsageOfParking.get(parkingId)==0){
					numberOfGarageParking-=facilityCapacities.get(parkingId);
				}
			} else if (parkingId.toString().contains("private")) {
				if (peakUsageOfParking.get(parkingId)==0){
					numberOfPrivateParking-=facilityCapacities.get(parkingId);
				}
			}
		}
		
		log.info("peak usage street parking (not used parking removed):" + numberOfPeakStreetParking/1.0/numberOfStreetParking + " - [" + numberOfPeakStreetParking + "]");
		log.info("peak usage garage parking (not used parking removed):" + numberOfPeakGarageParking/1.0/numberOfGarageParking+ " - [" + numberOfPeakGarageParking + "]");
		log.info("peak usage private parking (not used parking removed):" + numberOfPeakPrivateParking/1.0/numberOfPrivateParking+ " - [" + numberOfPeakPrivateParking + "]");
		
	}

	@Override
	public void processParkingWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog) {
		// TODO Auto-generated method stub
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

		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"walkingDistanceHistogrammStp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Street Parking Walk Time - It." + controler.getIterationNumber(), "walk time [min]",
				"number of walk legs");

		values = Collections.convertDoubleCollectionToArray(garageParkingWalkLegTimes);

		fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"walkingDistanceHistogrammGp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Garage Parking Walk Time - It." + controler.getIterationNumber(), "walk time [min]",
				"number of walk legs");
		
		if (GlobalParkingSearchParams.writeDetailedOutput(controler.getIterationNumber())){
			writeWalkTimes(parkingWalkTimesLog);
		}
	}

	private void writeWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog=parkingWalkTimesLog;
		
		String headerLine = "personId\tparkingFacilityId\tbothWayWalkTimeInMinutes";
		String outputFileName="walkTimes.txt";
		
		writeParkingLog(inputLog, headerLine, outputFileName);
	}

	private void writeParkingLog(LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog, String headerLine, String outputFileName) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(headerLine);
		
		for (Id personnId : inputLog.getKeySet()) {
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
		
		
		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				outputFileName);
		GeneralLib.writeList(list, fileName);
	}

	@Override
	public void processParkingSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog) {
		// TODO Auto-generated method stub
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

		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"searchTimeHistogrammStp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Street Parking Search Time - It." + controler.getIterationNumber(), "search time [min]",
				"number of parking searches");

		values = Collections.convertDoubleCollectionToArray(garageParkingSearchTimes);

		fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"searchTimeHistogrammGp.png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram Garage Parking Search Time - It." + controler.getIterationNumber(), "search time [min]",
				"number of parking searches");
		
		if (GlobalParkingSearchParams.writeDetailedOutput(controler.getIterationNumber())){
			writeSearchTimes(parkingSearchTimeLog);
		}
	}

	private void writeSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog=parkingSearchTimeLog;
		
		String headerLine = "personId\tparkingFacilityId\tsearchTimeInMinutes";
		String outputFileName="searchTimes.txt";
		
		writeParkingLog(inputLog, headerLine, outputFileName);		
	}

	@Override
	public void processParkingCost(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog) {
		// TODO Auto-generated method stub
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

				String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
						"parkingCostHistogrammStp.png");

				GeneralLib.generateHistogram(fileName, values, 10,
						"Histogram Street Parking Cost - It." + controler.getIterationNumber(), "cost [chf]",
						"number of parking operations");

				values = Collections.convertDoubleCollectionToArray(garageParkingCost);

				fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
						"parkingCostHistogrammGp.png");

				GeneralLib.generateHistogram(fileName, values, 10,
						"Histogram Garage Parking Cost - It." + controler.getIterationNumber(), "cost [chf]",
						"number of parking operations");
				
				if (GlobalParkingSearchParams.writeDetailedOutput(controler.getIterationNumber())){
					writeParkingCostDetails(parkingCostLog);
				}
		
	}
	
	private void writeParkingCostDetails(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog) {
		LinkedListValueHashMap<Id, Pair<Id, Double>> inputLog=parkingCostLog;
		
		String headerLine = "personId\tparkingFacilityId\tcost[chf]";
		String outputFileName="parkingCostLog.txt";
		
		writeParkingLog(inputLog, headerLine, outputFileName);		
	}

	@Override
	public void printShareOfCarUsers() {
		Map<Id, ? extends Person> persons = controler.getPopulation().getPersons();
		int numberOfPerson=persons.size();
		int numberOfCarUsers=0;
		for (Person person:persons.values()){
			for (PlanElement pe: person.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg){
					Leg leg=(Leg) pe;
					
					if (leg.getMode().equals(TransportMode.car)){
						numberOfCarUsers++;
						break;
					}
					
				}
			}
		}
		
		log.info("share of car users:" + numberOfCarUsers/1.0/numberOfPerson);
	}

}
