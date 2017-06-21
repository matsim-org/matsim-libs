/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.integrationCNE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.agarwalamit.analysis.modeSwitcherRetainer.ModeSwitchersTripTime;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.IKEventsReader;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;

/**
 * Created by amit on 12.06.17
 * ikaddoura
 * 
 */


public class ModeSwitcherAnalyser {
	
    public static void main(String[] args) throws IOException {
        ModeSwitcherAnalyser modeSwitcherAnalyser = new ModeSwitcherAnalyser();
        modeSwitcherAnalyser.analyze();
    }

    public void analyze() throws IOException {
    	final Map<String,Coord> modeRetainersOrigin = new HashMap<>();
    	final Map<String,Coord> modeRetainersDestination = new HashMap<>();
    	final Map<String,Coord> modeRetainersHomeCoord = new HashMap<>();

    	final Map<String,Coord> car2xOrigin = new HashMap<>();
    	final Map<String,Coord> car2xDestination = new HashMap<>();    	
    	final Map<String,Coord> car2xHomeCoord = new HashMap<>();
    	
    	final Map<String,Coord> x2carOrigin = new HashMap<>();
    	final Map<String,Coord> x2carDestination = new HashMap<>();
    	final Map<String,Coord> x2carHomeCoord = new HashMap<>();

    	
    	Map<Id<Person>, Map<Integer, Coord>> personId2actNr2coord = new HashMap<>();
    	Map<Id<Person>, Coord> personId2homeActCoord = new HashMap<>();

//        // Berlin
//    	int [] its = {0, 100};
//    	
//		// case without mode choice
//    	String directory1 = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/r_output_run4_bln_cne_DecongestionPID";
//
//		// case with mode choice
//    	String directory2 = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run4_bln_cne_DecongestionPID";

		// Munich
    	
    	int [] its = {1000, 1500};
    	
        // case without mode choice
    	String directory1 = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4_muc_cne_DecongestionPID";
       
		// case with mode choice
    	String directory2 = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4b_muc_cne_DecongestionPID";

    	
    	// ################
    	
		String dir1lastIterationFile = directory1 + "/ITERS/it." + its[1]+"/"+its[1]+".events.xml.gz";
	    String dir1networkFile = directory1 + "/output_network.xml.gz";
	    String dir1populationFile = directory1 + "/output_plans.xml.gz";
	    
	    String dir2firstIterationFile = directory2 + "/ITERS/it." + its[0]+"/"+its[0]+".events.xml.gz";
        String dir2lastIterationFile = directory2 + "/ITERS/it." + its[1]+"/"+its[1]+".events.xml.gz";
		String dir2networkFile = directory2 + "/output_network.xml.gz";
		String dir2populationFile = directory2 + "/output_plans.xml.gz";	  
		
		String analysisOutputFolder = "/modeSwitchAnalysis";
		File f = new File(directory2 + analysisOutputFolder + "/");
		f.mkdirs();
		
		BasicPersonTripAnalysisHandler basicHandlerlastIterationDir1;
		BasicPersonTripAnalysisHandler basicHandlerLastIterationDir2;
		
		PersonMoneyLinkHandler moneyHandlerLastIterationDir1;
		PersonMoneyLinkHandler moneyHandlerLastIterationDir2;
		
		{
			Config config = ConfigUtils.createConfig();	
			config.plans().setInputFile(dir1populationFile);
			config.network().setInputFile(dir1networkFile);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
	        
	        basicHandlerlastIterationDir1 = new BasicPersonTripAnalysisHandler();
			basicHandlerlastIterationDir1.setScenario(scenario);
			
			moneyHandlerLastIterationDir1 = new PersonMoneyLinkHandler();
			moneyHandlerLastIterationDir1.setBasicHandler(basicHandlerlastIterationDir1);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandlerlastIterationDir1);
			events.addHandler(moneyHandlerLastIterationDir1);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir1lastIterationFile);
		}
		
		{
			Config config = ConfigUtils.createConfig();	
			config.plans().setInputFile(dir2populationFile);
			config.network().setInputFile(dir2networkFile);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
	        
	        basicHandlerLastIterationDir2 = new BasicPersonTripAnalysisHandler();
			basicHandlerLastIterationDir2.setScenario(scenario);
			
			moneyHandlerLastIterationDir2 = new PersonMoneyLinkHandler();
			moneyHandlerLastIterationDir2.setBasicHandler(basicHandlerLastIterationDir2);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandlerLastIterationDir2);
			events.addHandler(moneyHandlerLastIterationDir2);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir2lastIterationFile);
			
			for (Person person : scenario.getPopulation().getPersons().values()) {
				int actCounter = 1;
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						
						if (act.getType().startsWith("home")) {
							personId2homeActCoord.put(person.getId(), act.getCoord());
						}
						
						if (actCounter == 1) {
							Map<Integer, Coord> actNr2Coord = new HashMap<>();
							actNr2Coord.put(actCounter, act.getCoord());
							personId2actNr2coord.put(person.getId(), actNr2Coord);
						
						} else {
							personId2actNr2coord.get(person.getId()).put(actCounter, act.getCoord());
						}
						
						actCounter++;
					}
				}
			}
		}

        ModeSwitchersTripTime modeSwitchersTripTime = new ModeSwitchersTripTime();
        modeSwitchersTripTime.processEventsFile(dir2firstIterationFile, dir2lastIterationFile);
        Map<Id<Person>, List<Tuple<String, String>>> personId2ModeSwitches =  modeSwitchersTripTime.getPersonId2ModeSwitcherRetainerTripInfo();

        BufferedWriter writerCar2X = IOUtils.getBufferedWriter( directory2 + analysisOutputFolder + "/modeSwitchAnalysis_carToXInfo.txt");
        BufferedWriter writerx2Car = IOUtils.getBufferedWriter( directory2 + analysisOutputFolder + "/modeSwitchAnalysis_xToCarInfo.txt");
        BufferedWriter writerCar2Car = IOUtils.getBufferedWriter( directory2 + analysisOutputFolder + "/modeSwitchAnalysis_carToCarInfo.txt");
        BufferedWriter writerAllCarTrips = IOUtils.getBufferedWriter( directory2 + analysisOutputFolder + "/modeSwitchAnalysis_allCarTrips.txt");
        
        writerCar2X.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\tcongestionToll-r\tnoiseToll-r\tairPollutionToll-r\tcongestionToll-m+r\tnoiseToll-m+r\tairPollutionToll-m+r\n");
        writerx2Car.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\tcongestionToll-r\tnoiseToll-r\tairPollutionToll-r\tcongestionToll-m+r\tnoiseToll-m+r\tairPollutionToll-m+r\n");
        writerCar2Car.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\tcongestionToll-r\tnoiseToll-r\tairPollutionToll-r\tcongestionToll-m+r\tnoiseToll-m+r\tairPollutionToll-m+r\n");
        writerAllCarTrips.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\tcongestionToll-r\tnoiseToll-r\tairPollutionToll-r\tcongestionToll-m+r\tnoiseToll-m+r\tairPollutionToll-m+r\n");

        for(Id<Person> personId : personId2ModeSwitches.keySet()) {
            int tripIndex = 1;
            for (Tuple<String, String> modeSwitch : personId2ModeSwitches.get(personId)) {                   
               
            	double congestionPaymentR = 0.;
				double noisePaymentR = 0.;
				double airPollutionPaymentR = 0.;
				
				double congestionPaymentMR = 0.;
				double noisePaymentMR = 0.;
				double airPollutionPaymentMR = 0.;

				if (moneyHandlerLastIterationDir1.getPersonId2tripNumber2congestionPayment().get(personId) != null
						&& moneyHandlerLastIterationDir1.getPersonId2tripNumber2congestionPayment().get(personId).get(tripIndex) != null) {
					congestionPaymentR = moneyHandlerLastIterationDir1.getPersonId2tripNumber2congestionPayment().get(personId).get(tripIndex);
				}
				
				if (moneyHandlerLastIterationDir1.getPersonId2tripNumber2noisePayment().get(personId) != null
						&& moneyHandlerLastIterationDir1.getPersonId2tripNumber2noisePayment().get(personId).get(tripIndex) != null) {
					noisePaymentR = moneyHandlerLastIterationDir1.getPersonId2tripNumber2noisePayment().get(personId).get(tripIndex);
				}
				
				if (moneyHandlerLastIterationDir1.getPersonId2tripNumber2airPollutionPayment().get(personId) != null
						&& moneyHandlerLastIterationDir1.getPersonId2tripNumber2airPollutionPayment().get(personId).get(tripIndex) != null) {
					airPollutionPaymentR = moneyHandlerLastIterationDir1.getPersonId2tripNumber2airPollutionPayment().get(personId).get(tripIndex);
				}
				
				if (moneyHandlerLastIterationDir2.getPersonId2tripNumber2congestionPayment().get(personId) != null
						&& moneyHandlerLastIterationDir2.getPersonId2tripNumber2congestionPayment().get(personId).get(tripIndex) != null) {
					congestionPaymentMR = moneyHandlerLastIterationDir2.getPersonId2tripNumber2congestionPayment().get(personId).get(tripIndex);
				}
				
				if (moneyHandlerLastIterationDir2.getPersonId2tripNumber2noisePayment().get(personId) != null
						&& moneyHandlerLastIterationDir2.getPersonId2tripNumber2noisePayment().get(personId).get(tripIndex) != null) {
					noisePaymentMR = moneyHandlerLastIterationDir2.getPersonId2tripNumber2noisePayment().get(personId).get(tripIndex);
				}
				
				if (moneyHandlerLastIterationDir2.getPersonId2tripNumber2airPollutionPayment().get(personId) != null
						&& moneyHandlerLastIterationDir2.getPersonId2tripNumber2airPollutionPayment().get(personId).get(tripIndex) != null) {
					airPollutionPaymentMR = moneyHandlerLastIterationDir2.getPersonId2tripNumber2airPollutionPayment().get(personId).get(tripIndex);
				}
				
				// all car trips
				
				if (modeSwitch.getFirst().equals(TransportMode.car) || modeSwitch.getSecond().equals(TransportMode.car)) {
					writerAllCarTrips.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+tripIndex+"\t"
		                	+ congestionPaymentR + "\t"
		                    + noisePaymentR + "\t"
		                    + airPollutionPaymentR + "\t"
		                    + congestionPaymentMR + "\t"
		                    + noisePaymentMR + "\t"
		                    + airPollutionPaymentMR + "\t"
		                	+"\n");
				}
				
				// switch type
				
				
            	if (modeSwitch.getFirst().equals(modeSwitch.getSecond())) {
					
					if (modeSwitch.getFirst().equals(TransportMode.car)) {
						
						modeRetainersOrigin.put(personId + "Trip" + tripIndex, personId2actNr2coord.get(personId).get(tripIndex));
						modeRetainersDestination.put(personId + "Trip" + (tripIndex), personId2actNr2coord.get(personId).get(tripIndex + 1));
						if (personId2homeActCoord.get(personId) != null) {
							modeRetainersHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
						}
						
						writerCar2Car.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+tripIndex+"\t"
			                	+ congestionPaymentR + "\t"
			                    + noisePaymentR + "\t"
			                    + airPollutionPaymentR + "\t"
			                    + congestionPaymentMR + "\t"
			                    + noisePaymentMR + "\t"
			                    + airPollutionPaymentMR + "\t"
			                	+"\n");
					}
            	
            	} else if (modeSwitch.getFirst().equals(TransportMode.car)) {
                	// car --> x
                	car2xOrigin.put(personId + "Trip" + tripIndex, personId2actNr2coord.get(personId).get(tripIndex));
                	car2xDestination.put(personId + "Trip" + (tripIndex), personId2actNr2coord.get(personId).get(tripIndex + 1));
					if (personId2homeActCoord.get(personId) != null) {
						car2xHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
					}

                	writerCar2X.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+tripIndex+"\t"
                			+ congestionPaymentR + "\t"
		                    + noisePaymentR + "\t"
		                    + airPollutionPaymentR + "\t"
		                    + congestionPaymentMR + "\t"
		                    + noisePaymentMR + "\t"
		                    + airPollutionPaymentMR + "\t"
                			+"\n");

                } else if (modeSwitch.getSecond().equals(TransportMode.car)) {
                	// x --> car
                	x2carOrigin.put(personId + "Trip" + tripIndex, personId2actNr2coord.get(personId).get(tripIndex));
                	x2carDestination.put(personId + "Trip" + (tripIndex), personId2actNr2coord.get(personId).get(tripIndex + 1));
					if (personId2homeActCoord.get(personId) != null) {
	                	x2carHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
					}

                	writerx2Car.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+tripIndex+"\t"
                			+ congestionPaymentR + "\t"
		                    + noisePaymentR + "\t"
		                    + airPollutionPaymentR + "\t"
		                    + congestionPaymentMR + "\t"
		                    + noisePaymentMR + "\t"
		                    + airPollutionPaymentMR + "\t"
                			+"\n");
                }
                tripIndex++;
            }
        }
        writerCar2X.close();
        writerx2Car.close();
        writerCar2Car.close();  
        writerAllCarTrips.close();
        
        // coordinates
        
        printCoordinates(modeRetainersOrigin, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_car2car_origin.csv");
        printCoordinates(modeRetainersDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_car2car_destination.csv");
        printCoordinates(modeRetainersHomeCoord, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_car2car_homeCoord.csv");
        printODLines(modeRetainersOrigin, modeRetainersDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_car2car_OD.shp");
        
        printCoordinates(car2xOrigin, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_car2x_origin.csv");
        printCoordinates(car2xDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_car2x_destination.csv");
        printCoordinates(car2xHomeCoord, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_car2x_homeCoord.csv");
        printODLines(car2xOrigin, car2xDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_car2x_OD.shp");

        printCoordinates(x2carOrigin, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_x2car_origin.csv");
        printCoordinates(x2carDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_actCoord_x2car_destination.csv");
        printCoordinates(x2carHomeCoord, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_x2car_homeCoord.csv");
        printODLines(x2carOrigin, x2carDestination, directory2 + analysisOutputFolder + "/modeSwitchAnalysis_x2car_OD.shp");

    }

	private void printCoordinates(Map<String, Coord> id2Coord, String fileName) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        writer.write("Id;xCoord;yCoord");
        writer.newLine();
        for (String personTripNr : id2Coord.keySet()) {
        	writer.write(personTripNr + ";" + id2Coord.get(personTripNr).getX() + ";" + id2Coord.get(personTripNr).getY());
        	writer.newLine();
        } 
        writer.close();
	}
	
	private void printODLines(Map<String, Coord> id2CoordOrigin, Map<String, Coord> id2CoordDestination, String fileName) throws IOException {
        
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
        		.setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4))
        		.setName("TripOD")
        		.addAttribute("PersTripId", String.class)
        		.create();
        		
        		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
        						
                for (String personTripNr : id2CoordOrigin.keySet()) {
                	SimpleFeature feature = factory.createPolyline(
    						
                			new Coordinate[] {
    								new Coordinate(MGC.coord2Coordinate(id2CoordOrigin.get(personTripNr))),
    								new Coordinate(MGC.coord2Coordinate(id2CoordDestination.get(personTripNr))) }
    						
    						, new Object[] {personTripNr}
                			, null
    				);
    				features.add(feature);
        		}
        		
        		ShapeFileWriter.writeGeometries(features, fileName);
	}
}
