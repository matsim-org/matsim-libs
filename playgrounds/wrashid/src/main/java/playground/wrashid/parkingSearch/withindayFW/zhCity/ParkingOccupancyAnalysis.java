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

import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyHandler;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;

public class ParkingOccupancyAnalysis extends ParkingOccupancyHandler {

	protected static final Logger log = Logger.getLogger(ParkingOccupancyAnalysis.class);
	
	private Set<String> selectedParkings;
	private double[] sumOfOccupancyCountsOfSelectedParkings;
	private final Controler controler;
	private final ParkingInfrastructure parkingInfrastructure;

	public ParkingOccupancyAnalysis(Controler controler, ParkingInfrastructure parkingInfrastructure) {
		super(controler);
		this.controler = controler;
		this.parkingInfrastructure = parkingInfrastructure;
		initializeParkingCounts(controler);
	}

	public void initializeParkingCounts(Controler controler) {
		String baseFolder = null;
		Double countsScalingFactor = Double.parseDouble(controler.getConfig().findParam("parking", "countsScalingFactor"));

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

	private void writeOutGraphComparingSumOfSelectedParkingsToCounts(ParkingOccupancyStats parkingOccupancy) {
		String iterationFilenamePng = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.png");
		String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
				"parkingOccupancyCountsComparison.txt");

		HashMap<String, String> mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount
				.getMappingOfParkingNameToParkingId();
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		int numberOfColumns = 2;
		for (String parkingName : selectedParkings) {
			ParkingOccupancyBins parkingOccupancyBins = parkingOccupancy.parkingOccupancies.get(new IdImpl(
					mappingOfParkingNameToParkingId.get(parkingName)));

			if (parkingOccupancyBins == null) {
				continue;
			}

			int[] occupancy = parkingOccupancyBins.getOccupancy();
			for (int i = 0; i < 96; i++) {
				sumOfSelectedParkingSimulatedCounts[i] += occupancy[i];
			}
		}

		double matrix[][] = new double[96][numberOfColumns];

		for (int i = 0; i < 96; i++) {
			matrix[i][0] = sumOfSelectedParkingSimulatedCounts[i];
			matrix[i][1] = sumOfOccupancyCountsOfSelectedParkings[i];
		}

		String title = "Parking Garage Counts Comparison";
		String xLabel = "time (15min-bin)";
		String yLabel = "# of occupied parkings";
		String[] seriesLabels = new String[2];
		seriesLabels[0] = "simulated counts";
		seriesLabels[1] = "real counts";
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
		writeOutGraphComparingSumOfSelectedParkingsToCounts(parkingOccupancy);
		writeOutGraphParkingTypeOccupancies(parkingOccupancy);

		logPeakUsageOfParkingTypes(parkingOccupancy, parkingInfrastructure);

		if (GlobalParkingSearchParams.getScenarioId() == 2) {
			ParkingCostCalculatorZHPerStreetOptimizedPrice parkingCostCalculator = (ParkingCostCalculatorZHPerStreetOptimizedPrice) this.parkingInfrastructure
					.getParkingCostCalculator();

			parkingCostCalculator.logParkingPriceStats(controler);
			parkingCostCalculator.updatePrices(parkingOccupancy);
		}

	}

	// log peak usage of garage and street parking defined as [peak usage of
	// each parking during day]/[number of parking of that type]
	// this can be used during calibration to find out, how saturated a certain
	// parking type is
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

				int[] occupancy = parkingOccupancy.parkingOccupancies.get(parkingId).getOccupancy();

				for (int i = 0; i < 96; i++) {
					if (peakUsageOfParking.get(parkingId) < occupancy[i]) {
						peakUsageOfParking.set(parkingId, occupancy[i]);
					}
				}
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
		
		log.info("peak usage street parking:" + numberOfPeakStreetParking/1.0/numberOfStreetParking);
		log.info("peak usage garage parking:" + numberOfPeakGarageParking/1.0/numberOfGarageParking);
		log.info("peak usage private parking:" + numberOfPeakPrivateParking/1.0/numberOfPrivateParking);
		
		// remove zero occupancy parking (probably has to do with population scaling artifects)
		for (Id parkingId : peakUsageOfParking.getKeySet()) {
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
		
		log.info("peak usage street parking (non used parking removed):" + numberOfPeakStreetParking/1.0/numberOfStreetParking);
		log.info("peak usage garage parking (non used parking removed):" + numberOfPeakGarageParking/1.0/numberOfGarageParking);
		log.info("peak usage private parking (non used parking removed):" + numberOfPeakPrivateParking/1.0/numberOfPrivateParking);
		
	}

}
