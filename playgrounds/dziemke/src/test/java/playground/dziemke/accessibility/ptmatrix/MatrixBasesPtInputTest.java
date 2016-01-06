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
        log.info("Read and check ptStops.csv...");
        ArrayList<String[]> ptStops = readCSVLine(outputRoot + "ptStops.csv", ",");
        if (ptStops.size() < 3) {
            Assert.fail();
        }
        System.out.println("ptStops.get(1) = " + Arrays.asList(ptStops.get(1)));
        Assert.assertEquals(ptStops.get(1)[0], "3");//"2b");
        Assert.assertEquals(ptStops.get(1)[1], "2050.0");
        Assert.assertEquals(ptStops.get(1)[2], "2960.0");
        Assert.assertEquals(ptStops.get(2)[0], "1");
        Assert.assertEquals(ptStops.get(2)[1], "1050.0");
        Assert.assertEquals(ptStops.get(2)[2], "1050.0");
        Assert.assertEquals(ptStops.get(3)[0], "3");
        Assert.assertEquals(ptStops.get(3)[1], "3950.0");
        Assert.assertEquals(ptStops.get(3)[2], "1050.0");
        Assert.assertEquals(ptStops.get(4)[0], "2a");
        Assert.assertEquals(ptStops.get(4)[1], "2050.0");
        Assert.assertEquals(ptStops.get(4)[2], "2940.0");
        log.info("Reading and checking ptStops.csv finished.");


        //travelDistanceMatrix
        log.info("Read and check travelDistanceMatrix_1.csv...");
        ArrayList<String[]> tdm = readCSVLine(outputRoot + "travelDistanceMatrix_1.csv", " ");
        assert tdm != null;
        Assert.assertEquals(tdm.get(0)[0], "2b");
        Assert.assertEquals(tdm.get(0)[1], "2b");
        Assert.assertEquals(tdm.get(0)[2], "0.0");
        Assert.assertEquals(tdm.get(1)[0], "2b");
        Assert.assertEquals(tdm.get(1)[1], "1");
        Assert.assertEquals(tdm.get(1)[2], "2410.0");
        Assert.assertEquals(tdm.get(2)[0], "2b");
        Assert.assertEquals(tdm.get(2)[1], "3");
        Assert.assertEquals(tdm.get(2)[2], "3630.0");
        Assert.assertEquals(tdm.get(3)[0], "2b");
        Assert.assertEquals(tdm.get(3)[1], "2a");
        Assert.assertEquals(tdm.get(3)[2], "20.0");
        Assert.assertEquals(tdm.get(4)[0], "1");
        Assert.assertEquals(tdm.get(4)[1], "2b");
        Assert.assertEquals(tdm.get(4)[2], "2158.2469455140113");
        Assert.assertEquals(tdm.get(5)[0], "1");
        Assert.assertEquals(tdm.get(5)[1], "1");
        Assert.assertEquals(tdm.get(5)[2], "0.0");
        Assert.assertEquals(tdm.get(6)[0], "1");
        Assert.assertEquals(tdm.get(6)[1], "3");
        Assert.assertEquals(tdm.get(6)[2], "6000.0");
        Assert.assertEquals(tdm.get(7)[0], "1");
        Assert.assertEquals(tdm.get(7)[1], "2a");
        Assert.assertEquals(tdm.get(7)[2], "2138.2469455140113");
        Assert.assertEquals(tdm.get(8)[0], "3");
        Assert.assertEquals(tdm.get(8)[1], "2b");
        Assert.assertEquals(tdm.get(8)[2], "2694.0861159213155");
        Assert.assertEquals(tdm.get(9)[0], "3");
        Assert.assertEquals(tdm.get(9)[1], "1");
        Assert.assertEquals(tdm.get(9)[2], "6000.0");
        Assert.assertEquals(tdm.get(10)[0], "3");
        Assert.assertEquals(tdm.get(10)[1], "3");
        Assert.assertEquals(tdm.get(10)[2], "0.0");
        Assert.assertEquals(tdm.get(11)[0], "3");
        Assert.assertEquals(tdm.get(11)[1], "2a");
        Assert.assertEquals(tdm.get(11)[2], "2714.0861159213155");
        Assert.assertEquals(tdm.get(12)[0], "2a");
        Assert.assertEquals(tdm.get(12)[1], "2b");
        Assert.assertEquals(tdm.get(12)[2], "20.0");
        Assert.assertEquals(tdm.get(13)[0], "2a");
        Assert.assertEquals(tdm.get(13)[1], "1");
        Assert.assertEquals(tdm.get(13)[2], "2430.0");
        Assert.assertEquals(tdm.get(14)[0], "2a");
        Assert.assertEquals(tdm.get(14)[1], "3");
        Assert.assertEquals(tdm.get(14)[2], "3610.0");
        Assert.assertEquals(tdm.get(15)[0], "2a");
        Assert.assertEquals(tdm.get(15)[1], "2a");
        Assert.assertEquals(tdm.get(15)[2], "0.0");
        log.info("Reading and checking travelDistanceMatrix_1.csv finished.");

        //travelTimeMatrix
        log.info("Read and check travelTimeMatrix_1.csv...");
        ArrayList<String[]> ttm = readCSVLine(outputRoot + "travelTimeMatrix_1.csv", " ");
        assert ttm != null;
        Assert.assertEquals(ttm.get(0)[0], "2b");
        Assert.assertEquals(ttm.get(0)[1], "2b");
        Assert.assertEquals(ttm.get(0)[2], "0.0");
        Assert.assertEquals(ttm.get(1)[0], "2b");
        Assert.assertEquals(ttm.get(1)[1], "1");
        Assert.assertEquals(ttm.get(1)[2], "540.0");
        Assert.assertEquals(ttm.get(2)[0], "2b");
        Assert.assertEquals(ttm.get(2)[1], "3");
        Assert.assertEquals(ttm.get(2)[2], "539.9999999999993");
        Assert.assertEquals(ttm.get(3)[0], "2b");
        Assert.assertEquals(ttm.get(3)[1], "2a");
        Assert.assertEquals(ttm.get(3)[2], "31.200000000000003");
        Assert.assertEquals(ttm.get(4)[0], "1");
        Assert.assertEquals(ttm.get(4)[1], "2b");
        Assert.assertEquals(ttm.get(4)[2], "231.2");
        Assert.assertEquals(ttm.get(5)[0], "1");
        Assert.assertEquals(ttm.get(5)[1], "1");
        Assert.assertEquals(ttm.get(5)[2], "0.0");
        Assert.assertEquals(ttm.get(6)[0], "1");
        Assert.assertEquals(ttm.get(6)[1], "3");
        Assert.assertEquals(ttm.get(6)[2], "540.0");
        Assert.assertEquals(ttm.get(7)[0], "1");
        Assert.assertEquals(ttm.get(7)[1], "2a");
        Assert.assertEquals(ttm.get(7)[2], "200.0");
        Assert.assertEquals(ttm.get(8)[0], "3");
        Assert.assertEquals(ttm.get(8)[1], "2b");
        Assert.assertEquals(ttm.get(8)[2], "300.0");
        Assert.assertEquals(ttm.get(9)[0], "3");
        Assert.assertEquals(ttm.get(9)[1], "1");
        Assert.assertEquals(ttm.get(9)[2], "540.0");
        Assert.assertEquals(ttm.get(10)[0], "3");
        Assert.assertEquals(ttm.get(10)[1], "3");
        Assert.assertEquals(ttm.get(10)[2], "0.0");
        Assert.assertEquals(ttm.get(11)[0], "3");
        Assert.assertEquals(ttm.get(11)[1], "2a");
        Assert.assertEquals(ttm.get(11)[2], "331.2");
        Assert.assertEquals(ttm.get(12)[0], "2a");
        Assert.assertEquals(ttm.get(12)[1], "2b");
        Assert.assertEquals(ttm.get(12)[2], "31.200000000000003");
        Assert.assertEquals(ttm.get(13)[0], "2a");
        Assert.assertEquals(ttm.get(13)[1], "1");
        Assert.assertEquals(ttm.get(13)[2], "539.9999999999993");
        Assert.assertEquals(ttm.get(14)[0], "2a");
        Assert.assertEquals(ttm.get(14)[1], "3");
        Assert.assertEquals(ttm.get(14)[2], "540.0");
        Assert.assertEquals(ttm.get(15)[0], "2a");
        Assert.assertEquals(ttm.get(15)[1], "2a");
        Assert.assertEquals(ttm.get(15)[2], "0.0");
        log.info("Reading and checking travelTimeMatrix_1.csv finished.");

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