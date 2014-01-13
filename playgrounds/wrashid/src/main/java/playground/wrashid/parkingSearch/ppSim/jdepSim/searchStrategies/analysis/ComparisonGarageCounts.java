/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.ppSim.jdepSim.MainPPSimZurich30km;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingActivityAttributes;

public class ComparisonGarageCounts {

	
	private static final Logger log = Logger.getLogger(ComparisonGarageCounts.class);
	
	private static StringMatrix countsMatrix;
	private static Set<String> selectedParkings;
	private static double[] sumOfOccupancyCountsOfSelectedParkings;
	private static HashMap<String, String> mappingOfParkingNameToParkingId;
	private static DefaultBoxAndWhiskerCategoryDataset boxPlotDataSet=new DefaultBoxAndWhiskerCategoryDataset();

	public static void logOutput(LinkedList<ParkingEventDetails> parkingEventDetails, String outputFileName) {
		// Output file
		log.info("starting log parking events");
		
		String iterationFilenamePng = outputFileName + ".png";
		String iterationFilenameTxt = outputFileName + ".txt";
		
	
		
		HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins=new HashMap<Id, ParkingOccupancyBins>();
		
		for (ParkingEventDetails ped : parkingEventDetails){
			ParkingActivityAttributes parkingActivityAttributes = ped.parkingActivityAttributes;
			Id facilityId = parkingActivityAttributes.getFacilityId();
			if (mappingOfParkingNameToParkingId.values().contains(facilityId.toString())){
				if (!parkingOccupancyBins.containsKey(facilityId)){
					parkingOccupancyBins.put(facilityId, new ParkingOccupancyBins());
				}
				ParkingOccupancyBins parkingOccupancyBin= parkingOccupancyBins.get(facilityId);
				parkingOccupancyBin.inrementParkingOccupancy(parkingActivityAttributes.getParkingArrivalTime(), parkingActivityAttributes.getParkingArrivalTime() + parkingActivityAttributes.getParkingDuration());
			}
		}
		
		int[] sumOfSelectedParkingSimulatedCounts = new int[96];
		for (ParkingOccupancyBins pob:parkingOccupancyBins.values()){
			for (int i = 0; i < 96; i++) {
				sumOfSelectedParkingSimulatedCounts[i] += pob.getOccupancy()[i];
			}
		}
		
		int numberOfColumns=2;
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
		
		String txtFileHeader=seriesLabels[0];
		
		for (int i=1;i<numberOfColumns;i++){
			txtFileHeader+="\t"+seriesLabels[i];
		}
		
		GeneralLib.writeMatrix(matrix, iterationFilenameTxt, txtFileHeader);
		
		log.info("finished log parking events");
	}

	public static void init() {
		String filePath = ZHScenarioGlobal.loadStringParam("ComparisonGarageCounts.garageParkingCountsFile");
		Double countsScalingFactor = ZHScenarioGlobal.loadDoubleParam("ComparisonGarageCounts.countsScalingFactor");
		
		countsMatrix = GeneralLib.readStringMatrix(filePath, "\t");

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
		
		mappingOfParkingNameToParkingId = SingleDayGarageParkingsCount.getMappingOfParkingNameToParkingId();
		
	}

	public static void logRelativeError(LinkedList<ParkingEventDetails> parkingEventDetails, String outputFileName) {
		
		HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins=new HashMap<Id, ParkingOccupancyBins>();
		
		for (ParkingEventDetails ped : parkingEventDetails){
			ParkingActivityAttributes parkingActivityAttributes = ped.parkingActivityAttributes;
			Id facilityId = parkingActivityAttributes.getFacilityId();
			if (mappingOfParkingNameToParkingId.values().contains(facilityId.toString())){
				if (!parkingOccupancyBins.containsKey(facilityId)){
					parkingOccupancyBins.put(facilityId, new ParkingOccupancyBins());
				}
				ParkingOccupancyBins parkingOccupancyBin= parkingOccupancyBins.get(facilityId);
				parkingOccupancyBin.inrementParkingOccupancy(parkingActivityAttributes.getParkingArrivalTime(), parkingActivityAttributes.getParkingArrivalTime() + parkingActivityAttributes.getParkingDuration());
			}
		}
		
		HashMap<String, Double[]> occupancyOfAllSelectedParkings = SingleDayGarageParkingsCount
				.getOccupancyOfAllSelectedParkings(countsMatrix);
		
		List relativeErrorList = new ArrayList();
		
		for (String parkingName : selectedParkings) {
			Double[] occupancyBins = occupancyOfAllSelectedParkings.get(parkingName);
			double measuredOccupancy=0;
			for (int i = 0; i < 96; i++) {
				measuredOccupancy += occupancyBins[i];
			}
			
			Id parkingId = new IdImpl( mappingOfParkingNameToParkingId.get(parkingName));
			ParkingOccupancyBins pob=parkingOccupancyBins.get(parkingId);
			
			double simulatedOccupancySum=0;
			
			if (pob!=null){
			for (int i = 0; i < 96; i++) {
				simulatedOccupancySum += pob.getOccupancy()[i];
			}
			}
			
			double relativeError=-1;
			if (measuredOccupancy!=0){
				relativeError=Math.abs(simulatedOccupancySum-measuredOccupancy)/measuredOccupancy;
			}
			
			relativeErrorList.add(new Double(relativeError));
		}
		boxPlotDataSet.add(relativeErrorList, "relativeError", ZHScenarioGlobal.iteration);
		
		
		
        final NumberAxis yAxis = new NumberAxis("Value");
        yAxis.setAutoRangeIncludesZero(false);
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        final JFreeChart chart = ChartFactory.createBoxAndWhiskerChart( "Rel. Error - Garage Parking Counts", "Iteration", "rel. Error", boxPlotDataSet, false);

        int width = 500;
		int height = 300;
        
        try {
			ChartUtilities.saveChartAsPNG(new File(outputFileName), chart, width,
					height);
		} catch (IOException e) {

		}
		
	}


}

