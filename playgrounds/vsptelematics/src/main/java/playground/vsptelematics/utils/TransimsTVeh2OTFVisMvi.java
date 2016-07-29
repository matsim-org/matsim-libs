/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsptelematics.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.TransimsSnapshotWriter.Labels;

/**
 * @author nagel
 *
 */
public class TransimsTVeh2OTFVisMvi implements MatsimReader {
	private double skipUntil = 0.0;
	private final List<SnapshotWriter> snapshotWriters = new ArrayList<>();
	private double snapshotPeriod = 1 ;
	private int lastSnapshotIndex = -1 ;
	Collection<AgentSnapshotInfo> positions = new ArrayList<>();
	
	public TransimsTVeh2OTFVisMvi() {
		throw new RuntimeException("this class seems to have all the necessary mechanics.  But I never tested it since I eventually "
				+ "moved on to a different track. kai, may'15") ;
	}

	@Override
	public void readFile(String filename) {

		TabularFileParser reader = new TabularFileParser() ;
		// ---
		TabularFileParserConfig config = new TabularFileParserConfig() ;
		config.setFileName( filename );
		// ---
		TabularFileHandler handler = new TabularFileHandler(){
			List<Labels> labels = new ArrayList<>() ;
			SnapshotLinkWidthCalculator widthCalculator = new SnapshotLinkWidthCalculator() ;
			AgentSnapshotInfoFactory factory = new AgentSnapshotInfoFactory(widthCalculator) ;
			@Override
			public void startRow(String[] row) {
				List<String> strs = Arrays.asList( row ) ;
				
				if ( row[0].matches("[a-zA-Z]") ) {
					for ( String str : strs ) {
						labels.add( Labels.valueOf( str ) ) ;
					}
				} else {
					double time = Double.parseDouble( strs.get( labels.indexOf( Labels.TIME ) ) ) ;
					testForAndPossiblyDoSnapshot(time);
					
					double easting = Double.parseDouble( strs.get( labels.indexOf( Labels.EASTING ) ) ) ;
					double northing = Double.parseDouble( strs.get( labels.indexOf( Labels.NORTHING ) ) ) ;
					double velocity = Double.parseDouble( strs.get( labels.indexOf( Labels.VELOCITY ) ) ) ;
					Id<Person> agentId = Id.create( strs.get( labels.indexOf( Labels.VEHICLE) ), Person.class ) ;
					double elevation = 0. ;
					double azimuth = 0. ;
					

					AgentSnapshotInfo snapshotInfo = factory.createAgentSnapshotInfo(agentId, easting, northing, elevation, azimuth) ;
					snapshotInfo.setColorValueBetweenZeroAndOne( velocity / (100./3.6) ); // assuming max_speed = 100km/h
					
					positions.add( snapshotInfo ) ;
					
					throw new RuntimeException("not implemented") ;
				}
			}
		} ;
		// ---
		reader.parse(config, handler);
		
	}

	private void testForAndPossiblyDoSnapshot(final double time) {
		int snapshotIndex = (int) (time / this.snapshotPeriod);
		if (this.lastSnapshotIndex == -1) {
			this.lastSnapshotIndex = snapshotIndex;
		}
		while (snapshotIndex > this.lastSnapshotIndex) {
			this.lastSnapshotIndex++;
			double snapshotTime = this.lastSnapshotIndex * this.snapshotPeriod;
			doSnapshot(snapshotTime);
		}
	}


	
	private void doSnapshot(final double time) {
		if (time >= skipUntil) {
			if (!this.snapshotWriters.isEmpty()) {
				for (SnapshotWriter writer : this.snapshotWriters) {
					writer.beginSnapshot(time);
					for (AgentSnapshotInfo position : positions) {
						writer.addAgent(position);
					}
					writer.endSnapshot();
				}
			}
			positions.clear(); 
		}
	}
	

	/**
	 *
	 * Allow this SnapshotGenerator to skip all snapshots up to, but not including, a give timestep.
	 * This is especially useful for interactive settings where a user may fast-forward.
	 * Snapshot generation is one of the most expensive parts of mobility simulations, so this saves a lot of time.
	 *
	 * @param when The earliest timestep at which the caller will be interested in snapshots again.
	 */
	public final void skipUntil(final double when) {
		this.skipUntil = when;
	}

	public final void addSnapshotWriter(final SnapshotWriter writer) {
		this.snapshotWriters.add(writer);
	}

	public final boolean removeSnapshotWriter(final SnapshotWriter writer) {
		return this.snapshotWriters.remove(writer);
	}

	public final void finish() {
		for (SnapshotWriter writer : this.snapshotWriters) {
			writer.finish();
		}
	}


}
