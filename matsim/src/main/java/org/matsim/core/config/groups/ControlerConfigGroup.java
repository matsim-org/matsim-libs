/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.config.experimental.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public final class ControlerConfigGroup extends ReflectiveConfigGroup {

	public enum RoutingAlgorithmType {Dijkstra, AStarLandmarks, FastDijkstra, FastAStarLandmarks}

	public enum EventsFileFormat {txt, xml}

	public static final String GROUP_NAME = "controler";

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private static final String FIRST_ITERATION = "firstIteration";
	private static final String LAST_ITERATION = "lastIteration";
	private static final String ROUTINGALGORITHM_TYPE = "routingAlgorithmType";
	private static final String RUNID = "runId";
	private static final String LINKTOLINK_ROUTING_ENABLED = "enableLinkToLinkRouting";
	/*package*/ static final String EVENTS_FILE_FORMAT = "eventsFileFormat";
	private static final String SNAPSHOT_FORMAT = "snapshotFormat";
	private static final String WRITE_EVENTS_INTERVAL = "writeEventsInterval";
	private static final String WRITE_PLANS_INTERVAL = "writePlansInterval";

	/*package*/ static final String MOBSIM = "mobsim";
	public enum MobsimType {qsim, JDEQSim}

    public static final String WRITE_SNAPSHOTS_INTERVAL = "writeSnapshotsInterval";


	private String outputDirectory = "./output";
	private int firstIteration = 0;
	private int lastIteration = 1000;
	private RoutingAlgorithmType routingAlgorithmType = RoutingAlgorithmType.Dijkstra;

	private boolean linkToLinkRoutingEnabled = false;

	private String runId = null;

	private Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));

	private int writeEventsInterval=10;
	private int writePlansInterval=10;
	private Set<String> snapshotFormat = Collections.emptySet();
	private String mobsim = MobsimType.qsim.toString();
	private int writeSnapshotsInterval = 1;
	private boolean createGraphs = true;

	public ControlerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ROUTINGALGORITHM_TYPE, "The type of routing (least cost path) algorithm used, may have the values: " + RoutingAlgorithmType.Dijkstra + ", " + 
				RoutingAlgorithmType.FastDijkstra + ", " + RoutingAlgorithmType.AStarLandmarks + " or "  + RoutingAlgorithmType.FastAStarLandmarks);
		map.put(RUNID, "An identifier for the current run which is used as prefix for output files and mentioned in output xml files etc.");
		map.put(EVENTS_FILE_FORMAT, "Default="+EventsFileFormat.xml+"; Specifies the file format for writing events. Currently supported: txt, xml."+IOUtils.NATIVE_NEWLINE+ "\t\t" +
				"Multiple values can be specified separated by commas (',').");
		map.put(WRITE_EVENTS_INTERVAL, "iterationNumber % writeEventsInterval == 0 defines in which iterations events are written " +
				"to a file. `0' disables events writing completely.");
		map.put(WRITE_PLANS_INTERVAL, "iterationNumber % writePlansInterval == 0 defines (hopefully) in which iterations plans are " +
				"written to a file. `0' disables plans writing completely.  Some plans in early iterations are always written");
		map.put(LINKTOLINK_ROUTING_ENABLED, "Default=false; "); // TODO: add description
		map.put(FIRST_ITERATION, "Default=0; "); // TODO: add description
		map.put(LAST_ITERATION, "Default=1000; "); // TODO: add description
		
		StringBuilder mobsimTypes = new StringBuilder();
		for ( MobsimType mtype : MobsimType.values() ) {
			mobsimTypes.append(mtype.toString());
			mobsimTypes.append(' ');
		}
		map.put(MOBSIM, "Defines which mobility simulation will be used. Currently supported: " + mobsimTypes + IOUtils.NATIVE_NEWLINE + "\t\t" +
				"Depending on the chosen mobsim, you'll have to add additional config modules to configure the corresponding mobsim." + IOUtils.NATIVE_NEWLINE + "\t\t" +
				"For 'qsim', add a module 'qsim' to the config.");
		
		map.put(SNAPSHOT_FORMAT, "Comma-separated list of visualizer output file formats. `transims', `googleearth', and `otfvis'.");
		map.put(WRITE_SNAPSHOTS_INTERVAL, "iterationNumber % " + WRITE_SNAPSHOTS_INTERVAL + " == 0 defines in which iterations snapshots are written " +
				"to a file. `0' disables snapshots writing completely");
		return map;
	}


	@StringSetter( OUTPUT_DIRECTORY )
	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@StringGetter( OUTPUT_DIRECTORY )
	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	@StringSetter( FIRST_ITERATION )
	public void setFirstIteration(final int firstIteration) {
		this.firstIteration = firstIteration;
	}

	@StringGetter( FIRST_ITERATION )
	public int getFirstIteration() {
		return this.firstIteration;
	}

	@StringSetter( LAST_ITERATION )
	public void setLastIteration(final int lastIteration) {
		this.lastIteration = lastIteration;
	}

	@StringGetter( LAST_ITERATION )
	public int getLastIteration() {
		return this.lastIteration;
	}

	@StringGetter( ROUTINGALGORITHM_TYPE )
	public RoutingAlgorithmType getRoutingAlgorithmType() {
		return this.routingAlgorithmType;
	}

	@StringSetter( ROUTINGALGORITHM_TYPE )
	public void setRoutingAlgorithmType(final RoutingAlgorithmType type) {
		this.routingAlgorithmType = type;
	}

	@StringGetter( RUNID )
	public String getRunId() {
		return this.runId;
	}

	@StringSetter( RUNID )
	public void setRunId(final String runid) {
		this.runId = runid;
	}

	@StringGetter( LINKTOLINK_ROUTING_ENABLED )
	public boolean isLinkToLinkRoutingEnabled() {
		return this.linkToLinkRoutingEnabled;
	}

	@StringSetter( LINKTOLINK_ROUTING_ENABLED )
	public void setLinkToLinkRoutingEnabled(final boolean enabled) {
		this.linkToLinkRoutingEnabled = enabled;
	}

    @StringGetter( EVENTS_FILE_FORMAT )
	private String getEventsFileFormatAsString() {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (EventsFileFormat format : this.eventsFileFormats) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(format.toString());
			isFirst = false;
		}
		return str.toString();
	}

	@StringSetter( EVENTS_FILE_FORMAT )
	private void setEventFileFormats( final String value ) {
		String[] parts = StringUtils.explode(value, ',');
		Set<EventsFileFormat> formats = EnumSet.noneOf(EventsFileFormat.class);
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				formats.add(EventsFileFormat.valueOf(trimmed));
			}
		}
		this.eventsFileFormats = formats;
	}

	public Set<EventsFileFormat> getEventsFileFormats() {
		return this.eventsFileFormats;
	}

	public void setEventsFileFormats(final Set<EventsFileFormat> eventsFileFormats) {
		this.eventsFileFormats = Collections.unmodifiableSet(EnumSet.copyOf(eventsFileFormats));
	}

	@StringSetter( SNAPSHOT_FORMAT )
	private void setSnapshotFormats( final String value ) {
		String[] parts = StringUtils.explode(value, ',');
		Set<String> formats = new HashSet<>();
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				formats.add(trimmed);
			}
		}
		this.snapshotFormat = formats;
	}

    @StringGetter( SNAPSHOT_FORMAT )
	private String getSnapshotFormatAsString() {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (String format : this.snapshotFormat) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(format);
			isFirst = false;
		}
		return str.toString();
	}

	public void setSnapshotFormat(final Collection<String> snapshotFormat) {
		this.snapshotFormat = Collections.unmodifiableSet(new HashSet<>(snapshotFormat));
	}

	public Collection<String> getSnapshotFormat() {
		return this.snapshotFormat;
	}

	@StringGetter( WRITE_EVENTS_INTERVAL )
	public int getWriteEventsInterval() {
		return this.writeEventsInterval;
	}

	@StringSetter( WRITE_EVENTS_INTERVAL )
	public void setWriteEventsInterval(final int writeEventsInterval) {
		this.writeEventsInterval = writeEventsInterval;
	}

	@StringGetter( MOBSIM )
	public String getMobsim() {
		return this.mobsim;
	}

	@StringSetter( MOBSIM )
	public void setMobsim(final String mobsim) {
		this.mobsim = mobsim;
	}

	@StringGetter( WRITE_PLANS_INTERVAL )
	public int getWritePlansInterval() {
		return this.writePlansInterval;
	}

	@StringSetter( WRITE_PLANS_INTERVAL )
	public void setWritePlansInterval(final int writePlansInterval) {
		this.writePlansInterval = writePlansInterval;
	}
	
	@StringGetter( WRITE_SNAPSHOTS_INTERVAL )
	public int getWriteSnapshotsInterval() {
		return writeSnapshotsInterval;
	}
	
	@StringSetter( WRITE_SNAPSHOTS_INTERVAL )
	public void setWriteSnapshotsInterval(int writeSnapshotsInterval) {
		this.writeSnapshotsInterval = writeSnapshotsInterval;
	}

	public boolean isCreateGraphs() {
		return createGraphs;
	}

    /**
     * Sets whether graphs showing some analyses should automatically be
     * generated during the simulation. The generation of graphs usually takes a
     * small amount of time that does not have any weight in big simulations,
     * but add a significant overhead in smaller runs or in test cases where the
     * graphical output is not even requested.
     *
     * @param createGraphs
     *            true if graphs showing analyses' output should be generated.
     */
	public void setCreateGraphs(boolean createGraphs) {
		this.createGraphs = createGraphs;
	}

}
