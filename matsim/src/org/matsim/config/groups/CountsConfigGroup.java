/* *********************************************************************** *
 * project: org.matsim.*
 * CountsConfigGroup.java
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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.config.Module;

/**
 * @author dgrether
 *
 */
public class CountsConfigGroup extends Module {

	public static final String GROUP_NAME = "counts";

	/**
	 * name of the linkattribute parameter in config
	 */
	private static final String LINKATTS = "linkattributes";

	/**
	 * name of the output format parameter in config
	 */
	private static final String OUTPUTFORMAT = "outputformat";

	/**
	 * name of the output file parameter in config
	 */
	private static final String OUTFILE = "outputCountsFile";

	/**
	 * name of the timefilter parameter in config
	 */
	private static final String TIMEFILTER = "timeFilter";

	/**
	 * name of the distancefilter parameter in config
	 */
	private static final String DISTANCEFILTER = "distanceFilter";

	/**
	 * name of the distancefilterCenterNode parameter in config
	 */
	private static final String DISTANCEFITLERCENTERNODE = "distanceFilterCenterNode";
	/**
	 * parameter name
	 */
	private static final String VISIBLETIMESTEP = "visibleTimeStep";

	/**
	 * parameter name
	 */
	private static final String ITERATIONNUMBER = "iterationNumber";
	/**
	 * parameter name
	 */
	private static final String COUNTSINPUTFILENAME = "inputCountsFile";
	/**
	 * parameter name
	 */
	private static final String LOCALINPUTXSD = "localInputXSD";
	/**
	 * parameter name
	 */
	private static final String OUTPUTCOUNTSXSD = "outputCountsXSD";
	/**
	 * parameter name
	 */
	private static final String OUTPUTVERSION = "outputVersion";
	/**
	 * parameter name
	 */
	private static final String COUNTSSCALEFACTOR = "countsScaleFactor";

	/**
	 * the name(path) to the output file
	 */
	private String outputFile;

	/**
	 * the link attributes file to read
	 */
	private String linkAttsFile;

	/**
	 * the output format
	 */
	private String outputFormat;

	/**
	 * the time filter in h A value in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m.
	 * to 2 a.m.
	 */
	private Integer timeFilter;

	/**
	 * the distance filter in m
	 */
	private Double distanceFilter;

	/**
	 * the id of the node used as center for the distance filter
	 */
	private String distanceFilterCenterNode;

	/**
	 * the timestep for which counts are initially drawn in the 3d viewer A value
	 * in 1..24, 1 for 0 a.m. to 1 a.m., 2 for 1 a.m. to 2 a.m.
	 */
	private Integer visibleTimeStep;

	/**
	 * the number of the iteration which can be set in the config file
	 */
	private Integer iterationNumber;
	/**
	 * the path to the file with the counts
	 */
	private String countsFileName = null;
	/**
	 * the scaling for the counts
	 */
	private double countsScaleFactor = 1.0;

	private static final Logger log = Logger.getLogger(CountsConfigGroup.class);

