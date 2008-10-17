/* *********************************************************************** *
 * project: org.matsim.*
 * CountsConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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
 */
public class CountsConfigGroup extends Module {

	public static final String GROUP_NAME = "counts";

	private static final String LINKATTS = "linkattributes";
	private static final String OUTPUTFORMAT = "outputformat";
	private static final String OUTFILE = "outputCountsFile";
	private static final String TIMEFILTER = "timeFilter";
	private static final String DISTANCEFILTER = "distanceFilter";
	private static final String DISTANCEFITLERCENTERNODE = "distanceFilterCenterNode";
	private static final String VISIBLETIMESTEP = "visibleTimeStep";
	private static final String ITERATIONNUMBER = "iterationNumber";
	private static final String COUNTSINPUTFILENAME = "inputCountsFile";
	private static final String LOCALINPUTXSD = "localInputXSD";
	private static final String OUTPUTCOUNTSXSD = "outputCountsXSD";
	private static final String OUTPUTVERSION = "outputVersion";
	private static final String COUNTSSCALEFACTOR = "countsScaleFactor";

	private String outputFile;

	private String outputFormat;

	/**
	 * the distance filter in m
	 */
	private Double distanceFilter;

	/**
	 * the id of the node used as center for the distance filter
	 */
	private String distanceFilterCenterNode;

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
		if (OUTPUTFORMAT.equals(key)) {
			return getOutputFormat();
		} else if (OUTFILE.equals(key)) {
			return getOutputFile();
		} else if (DISTANCEFILTER.equals(key)) {
			if (getDistanceFilter() == null) {
				return null;
			}
			return getDistanceFilter().toString();
		} else if (DISTANCEFITLERCENTERNODE.equals(key)) {
			return getDistanceFilterCenterNode();
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
		if (OUTPUTFORMAT.equals(key)) {
			setOutputFormat(value);
		} else if (OUTFILE.equals(key)) {
			setOutputFile(value.replace('\\', '/'));
		} else if (DISTANCEFILTER.equals(key)) {
			if (value == null) {
				setDistanceFilter(null);
			} else {
				setDistanceFilter(Double.valueOf(value));
			}
		} else if (DISTANCEFITLERCENTERNODE.equals(key)) {
			setDistanceFilterCenterNode(value);
		} else if (COUNTSINPUTFILENAME.equals(key)) {
			setCountsFileName(value.replace('\\', '/'));
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			this.setCountsScaleFactor(Double.parseDouble(value));
		} else if (TIMEFILTER.equals(key) || LINKATTS.equals(key) || VISIBLETIMESTEP.equals(key) || ITERATIONNUMBER.equals(key) || LOCALINPUTXSD.equals(key) || OUTPUTCOUNTSXSD.equals(key) || OUTPUTVERSION.equals(key)) {
			log.warn("The parameter " + key + " in module " + GROUP_NAME + " is no longer needed and should be removed from the configuration file.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, OUTPUTFORMAT);
		this.addParameterToMap(map, OUTFILE);
		this.addParameterToMap(map, DISTANCEFILTER);
		this.addParameterToMap(map, DISTANCEFITLERCENTERNODE);
		this.addParameterToMap(map, COUNTSINPUTFILENAME);
		this.addParameterToMap(map, COUNTSSCALEFACTOR);
		return map;
	}

	public String getOutputFile() {
		return this.outputFile;
	}

	public void setOutputFile(final String outputFile) {
		this.outputFile = outputFile;
	}

	public String getOutputFormat() {
		return this.outputFormat;
	}

	public void setOutputFormat(final String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Double getDistanceFilter() {
		return this.distanceFilter;
	}

	public void setDistanceFilter(final Double distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	public String getDistanceFilterCenterNode() {
		return this.distanceFilterCenterNode;
	}

	public void setDistanceFilterCenterNode(final String distanceFilterCenterNode) {
		this.distanceFilterCenterNode = distanceFilterCenterNode;
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

	public double getCountsScaleFactor() {
		return this.countsScaleFactor;
	}

	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
}
