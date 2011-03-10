/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimMeasurement.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.tnicolai.urbansim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.MATSimConfigObject;
import playground.tnicolai.urbansim.utils.MeasurementObject;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
import playground.toronto.ttimematrix.SpanningTree;


/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimMeasurement extends MATSim4Urbansim {
	
	private MeasurementObject measurements;
	
	public MATSim4UrbanSimMeasurement(String args[]){
		super(args);
		measurements = MeasurementObject.getInstance();
	}
	
	protected void ReadUrbansimParcelModel(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl facilities, ActivityFacilitiesImpl zones){
		
		long startTimeReadFacilities, endTimeReadFacilities, startTimeWriteFacilities, endTimeWriteFacilities;
		File output = new File(Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "locations.xml.gz");
		
		if(measurements != null){
			try {
				startTimeReadFacilities = System.currentTimeMillis();
				readFromUrbansim.readFacilities(facilities, zones);
				endTimeReadFacilities = System.currentTimeMillis();
				
				startTimeWriteFacilities = System.currentTimeMillis();
				// write the facilities from the urbansim parcel model as a compressed locations.xml file into the temporary directory as input for ???
				new FacilitiesWriter(facilities).write( output.getCanonicalPath() );
				endTimeWriteFacilities = System.currentTimeMillis();
				
				// store measurements
				
				// read and build facilities
				MeasurementObject.setDurationReadFacilities(endTimeReadFacilities-startTimeReadFacilities);
				
				// write facilities xml
				MeasurementObject.setDurationWriteFacilities(endTimeWriteFacilities-startTimeWriteFacilities);
				MeasurementObject.setFacilitiesOutputSize( output.length() );
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected Population readUrbansimPersons(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl facilities, NetworkImpl network){
		// read urbansim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;
		Population newPopulation = null;
		long startTimeReadPersons, endTimeReadPersons, startTimeWritePersons, endTimeWritePersons;
		File output = new File(Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "pop.xml.gz");
		
		if(measurements != null){
			try {
				if ( config.plans().getInputFile() != null ) 
					oldPopulation = scenario.getPopulation();
				else
					oldPopulation = null;
		
				newPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
				// read urbansim persons.  Generates hwh acts as side effect
				startTimeReadPersons = System.currentTimeMillis();
				readFromUrbansim.readPersons( oldPopulation, newPopulation, facilities, network, MATSimConfigObject.getSampeRate() ) ;
				endTimeReadPersons = System.currentTimeMillis();
				oldPopulation=null ;
				System.gc() ;
				
				startTimeWritePersons = System.currentTimeMillis();
				new PopulationWriter(newPopulation,network).write( output.getCanonicalPath() );
				endTimeWritePersons = System.currentTimeMillis();
				
				// store measurements
				
				// read read and build persons
				MeasurementObject.setDurationReadPersons(endTimeReadPersons-startTimeReadPersons);
				
				// write pop xml
				MeasurementObject.setDurationWritePersons(endTimeWritePersons-startTimeWritePersons);
				MeasurementObject.setPersonsOutputSize( output.length() );
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return newPopulation;
	}
	
	protected void runControler( ActivityFacilitiesImpl zones ){
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		// The following lines register what should be done _after_ the iterations were run:
		controler.addControlerListener( new MeasurementControlerListener(zones) );

		// run the iterations, including the post-processing:
		controler.run() ;
	}
	
	private void dumpMeasurements(){
		if(measurements != null)
			MeasurementObject.wirteLogfile();
	}
	
	/**
	 * Entry point
	 * @param args urbansim command prompt
	 * @return 0 if simulation successful, >0 else
	 */
	public static void main(String args[]){
		MATSim4UrbanSimMeasurement m4um = new MATSim4UrbanSimMeasurement(args);
		m4um.runMATSim();
		m4um.dumpMeasurements();
	}
	
	
	/**
	 * overwrites MyControlerListener in order to measure duration to write traveldata and its size.
	 * @author thomas
	 *
	 */
	private class MeasurementControlerListener implements ShutdownListener{
		
		private final Logger log = Logger.getLogger(MeasurementControlerListener.class);
		
		private MeasurementObject measurements;
		private ActivityFacilitiesImpl zones;
		private String travelDataPath;
		
		public MeasurementControlerListener( ActivityFacilitiesImpl zones ){
			measurements = MeasurementObject.getInstance();
			this.zones = zones;
			this.travelDataPath = Constants.OPUS_HOME + MATSimConfigObject.getTempDirectory() + "travel_data.csv";
		}
		
		public void notifyShutdown(ShutdownEvent event){
			
			long startTimeWriteTravelData, endTimeWriteTravelData;
			File output = new File(travelDataPath);
			
			if(measurements != null){
				startTimeWriteTravelData = System.currentTimeMillis();
				log.info("Entering notifyShutdown ..." ) ;

				// get the calling controler:
				Controler controler = event.getControler() ;

				TravelTime ttc = controler.getTravelTimeCalculator();
				//SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc));
				SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));

				NetworkImpl network = controler.getNetwork() ;
				double depatureTime = 8.*3600 ;
				st.setDepartureTime(depatureTime);

				try {
					BufferedWriter writer = IOUtils.getBufferedWriter( travelDataPath );

					log.info("Computing and writing travel_data" ) ;


					writer.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4" ) ; writer.newLine();

					System.out.println("|--------------------------------------------------------------------------------------------------|") ;
					long cnt = 0 ; long percentDone = 0 ;
					for ( ActivityFacility fromZone : zones.getFacilities().values() ) {
						if ( (int) (100.*cnt/zones.getFacilities().size()) > percentDone ) {
							percentDone++ ; System.out.print('.') ;
						}
						cnt++ ;
						Coord coord = fromZone.getCoord() ;
						assert( coord != null ) ;
						Node fromNode = network.getNearestNode( coord ) ;
						assert( fromNode != null ) ;
						st.setOrigin( fromNode ) ;
						st.run(network) ;
						for ( ActivityFacility toZone : zones.getFacilities().values() ) {
							Coord toCoord = toZone.getCoord() ;
							Node toNode = network.getNearestNode( toCoord ) ;
							double arrivalTime = st.getTree().get(toNode.getId()).getTime();
							double ttime = arrivalTime - depatureTime ;
							writer.write ( fromZone.getId().toString()
									+ "," + toZone.getId().toString()
									+ "," + ttime ) ;
							writer.newLine();
						}
					}
					writer.flush();
					writer.close();
					System.out.println(" ... done") ;
					log.info("... done with writing travel_data" ) ;

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				log.info("... ... done with notifyShutdown.") ;
				endTimeWriteTravelData = System.currentTimeMillis();
				
				// store measurements
				MeasurementObject.setDurationWriteTravelData(endTimeWriteTravelData-startTimeWriteTravelData);
				MeasurementObject.setTravelDataSize(output.length());
			}
		}
	}

}

