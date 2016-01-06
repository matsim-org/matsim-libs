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

package playground.dziemke.accessibility.ptmatrix;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dziemke
 */
public class MatrixBasesPtInputTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	private static final Logger log = Logger.getLogger(MatrixBasesPtInputTest.class);

	@Test
	public final void test() {
		String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";
		String networkFile = "../../matsim/examples/pt-tutorial/multimodalnetwork.xml";
		String outputRoot = utils.getOutputDirectory();
        System.out.println("outputRoot = " + outputRoot);

        double departureTime = 8. * 60 * 60;

		
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().transit().setUseTransit(true);
		
		
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transitScheduleFile);
		
		Map<Id<Coord>, Coord> ptMatrixLocationsMap = new HashMap<>();
		
		for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
			Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
			Coord coord = transitStopFacility.getCoord();
			ptMatrixLocationsMap.put(id, coord);
		}
				
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFile);

		MatrixBasesPtInputUtils.createStopsFile(ptMatrixLocationsMap, outputRoot + "ptStops.csv", ",");
		
		// The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
		// In other uses the two maps may be different -- thus the duplication here.
        log.info("Start matrix-computation...");
		ThreadedMatrixCreator tmc = new ThreadedMatrixCreator(scenario, ptMatrixLocationsMap, 
				ptMatrixLocationsMap, departureTime, outputRoot, " ", 1);

        //waiting for the output to be written
        try {
            tmc.getThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //ptStops
        ArrayList<String[]> ptStops = readCSVLine(outputRoot + "/ptStops.csv", ",");
        ArrayList<String[]> ptStopsCompare = new ArrayList<>();
        ptStopsCompare.add(new String[]{"id", "x", "y"});
        ptStopsCompare.add(new String[]{"2b", "2050.0", "2960.0"});
        ptStopsCompare.add(new String[]{"1", "1050.0", "1050.0"});
        ptStopsCompare.add(new String[]{"3", "3950.0", "1050.0"});
        ptStopsCompare.add(new String[]{"2a", "2050.0", "2940.0"});
        Assert.assertNotNull(ptStops);
        for (String[] line : ptStops) {
            boolean contained = false;
            for (String[] compareLine : ptStopsCompare) {
                if (Arrays.equals(line, compareLine)) contained = true;
            }
            Assert.assertTrue(Arrays.asList(line) + " was not expected", contained);
        }

        //travelDistanceMatrix
        ArrayList<String[]> tdm = readCSVLine(outputRoot + "/travelDistanceMatrix_1.csv", " ");
        ArrayList<String[]> tdmCompare = new ArrayList<>();
        tdmCompare.add(new String[]{"2b", "2b", "0.0"});
        tdmCompare.add(new String[]{"2b", "1", "2410.0"});
        tdmCompare.add(new String[]{"2b", "3", "3630.0"});
        tdmCompare.add(new String[]{"2b", "2a", "20.0"});
        tdmCompare.add(new String[]{"1", "2b", "2158.2469455140113"});
        tdmCompare.add(new String[]{"1", "1", "0.0"});
        tdmCompare.add(new String[]{"1", "3", "6000.0"});
        tdmCompare.add(new String[]{"1", "2a", "2138.2469455140113"});
        tdmCompare.add(new String[]{"3", "2b", "2694.0861159213155"});
        tdmCompare.add(new String[]{"3", "1", "6000.0"});
        tdmCompare.add(new String[]{"3", "3", "0.0"});
        tdmCompare.add(new String[]{"3", "2a", "2714.0861159213155"});
        tdmCompare.add(new String[]{"2a", "2b", "20.0"});
        tdmCompare.add(new String[]{"2a", "1", "2430.0"});
        tdmCompare.add(new String[]{"2a", "3", "3610.0"});
        tdmCompare.add(new String[]{"2a", "2a", "0.0"});
        Assert.assertNotNull(tdm);
        for (String[] line : tdm) {
            boolean contained = false;
            for (String[] compareLine : tdmCompare) {
                if (Arrays.equals(line, compareLine)) contained = true;
            }
            Assert.assertTrue(Arrays.asList(line) + " was not expected", contained);
        }

        //travelTimeMatrix
        ArrayList<String[]> ttm = readCSVLine(outputRoot + "/travelTimeMatrix_1.csv", " ");
        ArrayList<String[]> ttmCompare = new ArrayList<>();
        ttmCompare.add(new String[]{"2b", "2b", "0.0"});
        ttmCompare.add(new String[]{"2b", "1", "540.0"});
        ttmCompare.add(new String[]{"2b", "3", "539.9999999999993"});
        ttmCompare.add(new String[]{"2b", "2a", "31.200000000000003"});
        ttmCompare.add(new String[]{"1", "2b", "231.2"});
        ttmCompare.add(new String[]{"1", "1", "0.0"});
        ttmCompare.add(new String[]{"1", "3", "540.0"});
        ttmCompare.add(new String[]{"1", "2a", "200.0"});
        ttmCompare.add(new String[]{"3", "2b", "300.0"});
        ttmCompare.add(new String[]{"3", "1", "540.0"});
        ttmCompare.add(new String[]{"3", "3", "0.0"});
        ttmCompare.add(new String[]{"3", "2a", "331.2"});
        ttmCompare.add(new String[]{"2a", "2b", "31.200000000000003"});
        ttmCompare.add(new String[]{"2a", "1", "539.9999999999993"});
        ttmCompare.add(new String[]{"2a", "3", "540.0"});
        ttmCompare.add(new String[]{"2a", "2a", "0.0"});
        Assert.assertNotNull(ttm);
        for (String[] line : ttm) {
            boolean contained = false;
            for (String[] compareLine : ttmCompare) {
                if (Arrays.equals(line, compareLine)) contained = true;
            }
            Assert.assertTrue(Arrays.asList(line) + " was not expected", contained);
        }
    }

	public static ArrayList<String[]> readCSVLine(String filePath, String splitString) {
		BufferedReader CSVFile = null;
		try {
			CSVFile = new BufferedReader(new FileReader(filePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
            assert CSVFile != null;
            String dataRow = CSVFile.readLine();
			ArrayList<String[]> lineList = new ArrayList<>();
			while (dataRow != null){
				lineList.add(dataRow.split(splitString));
				dataRow = CSVFile.readLine();
			}
			return lineList;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}