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
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.run.Events2Snapshot;
import org.matsim.vis.snapshotwriters.TransimsSnapshotWriter.Labels;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * 1) Create Transims snapshot file from events or use existing file. 
 * 2) read events 
 * 3) Write data in simpler form for space time plotting.
 * <b> At the moment, this should be only used for race track experiment. The methodology can work for other scenarios.
 * But, some parameters many need to change and yet to be tested. 
 * @author amit 
 */

public class AgentPositionWriter {

	private final static Logger LOGGER = Logger.getLogger(AgentPositionWriter.class);
	private final static boolean IS_WRITING_TRANSIM_FILE = false;
	private final double linkLength = 1000;
	private final double trackLength = 3000;
	private final double maxSpeed = 60/3.6;
	private final Scenario sc;
	private int warnCount_higherSpeed = 0;
	private int warnCount_negativeVehPosition = 0;

	public static void main(String[] args) 
	{
		final String dir = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/holes/car_SW/";
		final String networkFile = dir+"/network.xml";
		final String configFile = dir+"/config.xml";
		final String prefix = "10";
		final String eventsFile = dir+"/events/events["+prefix+"].xml";

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(networkFile, configFile);
		final SnapshotStyle snapshotStyle = sc.getConfig().qsim().getSnapshotStyle();

		AgentPositionWriter apw = new AgentPositionWriter(dir+"/snapshotFiles/rDataPersonPosition_"+prefix+"_"+snapshotStyle+".txt", sc);
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
	public AgentPositionWriter(String outFile, Scenario scenario)
	{
		this.sc = scenario;
		writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("personId \t legMode \t positionOnLink \t time \t speed  \t cycleNumber \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	public String createAndReturnTransimSnapshotFile(Scenario sc, String eventsFile)
	{
		Events2Snapshot e2s = new Events2Snapshot();
		File file = new File(eventsFile);
		e2s.run(file, sc.getConfig(), sc.getNetwork());
		return file.getParent() + "/" +"T.veh";
	}

	public void storePerson2Modes(String eventsFile)
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
	private final Map<Id<Person>,Double> prevTime = new HashMap<>();
	private final Map<Id<Person>,Double> prevTrackPositions = new HashMap<>();
	private final Map<Id<Person>,Integer> prevCycle = new HashMap<>();
	private final Map<Id<Person>,Double> prevLink = new HashMap<>();

	public void readTransimFileAndWriteData(String inputFile)
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

					Tuple<Double, Double> posAndLinkId = getDistanceFromFromNode ( new Coord(easting , northing) ); 
					if (posAndLinkId == null) return;

					double currentPositionOnLink = posAndLinkId.getFirst();
					double linkId = posAndLinkId.getSecond();

					double velocity = Double.NaN ;
					Double trackPosition = 0. ;

					Double prevPosition = prevTrackPositions.get(agentId);

					if (prevTime.containsKey(agentId)) {
						double timegap = time - prevTime.get(agentId);

						trackPosition =  linkId  * linkLength  + currentPositionOnLink;

						if(trackPosition < 0. || trackPosition > trackLength) {

							throw new RuntimeException("Position can not be negative or higher than track length ("+trackLength+"m).");

						} else if(trackPosition < prevPosition  && linkId == prevLink.get(agentId) ){ 

							// prev = 1986 and now = 1980 should not happen however, prev = 2993, now = 6 is possible.
							//							throw new RuntimeException("Should not happen.");
							if(warnCount_negativeVehPosition < 1){
								warnCount_negativeVehPosition++;
								LOGGER.warn("Prev position is "+prevPosition+" and current position is "+ trackPosition+". "
										+ "\n This happens due to filling the holes. I am not sure if this is absolutely right.");
								LOGGER.warn(Gbl.ONLYONCE);
							}
//							trackPosition = prevPosition;
						} else if ( Math.round(trackPosition) == Math.round(prevPosition) ) {

							// same cycle, but rounding errors, for e.g. first position 933.33 and next position 933.0
							trackPosition = prevPosition;

						} else if ( trackPosition < prevPosition ) {// next cycle

							if ( trackPosition == 0.) {
								// agent is EXACTLY at the end of the track, thus need to write it twice.
								velocity =  Math.min( ( trackLength - prevPosition ) / timegap , maxSpeed) ;
								writeString(agentId+"\t"+person2mode.get(agentId)+"\t"+ trackLength +"\t"+time+"\t"+velocity+"\t"+prevCycle.get(agentId)+"\n");
							} else {
								velocity = ( trackPosition + trackLength - prevPosition ) / timegap;
							}
							prevCycle.put(agentId, prevCycle.get(agentId) + 1 ); 
						} 

						if( Double.isNaN(velocity)) {
							velocity = Math.abs( ( trackPosition - prevPosition ) / timegap );	
						} else if(velocity < 0. ) {
							System.err.println("In appropriate speed "+velocity);
						} 

						if(Math.round(velocity) > Math.round(maxSpeed) ){
							// sometimes its positioning errors from simulation snapshots
							if (velocity <= 50.0 ) {
								if(warnCount_higherSpeed<1){
									warnCount_higherSpeed++;
									LOGGER.warn("Velocity is "+velocity+".This comes from the error in positioning "
											+ "during snapshot generation in the simulation. Setting it to max speed.");
									LOGGER.warn("Prev and current track positions are " + prevPosition +", " + trackPosition +" and time gap between two snapshots is "+ timegap );
									LOGGER.warn(Gbl.ONLYONCE);
								}
							} else {
								LOGGER.info("Prev and current track positions are " + prevPosition +", " + trackPosition +" and time gap between two snapshots is "+ timegap );
								LOGGER.error("Velocity is "+velocity+". This is way too high.");
//								throw new RuntimeException("Velocity is "+velocity+". This is way too high.");
							}
							velocity = Math.min(velocity, maxSpeed);
						}

						prevTrackPositions.put(agentId, trackPosition);
						prevLink.put(agentId, linkId);
					} else {
						velocity = maxSpeed;
						prevCycle.put(agentId, 1);
						prevTrackPositions.put(agentId, currentPositionOnLink);
						prevLink.put(agentId, linkId);
					}

					prevTime.put(agentId, time);

					writeString(agentId+"\t"+person2mode.get(agentId)+"\t"+prevTrackPositions.get(agentId) +"\t"+time+"\t"+velocity+"\t"+prevCycle.get(agentId)+"\n");

				}
			}
		} ;
		TabularFileParser reader = new TabularFileParser() ;
		reader.parse(config, handler);
		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}


	private void writeString(String buffer) {
		try {
			writer.write(buffer);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	/**
	 * This should work for all types of network provided easting and northing are free from any corrections for positioning in the 2D space.
	 */
	private Tuple<Double, Double> getDistanceFromFromNode (final Coord eastinNorthing) {
		// check if easting is on "Home" or "Work" links
		if (eastinNorthing.getX() < 0 || eastinNorthing.getX() > 1000) return null;

		double distFromFromNode = Double.NaN;
		double linkId = Double.NaN;
		for (Link l : sc.getNetwork().getLinks().values()) {
			Coord fromNode = l.getFromNode().getCoord();
			Coord toNode = l.getToNode().getCoord();

			double distFromNodeAndPoint = NetworkUtils.getEuclideanDistance(eastinNorthing, fromNode); // alternatively use Point2D.distance(...)
			double distPointAndToNode = NetworkUtils.getEuclideanDistance(eastinNorthing, toNode);
			double distFromNodeAndToNode = NetworkUtils.getEuclideanDistance(fromNode, toNode);

			if ( Math.abs( distFromNodeAndPoint + distPointAndToNode - distFromNodeAndToNode ) < 0.01) { 
				// 0.01 to ignore rounding errors, In general, if AC + CB = AB, C lies on AB
				distFromFromNode = Math.round(distFromNodeAndPoint*100) / 100.0; 
				linkId = Double.valueOf( l.getId().toString() );
				break;
			}
		}
		if( Double.isNaN(distFromFromNode) ) { // this is possible, because of minor errors --

			Link nearestLink = NetworkUtils.getNearestLink(sc.getNetwork(), eastinNorthing);
			assert nearestLink != null;
			linkId = Double.valueOf(nearestLink.getId().toString());
			distFromFromNode = NetworkUtils.getEuclideanDistance(eastinNorthing, nearestLink.getFromNode().getCoord());

			// since easting northing is slightly out, dist should not higher than link length
			distFromFromNode = Math.min(nearestLink.getLength(), distFromFromNode); 
			LOGGER.warn("Easting northing "+eastinNorthing.toString() + " is not found to be on the link. \n"
					+ "Thus, using, nearest link to get the distance.");
			LOGGER.info("Thus, identified link is "+nearestLink.getId()+" and the distance of the point from from node is "+distFromFromNode);
			//throw new RuntimeException("Easting, northing ("+ eastinNorthing.getX() +","+eastinNorthing.getY() +") is outside the network.");
		}
		return new Tuple<>(distFromFromNode, linkId);
	}
}
