/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionHandler.java
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
package playground.benjamin.emissions;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.dataTypes.HbefaColdEmissionTableCreator;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreator;
import playground.benjamin.emissions.dataTypes.HbefaWarmEmissionTableCreatorDetailed;
import playground.benjamin.emissions.dataTypes.VisumRoadTypes;

/**
 * @author benjamin
 *
 */
public class EmissionHandler {
	private static final Logger logger = Logger.getLogger(EmissionHandler.class);
	
	private final String vehicleFile = "../../detailedEval/emissions/testScenario/input/vehicles.xml";
	
	private final String visum2hbefaRoadTypeFile = "../../detailedEval/emissions/hbefaForMatsim/road_types.txt";
	private final String visum2hbefaRoadTypeTrafficSituationFile = "../../detailedEval/emissions/hbefaForMatsim/road_types_trafficSituation.txt";
	
	private String warmEmissionFactorsFile = null ;
	private final String coldEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_coldstart_emission_factors.txt";
	private final String averageFleetEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW.txt";
	private final String averageFleetHdvEmissionFactorsFile = "../../detailedEval/emissions/hbefaForMatsim/hbefa_emission_factors_urban_rural_MW_hdv.txt";
	
	private static EventWriterXML emissionEventWriter;
	
	public void createLookupTables() {
		
	}

	public void installEmissionEventHandler(Scenario scenario, EventsManager eventsManager, String emissionEventOutputFile) {
		logger.info("entering installEmissionsEventHandler") ;
		
		Network network = scenario.getNetwork() ;
		
		warmEmissionFactorsFile = scenario.getConfig().vspExperimental().getEmissionFactorsFile() ;
		
		EventsManager emissionEventsManager = EventsUtils.createEventsManager();
		
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreator = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreator.makeHbefaWarmTable(averageFleetEmissionFactorsFile);
		HbefaWarmEmissionTableCreator hbefaAvgWarmEmissionTableCreatorHDV = new HbefaWarmEmissionTableCreator();
		hbefaAvgWarmEmissionTableCreatorHDV.makeHbefaWarmTable(averageFleetHdvEmissionFactorsFile);
		HbefaColdEmissionTableCreator hbefaAvgColdEmissionTableCreator = new HbefaColdEmissionTableCreator();
		hbefaAvgColdEmissionTableCreator.makeHbefaColdTable(coldEmissionFactorsFile);
		HbefaWarmEmissionTableCreatorDetailed hbefaWarmEmissionTableCreatorDetailed = new HbefaWarmEmissionTableCreatorDetailed();
		hbefaWarmEmissionTableCreatorDetailed.makeHbefaWarmTableDetailed(warmEmissionFactorsFile);
		
		// read the vehicle file
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehicleReaderV1 vehicleReader = new VehicleReaderV1(vehicles);
		vehicleReader.readFile(vehicleFile);

		// TODO: make the following homogeneous?!?
		VisumRoadTypes[] roadTypes = createRoadTypes(visum2hbefaRoadTypeFile);
		
		for(int i=0; i<100; i++){
			logger.info(roadTypes[i].getVISUM_RT_NR());
			logger.info(roadTypes[i].getVISUM_RT_NAME());
			logger.info(roadTypes[i].getHBEFA_RT_NR());
			logger.info(roadTypes[i].getHBEFA_RT_NAME());
		}
		
		String[][] roadTypesTrafficSituations = createRoadTypesTafficSituation(visum2hbefaRoadTypeTrafficSituationFile, roadTypes);
		
		for(int i=0; i<100; i++){
			logger.info(roadTypesTrafficSituations[i][0]);
			logger.info(roadTypesTrafficSituations[i][1]);
			logger.info(roadTypesTrafficSituations[i][2]);
			logger.info(roadTypesTrafficSituations[i][3]);
		}
		
		// instantiate analysis modules
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = new WarmEmissionAnalysisModule(
				roadTypes,
				roadTypesTrafficSituations,
				hbefaWarmEmissionTableCreatorDetailed,
				hbefaAvgWarmEmissionTableCreator,
				hbefaAvgWarmEmissionTableCreatorHDV,
				emissionEventsManager);
		ColdEmissionAnalysisModule coldEmissionAnalysisModule = new ColdEmissionAnalysisModule ();
		
		// create the different emission handler
		WarmEmissionHandler warmEmissionHandler = new WarmEmissionHandler(
				vehicles,
				network,
				warmEmissionAnalysisModule);
		ColdEmissionHandler coldEmissionHandler = new ColdEmissionHandler(
				network,
				hbefaAvgColdEmissionTableCreator,
				coldEmissionAnalysisModule,
				emissionEventsManager);
		
		// create the writer for emission events
		emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionEventsManager.addHandler(emissionEventWriter);
		
		// add the handler
		eventsManager.addHandler(warmEmissionHandler);
		eventsManager.addHandler(coldEmissionHandler);
		
		logger.info("leaving installEmissionsEventHandler") ;
	}

