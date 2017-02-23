/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.qPositionPlots;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.run.Events2Snapshot;
import org.matsim.vis.snapshotwriters.TransimsSnapshotWriter.Labels;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * 1) Create Transims snapshot file from events or use given Transims snapshot file. 
 * 2) Read events to get mode information 
 * 3) Write data in simpler form for space time plotting.
 *  
 *  @deprecated
 *  The distance between two subsequent easting, northing does not result in the true position. 
 *  For e.g.\ Take two points on either edges of a vertex as subsequent points ... (993.0,0) and (991,14)
 *  In the FD scenario set up, the true distance is not the distance between two points 
 *  instead, sum of distance between (993.0,0) (1000.0,0.0) and (1000,0) (991,14).
 *  This is here only for documentation, I will remove the whole class eventually.
 *  
 *  Alternatively see {@link AgentPositionWriter}
 * @author amit 
 */

@Deprecated
public class AgentPositionWriterOld {

	private final static Logger LOGGER = Logger.getLogger(AgentPositionWriterOld.class);
	private final static boolean IS_WRITING_TRANSIM_FILE = false;
	private final double trackLength = 3000;
	private final double maxSpeed = 60/3.6;

	public static void main(String[] args) 
	{
		final String dir = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/holes/car_SW_kn/";
		final String networkFile = dir+"/network.xml";
		final String configFile = dir+"/config.xml";
		final String prefix = "5";
		final String eventsFile = dir+"/events/events["+prefix+"].xml";

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);
		final SnapshotStyle snapshotStyle = sc.getConfig().qsim().getSnapshotStyle();

		AgentPositionWriterOld apw = new AgentPositionWriterOld(dir+"/snapshotFiles/position_"+prefix+"_"+snapshotStyle+".txt");
		String transimFile;

		if( IS_WRITING_TRANSIM_FILE ){
			// not sure, if following three lines are required.
			sc.getConfig().qsim().setLinkWidthForVis((float)0);
			sc.getNetwork().setEffectiveLaneWidth(0.);

			sc.getConfig().controler().setSnapshotFormat(Arrays.asList("transims"));
			transimFile = apw.createAndReturnTransimSnapshotFile(sc, eventsFile);
		} else {
			transimFile = dir+"/TransVeh/T_["+prefix+"].veh.gz";
		}

		apw.storePerson2Modes(eventsFile);
		apw.readTransimFileAndWriteData(transimFile);
	}

	/**
	 * Constructor opens writer, creates transims file and stores person 2 mode from events file.
	 */
	public AgentPositionWriterOld(String outFile)
	{
		writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("personId \t legMode \t positionOnLink \t time \t speed  \t cycleNumber \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	private String createAndReturnTransimSnapshotFile(Scenario sc, String eventsFile)
	{
		Events2Snapshot e2s = new Events2Snapshot();
		File file = new File(eventsFile);
		e2s.run(file, sc.getConfig(), sc.getNetwork());
		return file.getParent() + "/" +"T.veh";
	}

	private void storePerson2Modes(String eventsFile)
	{
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonDepartureEventHandler() {
			@Override
			public void reset(int iteration) {
				person2mode.clear();
			}
			@Override
			public void handleEvent(PersonDepartureEvent event) {
				person2mode.put(event.getPersonId(), event.getLegMode());
			}
		});
		new MatsimEventsReader(manager).readFile(eventsFile);
	}

	private final BufferedWriter writer ;

	private final Map<Id<Person>,String> person2mode = new HashMap<>();
	private final Map<Id<Person>,Tuple<Double,Double>> prevEastingNorthing = new HashMap<>();
	private final Map<Id<Person>,Double> prevPosition = new HashMap<>();
	private final Map<Id<Person>,Double> prevTime = new HashMap<>();
	private final Map<Id<Person>,Integer> prevCycle = new HashMap<>();

	private void readTransimFileAndWriteData(String inputFile)
	{
		LOGGER.info("Reading transim file "+ inputFile);

		TabularFileParserConfig config = new TabularFileParserConfig() ;
		config.setFileName( inputFile );
		config.setDelimiterTags( new String []{"\t"} );
		// ---
		TabularFileHandler handler = new TabularFileHandler(){
			final List<String> labels = new ArrayList<>() ;
			@Override
			public void startRow(String[] row) {
				List<String> strs = Arrays.asList( row ) ;

				if ( row[0].substring(0, 1).matches("[A-Za-z]") ) {
					if ( row[0].startsWith("hole") ) return; 
					for ( String str : strs ) {
						labels.add( str ) ;
					}
				} else {
					double time = Double.parseDouble( strs.get( labels.indexOf( Labels.TIME.toString() ) ) ) ;
					double easting = Double.parseDouble( strs.get( labels.indexOf( Labels.EASTING.toString() ) ) ) ;
					double northing = Double.parseDouble( strs.get( labels.indexOf( Labels.NORTHING.toString() ) ) ) ;
					Id<Person> agentId = Id.createPersonId( strs.get( labels.indexOf( Labels.VEHICLE.toString() ) ) ) ;
					try {
						if( prevEastingNorthing.containsKey(agentId) ){
							Tuple<Double,Double> eastingNorthing = prevEastingNorthing.get(agentId);
							double currentDist = Math.sqrt( (easting - eastingNorthing.getFirst() )*(easting - eastingNorthing.getFirst()) 
									+ (northing- eastingNorthing.getSecond())*(northing- eastingNorthing.getSecond() ) );
							
							double timegap =  time - prevTime.get(agentId);
							double velocity = currentDist / (timegap); // denominator should be equal to snapshot period.
							if(Math.round( velocity) > Math.round( maxSpeed) ) { 
								// person arriving (vehicle leaving traffic) and rounding errors 
								velocity = maxSpeed;
							}else if (velocity < 0.0) throw new RuntimeException("Speed can not be negative. Aborting ...");
						
							double position = prevPosition.get(agentId) + currentDist ;
							if(position > trackLength) {
								position = position-trackLength;
								prevCycle.put(agentId, prevCycle.get(agentId)+1);
							}
							
							writer.write(agentId+"\t"+person2mode.get(agentId)+"\t"+position+"\t"+time+"\t"+velocity+"\t"+prevCycle.get(agentId)+"\n");
							prevPosition.put(agentId, position);
						}  else {
							writer.write(agentId+"\t"+person2mode.get(agentId)+"\t"+0.+"\t"+time+"\t"+maxSpeed+"\t"+"1"+"\n");
							prevPosition.put(agentId, 0.);
							prevCycle.put(agentId, 1);
						}
						prevEastingNorthing.put(agentId, new Tuple<>(easting, northing));
						prevTime.put(agentId, time);
					} catch (Exception e) {
						throw new RuntimeException("Data is not written to the file. Reason :"+e);
					}
				}
			}
		} ;
		TabularFileParser reader = new TabularFileParser() ;
		reader.parse(config, handler);
		try{
			LOGGER.info("Data writing is complete.");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}
}