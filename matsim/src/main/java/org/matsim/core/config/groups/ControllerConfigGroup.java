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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import java.util.*;


public final class ControllerConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = LogManager.getLogger( ControllerConfigGroup.class );

	public enum RoutingAlgorithmType {Dijkstra, AStarLandmarks, SpeedyALT}

	public enum EventTypeToCreateScoringFunctions {IterationStarts, BeforeMobsim}

	public enum EventsFileFormat {xml, pb, json}

	public enum CompressionType {
		none(""),
		gzip(".gz"),
		lz4(".lz4"),
		zst(".zst");

		public final String fileEnding;

		CompressionType(String fileEnding) {
			this.fileEnding = fileEnding;
		}
	}

	public enum CleanIterations {
		keep,
		delete,
	}

	public static final String GROUP_NAME = "controller";

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
	private static final String WRITE_TRIPS_INTERVAL = "writeTripsInterval";
	private static final String OVERWRITE_FILE = "overwriteFiles";
	private static final String CREATE_GRAPHS = "createGraphs";
	private static final String CREATE_GRAPHS_INTERVAL = "createGraphsInterval";
	private static final String DUMP_DATA_AT_END = "dumpDataAtEnd";
	private static final String CLEAN_ITERS_AT_END = "cleanItersAtEnd";
	private static final String COMPRESSION_TYPE = "compressionType";
	private static final String EVENT_TYPE_TO_CREATE_SCORING_FUNCTIONS = "createScoringFunctionType";

	private static final String MEMORY_OBSERVER_INTERVAL = "memoryObserverInterval";

	/*package*/ static final String MOBSIM = "mobsim";
	public enum MobsimType {qsim, hermes}

	private static final String WRITE_SNAPSHOTS_INTERVAL = "writeSnapshotsInterval";

	private String outputDirectory = "./output";
	private int firstIteration = 0;
	private int lastIteration = 1000;
	private RoutingAlgorithmType routingAlgorithmType = RoutingAlgorithmType.SpeedyALT;
	private EventTypeToCreateScoringFunctions eventTypeToCreateScoringFunctions = EventTypeToCreateScoringFunctions.IterationStarts;

	private boolean linkToLinkRoutingEnabled = false;

	private String runId = null;

	private Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));

	private int writeEventsInterval= 50;
	private int writePlansInterval= 50;
	private int writeTripsInterval = 50;
	private String mobsim = MobsimType.qsim.toString();
	private int writeSnapshotsInterval = 1;
	private int createGraphsInterval = 1;
	private boolean dumpDataAtEnd = true;

	private CompressionType compressionType = CompressionType.gzip;
	private OverwriteFileSetting overwriteFileSetting = OverwriteFileSetting.failIfDirectoryExists;

	private CleanIterations cleanItersAtEnd = CleanIterations.keep;

	private int memoryObserverInterval = 60;

	public ControllerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ROUTINGALGORITHM_TYPE, "The type of routing (least cost path) algorithm used, may have the values: " + Arrays.toString(RoutingAlgorithmType.values()));
		map.put(RUNID, "An identifier for the current run which is used as prefix for output files and mentioned in output xml files etc.");
		map.put(EVENTS_FILE_FORMAT, "Default="+EventsFileFormat.xml+"; Specifies the file format for writing events. Currently supported: " + Arrays.toString(EventsFileFormat.values()) + IOUtils.NATIVE_NEWLINE+ "\t\t" +
				"Multiple values can be specified separated by commas (',').");
		map.put(WRITE_EVENTS_INTERVAL, "iterationNumber % writeEventsInterval == 0 defines in which iterations events are written " +
				"to a file. `0' disables events writing completely.");
		map.put(WRITE_TRIPS_INTERVAL, "iterationNumber % writeEventsInterval == 0 defines in which iterations trips CSV are written " +
                "to a file. `0' disables trips writing completely.");
		map.put(WRITE_PLANS_INTERVAL, "iterationNumber % writePlansInterval == 0 defines (hopefully) in which iterations plans are " +
                "written to a file. `0' disables plans writing completely.  Some plans in early iterations are always written");
		map.put(LINKTOLINK_ROUTING_ENABLED, "Default=false. If enabled, the router takes travel times needed for turning moves into account."
		        + " Can only be used with Dijkstra routing. Cannot be used when TravelTimeCalculator.separateModes is enabled.");
		map.put(FIRST_ITERATION, "Default=0. First Iteration of a simulation.");
		map.put(LAST_ITERATION, "Default=1000. Last Iteration of a simulation.");

		map.put(CREATE_GRAPHS, "Sets whether graphs showing some analyses should automatically be generated during the simulation." +
				" The generation of graphs usually takes a small amount of time that does not have any weight in big simulations," +
				" but add a significant overhead in smaller runs or in test cases where the graphical output is not even requested." );

		map.put(CREATE_GRAPHS_INTERVAL, "Sets the interval in which graphs are generated. Default is 1. If set to 0, no graphs are generated." +
			" The generation of graphs usually takes a small amount of time that does not have any weight in big simulations," +
			" but add a significant overhead in smaller runs or in test cases where the graphical output is not even requested." );

		map.put(COMPRESSION_TYPE, "Compression algorithm to use when writing out data to files. Possible values: " + Arrays.toString(CompressionType.values()));
		map.put(EVENT_TYPE_TO_CREATE_SCORING_FUNCTIONS, "Defines when the scoring functions for the population are created. Default=IterationStarts. Possible values: " + Arrays.toString(EventTypeToCreateScoringFunctions.values()));

		map.put(MOBSIM, "Defines which mobility simulation will be used. Currently supported: " + Arrays.toString(MobsimType.values()) + IOUtils.NATIVE_NEWLINE + "\t\t" +
				"Depending on the chosen mobsim, you'll have to add additional config modules to configure the corresponding mobsim." + IOUtils.NATIVE_NEWLINE + "\t\t" +
				"For 'qsim', add a module 'qsim' to the config.");

		map.put(SNAPSHOT_FORMAT, "Comma-separated list of visualizer output file formats. `transims' and `otfvis'.");
		map.put(WRITE_SNAPSHOTS_INTERVAL, "iterationNumber % " + WRITE_SNAPSHOTS_INTERVAL + " == 0 defines in which iterations snapshots are written " +
				"to a file. `0' disables snapshots writing completely");
		map.put(DUMP_DATA_AT_END, "true if at the end of a run, plans, network, config etc should be dumped to a file");
		map.put(CLEAN_ITERS_AT_END, "Defines what should be done with the ITERS directory when a simulation finished successfully");
		map.put(MEMORY_OBSERVER_INTERVAL, "Defines the interval for printing memory usage to the log in [seconds]. Must be positive. Defaults to 60.");
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

	@StringGetter( COMPRESSION_TYPE )
	public CompressionType getCompressionType() {
		return this.compressionType;
	}

	@StringSetter( COMPRESSION_TYPE )
	public void setCompressionType(CompressionType type) {
		this.compressionType = type;
	}

	@StringGetter( RUNID )
	public String getRunId() {
		return this.runId;
	}

    @StringGetter(WRITE_TRIPS_INTERVAL)
    public int getWriteTripsInterval() {
        return writeTripsInterval;
    }

    @StringSetter(WRITE_TRIPS_INTERVAL)
    public void setWriteTripsInterval(int writeTripsInterval) {
        this.writeTripsInterval = writeTripsInterval;
    }

	@Parameter
	@Comment("Defines in which iterations the legHistogram analysis is run (iterationNumber % interval == 0). Use 0 to disable this analysis.")
	private int legHistogramInterval = 1;

	@Parameter
	@Comment("Defines in which iterations the legDurations analysis is run (iterationNumber % interval == 0). Use 0 to disable this analysis.")
	private int legDurationsInterval = 1;

	public int getLegHistogramInterval() {
		return this.legHistogramInterval;
	}

	public void setLegHistogramInterval(int legHistogramInterval) {
		this.legHistogramInterval = legHistogramInterval;
	}

	public int getLegDurationsInterval() {
		return this.legDurationsInterval;
	}

	public void setLegDurationsInterval(int legDurationsInterval) {
		this.legDurationsInterval = legDurationsInterval;
	}


	@StringSetter( RUNID )
	public void setRunId(final String runid) {
		if (runid == null) {
			this.runId = null;
		} else {
			if (runid.equals("")) {
				log.info("No run Id provided. Setting run Id to null.");
				this.runId = null;
			} else {
				this.runId = runid;
			}
		}
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
	// ---
	public enum SnapshotFormat { transims, googleearth, otfvis, positionevents }
	private Set<SnapshotFormat> snapshotFormat = Collections.emptySet();

	@StringSetter( SNAPSHOT_FORMAT )
	private void setSnapshotFormats( final String value ) {
		String[] parts = StringUtils.explode(value, ',');
		Set<SnapshotFormat> formats = EnumSet.noneOf( SnapshotFormat.class );
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.length() > 0) {
				formats.add(SnapshotFormat.valueOf( trimmed ) );
			}
		}
		this.snapshotFormat = formats;
	}

	@StringGetter( SNAPSHOT_FORMAT )
	private String getSnapshotFormatAsString() {
		boolean isFirst = true;
		StringBuilder str = new StringBuilder();
		for (SnapshotFormat format : this.snapshotFormat) {
			if (!isFirst) {
				str.append(',');
			}
			str.append(format.name());
			isFirst = false;
		}
		return str.toString();
	}

	public void setSnapshotFormat(final Collection<SnapshotFormat> snapshotFormat) {
		this.snapshotFormat = Collections.unmodifiableSet(EnumSet.copyOf( snapshotFormat) );
	}

	public Collection<SnapshotFormat> getSnapshotFormat() {
		return this.snapshotFormat;
	}
	// ---
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

	@StringGetter( CREATE_GRAPHS_INTERVAL )
	public int getCreateGraphsInterval() {
		return createGraphsInterval;
	}

	/**
	 * Sets whether graphs showing some analyses should automatically be
	 * generated during the simulation. The generation of graphs usually takes a
	 * small amount of time that does not have any weight in big simulations,
	 * but add a significant overhead in smaller runs or in test cases where the
	 * graphical output is not even requested.
	 *
	 * @param createGraphsInterval iteration interval in which graphs are generated
	 */
	@StringSetter( CREATE_GRAPHS_INTERVAL )
	public void setCreateGraphsInterval(int createGraphsInterval) {
		this.createGraphsInterval = createGraphsInterval;
	}

	@StringSetter( CREATE_GRAPHS )
	@Deprecated
	public void setCreateGraphs(boolean createGraphs) {
		log.warn("Parameter 'createGraphs' is deprecated. Using 'createGraphsInterval' instead. The output_config.xml will contain the new parameter.");
		if (createGraphs) {
			this.setCreateGraphsInterval(1);
		} else {
			this.setCreateGraphsInterval(0);
		}
	}

	@StringGetter( OVERWRITE_FILE )
	public OverwriteFileSetting getOverwriteFileSetting() {
		return overwriteFileSetting;
	}

	@StringSetter( OVERWRITE_FILE )
	public void setOverwriteFileSetting(final OverwriteFileSetting overwriteFileSetting) {
		this.overwriteFileSetting = overwriteFileSetting;
	}

	@StringGetter(DUMP_DATA_AT_END)
	public boolean getDumpDataAtEnd() {
		return dumpDataAtEnd;
	}

	@StringSetter(DUMP_DATA_AT_END)
	public void setDumpDataAtEnd(boolean dumpDataAtEnd) {
		this.dumpDataAtEnd = dumpDataAtEnd;
	}

	@StringSetter(CLEAN_ITERS_AT_END)
	public void setCleanItersAtEnd(CleanIterations cleanItersAtEnd) {
		this.cleanItersAtEnd = cleanItersAtEnd;
	}

	@StringGetter(CLEAN_ITERS_AT_END)
	public CleanIterations getCleanItersAtEnd() {
		return cleanItersAtEnd;
	}

	@StringGetter(EVENT_TYPE_TO_CREATE_SCORING_FUNCTIONS)
	public EventTypeToCreateScoringFunctions getEventTypeToCreateScoringFunctions() {
		return eventTypeToCreateScoringFunctions;
	}

	@StringSetter(EVENT_TYPE_TO_CREATE_SCORING_FUNCTIONS)
	public void setEventTypeToCreateScoringFunctions(EventTypeToCreateScoringFunctions eventTypeToCreateScoringFunctions) {
		this.eventTypeToCreateScoringFunctions = eventTypeToCreateScoringFunctions;
	}

	@StringGetter(MEMORY_OBSERVER_INTERVAL)
	public int getMemoryObserverInterval() {
		return memoryObserverInterval;
	}

	@StringSetter(MEMORY_OBSERVER_INTERVAL)
	public void setMemoryObserverInterval(int memoryObserverInterval) {
		this.memoryObserverInterval = memoryObserverInterval;
	}

	// ---
	int writePlansUntilIteration = 1 ;
	public int getWritePlansUntilIteration() {
		return this.writePlansUntilIteration ;
	}
	public void setWritePlansUntilIteration(int val) {
		this.writePlansUntilIteration = val ;
	}
	// ---
	int writeEventsUntilIteration = 0 ; // old default of this was 0, not 1 as for plans. kai, aug'16
	public int getWriteEventsUntilIteration() {
		return this.writeEventsUntilIteration ;
	}
	public void setWriteEventsUntilIteration(int val) {
		this.writeEventsUntilIteration = val ;
	}
	@Override
	protected void checkConsistency(Config config) {
		if ( config.controller().getOverwriteFileSetting() == OverwriteFileSetting.overwriteExistingFiles ) {
			log.warn( "setting overwriting behavior to "+overwriteFileSetting );
			log.warn( "this is not recommended, as it might result in a directory containing output from several model runs" );
			log.warn( "prefer the options "+OverwriteFileSetting.deleteDirectoryIfExists+" or "+OverwriteFileSetting.failIfDirectoryExists );
		}
		if(config.controller().getMemoryObserverInterval() < 0) {
			log.warn("Memory observer interval is negative. Simulation will most likely crash.");
		}
	}
}