	public CountsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (LINKATTS.equals(key)) {
			return getLinkAttsFile();
		} else if (OUTPUTFORMAT.equals(key)) {
			return getOutputFormat();
		} else if (OUTFILE.equals(key)) {
			return getOutputFile();
		} else if (TIMEFILTER.equals(key)) {
			if (getTimeFilter() == null) {
				return null;
			}
			return getTimeFilter().toString();
		} else if (DISTANCEFILTER.equals(key)) {
			if (getDistanceFilter() == null) {
				return null;
			}
			return getDistanceFilter().toString();
		} else if (DISTANCEFITLERCENTERNODE.equals(key)) {
			return getDistanceFilterCenterNode();
		} else if (VISIBLETIMESTEP.equals(key)) {
			if (getVisibleTimeStep() == null) {
				return null;
			}
			return getVisibleTimeStep().toString();
		} else if (ITERATIONNUMBER.equals(key)) {
			if (getIterationNumber() == null) {
				return null;
			}
			return getIterationNumber().toString();
		} else if (COUNTSINPUTFILENAME.equals(key)) {
			return getCountsFileName();
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			return Double.toString(getCountsScaleFactor());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (LINKATTS.equals(key)) {
			setLinkAttsFile(value.replace('\\', '/'));
		} else if (OUTPUTFORMAT.equals(key)) {
			setOutputFormat(value);
		} else if (OUTFILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (TIMEFILTER.equals(key)) {
			if (value == null) {
				setTimeFilter(null);
			} else {
				setTimeFilter(Integer.valueOf(value));
			}
		} else if (DISTANCEFILTER.equals(key)) {
			if (value == null) {
				setDistanceFilter(null);
			} else {
				setDistanceFilter(Double.valueOf(value));
			}
		} else if (DISTANCEFITLERCENTERNODE.equals(key)) {
			setDistanceFilterCenterNode(value);
		} else if (VISIBLETIMESTEP.equals(key)) {
			if (value == null) {
				setVisibleTimeStep(null);
			} else {
				setVisibleTimeStep(Integer.valueOf(value));
			}
		} else if (ITERATIONNUMBER.equals(key)) {
			if (value == null) {
				setIterationNumber(null);
			} else {
				setIterationNumber(Integer.valueOf(value));
			}
		} else if (COUNTSINPUTFILENAME.equals(key)) {
			setCountsFileName(value.replace('\\', '/'));
		} else if (LOCALINPUTXSD.equals(key) || OUTPUTCOUNTSXSD.equals(key) || OUTPUTVERSION.equals(key)) {
			log.info("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			this.setCountsScaleFactor(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addNotNullParameterToMap(map, LINKATTS);
		this.addNotNullParameterToMap(map, OUTPUTFORMAT);
		this.addNotNullParameterToMap(map, OUTFILE);
		this.addNotNullParameterToMap(map, TIMEFILTER);
		this.addNotNullParameterToMap(map, DISTANCEFILTER);
		this.addNotNullParameterToMap(map, DISTANCEFITLERCENTERNODE);
		this.addNotNullParameterToMap(map, VISIBLETIMESTEP);
		this.addNotNullParameterToMap(map, ITERATIONNUMBER);
		this.addNotNullParameterToMap(map, COUNTSINPUTFILENAME);
		this.addNotNullParameterToMap(map, COUNTSSCALEFACTOR);
		return map;
	}
	/**
	 * @return the filename of the resulting file
	 */
	public String getOutputFile() {
		return this.outputFile;
	}

	/**
	 * @param outputFile
	 *          the filename of the resulting file
	 */
	public void setOutputFile(final String outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * @return the outputFormat
	 */
	public String getOutputFormat() {
		return this.outputFormat;
	}

	/**
	 * @param outputFormat
	 *          the outputFormat to set
	 */
	public void setOutputFormat(final String outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * @return the timeFilter
	 */
	public Integer getTimeFilter() {
		return this.timeFilter;
	}

	/**
	 * @param timeFilter
	 *          the timeFilter to set
	 */
	public void setTimeFilter(final Integer timeFilter) {
		this.timeFilter = timeFilter;
	}

	/**
	 * @return the distanceFilter
	 */
	public Double getDistanceFilter() {
		return this.distanceFilter;
	}

	/**
	 * @param distanceFilter
	 *          the distanceFilter to set
	 */
	public void setDistanceFilter(final Double distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	/**
	 * @return the distanceFilterCenterNode
	 */
	public String getDistanceFilterCenterNode() {
		return this.distanceFilterCenterNode;
	}

	/**
	 * @param distanceFilterCenterNode
	 *          the distanceFilterCenterNode to set
	 */
	public void setDistanceFilterCenterNode(final String distanceFilterCenterNode) {
		this.distanceFilterCenterNode = distanceFilterCenterNode;
	}

	/**
	 * @return the hour (1-24) that will be shown by default when the file is opened (KML only)
	 */
	public Integer getVisibleTimeStep() {
		return this.visibleTimeStep;
	}

	/**
	 * @param visibleTimeStep
	 *          the hour (1-24) that should be shown by default when the file is opened (KML only)
	 */
	public void setVisibleTimeStep(final Integer visibleTimeStep) {
		this.visibleTimeStep = visibleTimeStep;
	}

	/**
	 * @return the iteration number that will be shown on the generated graphs
	 */
	public Integer getIterationNumber() {
		return this.iterationNumber;
	}

	/**
	 * @param iterationNumber
	 *          the iteration number that will be shown on the generated graphs
	 */
	public void setIterationNumber(final Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	/**
	 * @return the linkAttsFile
	 */
	public String getLinkAttsFile() {
		return this.linkAttsFile;
	}

	/**
	 * @param linkAttsFile
	 *          the linkAttsFile to set
	 */
	public void setLinkAttsFile(final String linkAttsFile) {
		this.linkAttsFile = linkAttsFile;
	}


	/**
	 * @return the filename of the counts file to be read in
	 */
	public String getCountsFileName() {
		return this.countsFileName;
	}


	/**
	 * @param countsFileName the filename of the counts file to be read in
	 */
	public void setCountsFileName(final String countsFileName) {
		this.countsFileName = countsFileName;
	}


	/**
	 * @return the countsScaleFactor
	 */
	public double getCountsScaleFactor() {
		return this.countsScaleFactor;
	}


	/**
	 * @param countsScaleFactor the countsScaleFactor to set
	 */
	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
}