	public EventWriterXML getEmissionEventWriter() {
		return emissionEventWriter;
	}

	private String[][] createRoadTypesTafficSituation(String filename, VisumRoadTypes[] roadTypes ) {
		logger.info("entering createRoadTypesTafficSituation ...") ;

		String[][] roadTypesTrafficSituations = new String[100][4];
		int[] counter = new int[100];
		for(int i=0; i<100;i++)
			counter[i]=0;
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine="";
			//Read File Line By Line
			strLine = br.readLine();
			
//			FileWriter out = new FileWriter("../../detailedEval/emissions/hbefaForMatsim/asdf.txt") ;
//			out.write(strLine+"\n") ;
	
			while ((strLine = br.readLine()) != null){
				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(";");
				int roadtype=Integer.valueOf(array[0]);
				int traficSitIndex = counter[roadtype]++;
//				roadTypesTrafficSituations[roadtype][traficSitIndex] = array[3];
				roadTypesTrafficSituations[roadtype][traficSitIndex] = array[4];
				
//				final String str1 = roadTypes[roadtype].getVISUM_RT_NR() + ";";
//				System.out.print(str1) ;
//				final String str2 = roadTypes[roadtype].getVISUM_RT_NAME() + ";";
//				System.out.print(str2) ;
//
//				final String str3 = roadTypes[roadtype].getHBEFA_RT_NR() + ";";
//				System.out.print(str3) ;
//				final String str4 = roadTypes[roadtype].getHBEFA_RT_NAME() + ";";
//				System.out.print(str4) ;
//
////				for ( int ii=0 ; ii<array.length; ii++ ) {
////					System.out.print(array[ii] + ";") ;
////				} 
//				final String str5 = array[3] + ";\n";
//				System.out.print(str5) ;
//				
//				out.write(str1+str2+str3+str4+str5);
				
			}
			in.close();
//			out.close();
//			throw new RuntimeException("stopping here") ;
			
			logger.info("leaving createRoadTypesTafficSituation ...") ;

			return roadTypesTrafficSituations;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}
	
	VisumRoadTypes[] createRoadTypes(String filename){
		logger.info("entering createRoadTypes ...") ;
		
		VisumRoadTypes[] roadTypes = new VisumRoadTypes[100];
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			br.readLine();
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) {
					throw new RuntimeException("cannot handle this character in parsing") ;
				}
	
				//for all lines we split the line to an array 
				String[] array = strLine.split(",");
				VisumRoadTypes obj = new VisumRoadTypes(Integer.parseInt(array[0]), Integer.parseInt(array[2]));
				obj.setVISUM_RT_NAME(array[1]) ;
				if ( array.length >=4 ) {
					obj.setHBEFA_RT_NAME(array[3]) ;
				} else {
					obj.setHBEFA_RT_NAME("no hbefa road type name in file") ;
				}
				roadTypes[obj.getVISUM_RT_NR()] = obj;
			}
			in.close();

			logger.info("leaving createRoadTypes ...") ;
			return roadTypes;
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
	}
}
