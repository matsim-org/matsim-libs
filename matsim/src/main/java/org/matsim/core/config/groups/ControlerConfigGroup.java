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

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import java.util.*;

public class ControlerConfigGroup extends ConfigGroup {

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
	public String getValue(final String key) {
        switch (key) {
            case OUTPUT_DIRECTORY:
                return getOutputDirectory();
            case FIRST_ITERATION:
                return Integer.toString(getFirstIteration());
            case LAST_ITERATION:
                return Integer.toString(getLastIteration());
            case ROUTINGALGORITHM_TYPE:
                return this.getRoutingAlgorithmType().toString();
            case RUNID:
                return this.getRunId();
            case LINKTOLINK_ROUTING_ENABLED:
                return Boolean.toString(this.linkToLinkRoutingEnabled);
            case EVENTS_FILE_FORMAT: {
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
            case WRITE_EVENTS_INTERVAL:
                throw new RuntimeException("use direct getter.  Aborting ...");
//			return Integer.toString(getWriteEventsInterval());
            case MOBSIM:
                return getMobsim();
            case SNAPSHOT_FORMAT: {
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
            case WRITE_SNAPSHOTS_INTERVAL:
                return Integer.toString(this.getWriteSnapshotsInterval());
            default:
                throw new IllegalArgumentException(key);
        }
	}

	@Override
	public void addParam(final String key, final String value) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			setOutputDirectory(value);
		} else if (FIRST_ITERATION.equals(key)) {
			setFirstIteration(Integer.parseInt(value));
		} else if (LAST_ITERATION.equals(key)) {
			setLastIteration(Integer.parseInt(value));
		} else if (ROUTINGALGORITHM_TYPE.equals(key)){
			if (RoutingAlgorithmType.Dijkstra.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
			}
			else if (RoutingAlgorithmType.AStarLandmarks.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
			}
			else if (RoutingAlgorithmType.FastDijkstra.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.FastDijkstra);
			}
			else if (RoutingAlgorithmType.FastAStarLandmarks.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
			}
			else {
				throw new IllegalArgumentException(value + " is not a valid parameter value for key: "+ key + " of config group " + GROUP_NAME);
			}
		} else if (RUNID.equals(key)){
			this.setRunId(value.trim());
		} else if (LINKTOLINK_ROUTING_ENABLED.equalsIgnoreCase(key)){
			if (value != null) {
				this.linkToLinkRoutingEnabled = Boolean.parseBoolean(value.trim());
			}
		} else if (EVENTS_FILE_FORMAT.equals(key)) {
			String[] parts = StringUtils.explode(value, ',');
			Set<EventsFileFormat> formats = EnumSet.noneOf(EventsFileFormat.class);
			for (String part : parts) {
				String trimmed = part.trim();
				if (trimmed.length() > 0) {
					formats.add(EventsFileFormat.valueOf(trimmed));
				}
			}
			this.eventsFileFormats = formats;
		} else if (WRITE_EVENTS_INTERVAL.equals(key)) {
			setWriteEventsInterval(Integer.parseInt(value));
		} else if (WRITE_PLANS_INTERVAL.equals(key)) {
			setWritePlansInterval(Integer.parseInt(value));
		} else if (MOBSIM.equals(key)) {
			setMobsim(value);
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			String[] parts = StringUtils.explode(value, ',');
			Set<String> formats = new HashSet<>();
			for (String part : parts) {
				String trimmed = part.trim();
				if (trimmed.length() > 0) {
					formats.add(trimmed);
				}
			}
			this.snapshotFormat = formats;
		} else if (WRITE_SNAPSHOTS_INTERVAL.equals(key)) {
			setWriteSnapshotsInterval(Integer.parseInt(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<>();
		map.put(OUTPUT_DIRECTORY, getValue(OUTPUT_DIRECTORY));
		map.put(FIRST_ITERATION, getValue(FIRST_ITERATION));
		map.put(LAST_ITERATION, getValue(LAST_ITERATION));
		map.put(ROUTINGALGORITHM_TYPE, getValue(ROUTINGALGORITHM_TYPE));
		map.put(RUNID, getValue(RUNID));
		map.put(LINKTOLINK_ROUTING_ENABLED, Boolean.toString(this.isLinkToLinkRoutingEnabled()));
		map.put(EVENTS_FILE_FORMAT, getValue(EVENTS_FILE_FORMAT));
		map.put(WRITE_EVENTS_INTERVAL, Integer.toString(this.getWriteEventsInterval()) );
		map.put(WRITE_PLANS_INTERVAL, Integer.toString(this.getWritePlansInterval()) );
		map.put(MOBSIM, getValue(MOBSIM));
		map.put(SNAPSHOT_FORMAT, getValue(SNAPSHOT_FORMAT));
		map.put(WRITE_SNAPSHOTS_INTERVAL, String.valueOf(getWriteSnapshotsInterval()));
		return map;
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

	/* direct access */

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setFirstIteration(final int firstIteration) {
		this.firstIteration = firstIteration;
	}

	public int getFirstIteration() {
		return this.firstIteration;
	}

	public void setLastIteration(final int lastIteration) {
		this.lastIteration = lastIteration;
	}

	public int getLastIteration() {
		return this.lastIteration;
	}

	public RoutingAlgorithmType getRoutingAlgorithmType() {
		return this.routingAlgorithmType;
	}

	public void setRoutingAlgorithmType(final RoutingAlgorithmType type) {
		this.routingAlgorithmType = type;
	}

	public String getRunId() {
		return this.runId;
	}

	public void setRunId(final String runid) {
		this.runId = runid;
	}

	public boolean isLinkToLinkRoutingEnabled() {
		return this.linkToLinkRoutingEnabled;
	}

	public void setLinkToLinkRoutingEnabled(final boolean enabled) {
		this.linkToLinkRoutingEnabled = enabled;
	}

	public Set<EventsFileFormat> getEventsFileFormats() {
		return this.eventsFileFormats;
	}

	public void setEventsFileFormats(final Set<EventsFileFormat> eventsFileFormats) {
		this.eventsFileFormats = Collections.unmodifiableSet(EnumSet.copyOf(eventsFileFormats));
	}

	public void setSnapshotFormat(final Collection<String> snapshotFormat) {
		this.snapshotFormat = Collections.unmodifiableSet(new HashSet<>(snapshotFormat));
	}

	public Collection<String> getSnapshotFormat() {
		return this.snapshotFormat;
	}

	public int getWriteEventsInterval() {
		return this.writeEventsInterval;
	}

	public void setWriteEventsInterval(final int writeEventsInterval) {
		this.writeEventsInterval = writeEventsInterval;
	}

	public String getMobsim() {
		return this.mobsim;
	}

	public void setMobsim(final String mobsim) {
		this.mobsim = mobsim;
	}

	public int getWritePlansInterval() {
		return this.writePlansInterval;
	}

	public void setWritePlansInterval(final int writePlansInterval) {
		this.writePlansInterval = writePlansInterval;
	}
	
	public int getWriteSnapshotsInterval() {
		return writeSnapshotsInterval;
	}
	
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
