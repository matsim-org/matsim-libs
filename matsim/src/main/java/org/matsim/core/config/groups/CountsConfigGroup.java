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

package org.matsim.core.config.groups;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;

/**
 * @author dgrether
 * @author mrieser
 */
public class CountsConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "counts";

	private static final String OUTPUTFORMAT = "outputformat";
	private static final String DISTANCEFILTER = "distanceFilter";
	private static final String DISTANCEFILTERCENTERNODE = "distanceFilterCenterNode";
	private static final String COUNTSINPUTFILENAME = "inputCountsFile";
	private static final String COUNTSSCALEFACTOR = "countsScaleFactor";
	private static final String WRITECOUNTSINTERVAL = "writeCountsInterval";
	private static final String AVERAGECOUNTSOVERITERATIONS = "averageCountsOverIterations";
	private static final String ANALYZEDMODES = "analyzedModes";
	private static final String FILTERMODES = "filterModes";
	
	private String outputFormat = "txt";

	/**
	 * the distance filter in [length unit defined by coordinates] 
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
	
	private int writeCountsInterval = 10;
	private int averageCountsOverIterations = 5;

	private String analyzedModes = TransportMode.car;
	private boolean filterModes = false;
	
	public CountsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (OUTPUTFORMAT.equals(key)) {
			return getOutputFormat();
		} else if (DISTANCEFILTER.equals(key)) {
			if (getDistanceFilter() == null) {
				return null;
			}
			return getDistanceFilter().toString();
		} else if (DISTANCEFILTERCENTERNODE.equals(key)) {
			return getDistanceFilterCenterNode();
		} else if (COUNTSINPUTFILENAME.equals(key)) {
			return getCountsFileName();
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			return Double.toString(getCountsScaleFactor());
		} else if (WRITECOUNTSINTERVAL.equals(key)) {
			return Integer.toString(getWriteCountsInterval());
		} else if (AVERAGECOUNTSOVERITERATIONS.equals(key)) {
			return Integer.toString(getAverageCountsOverIterations());
		} else if (ANALYZEDMODES.equals(key)){
			return getAnalyzedModes();
		} else if (FILTERMODES.equals(key)){
			return Boolean.toString(isFilterModes());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (OUTPUTFORMAT.equals(key)) {
			setOutputFormat(value);
		} else if (DISTANCEFILTER.equals(key)) {
			if (value == null) {
				setDistanceFilter(null);
			} else {
				setDistanceFilter(Double.valueOf(value));
			}
		} else if (DISTANCEFILTERCENTERNODE.equals(key)) {
			setDistanceFilterCenterNode(value);
		} else if (COUNTSINPUTFILENAME.equals(key)) {
			setCountsFileName(value);
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			this.setCountsScaleFactor(Double.parseDouble(value));
		} else if (WRITECOUNTSINTERVAL.equals(key)) {
			this.setWriteCountsInterval(Integer.parseInt(value));
		} else if (AVERAGECOUNTSOVERITERATIONS.equals(key)) {
			this.setAverageCountsOverIterations(Integer.parseInt(value));
		} else if (ANALYZEDMODES.equals(key)){
			this.setAnalyzedModes(value);
		} else if (FILTERMODES.equals(key)){
			this.setFilterModes(Boolean.parseBoolean(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, OUTPUTFORMAT);
		this.addParameterToMap(map, DISTANCEFILTER);
		this.addParameterToMap(map, DISTANCEFILTERCENTERNODE);
		this.addParameterToMap(map, COUNTSINPUTFILENAME);
		this.addParameterToMap(map, COUNTSSCALEFACTOR);
		this.addParameterToMap(map, WRITECOUNTSINTERVAL);
		this.addParameterToMap(map, AVERAGECOUNTSOVERITERATIONS);
		map.put(ANALYZEDMODES, getValue(ANALYZEDMODES));
		map.put(FILTERMODES, getValue(FILTERMODES));
		return map;
	}
	
	// the following are public so they can be re-used in PtCountsComparisonConfigGroup.  Once that group is moved into the same
	// package, they can be made package-private.  kai, oct'10
	public static final String COUNTS_OUTPUTFORMAT_COMMENT = "possible values: `html', `kml', `txt', `all'"  ;
	public static final String COUNTS_DISTANCEFILTER_COMMENT = "distance to distanceFilterCenterNode to include counting stations. The unit of distance is "
		+ "the Euclidean distance implied by the coordinate system" ;
	public static final String COUNTS_DISTANCEFILTERCENTERNODE_COMMENT = "node id for center node of distance filter" ;
	public static final String COUNTSINPUTFILENAME_COMMENT = "input file name to counts package" ;
	public static final String COUNTSSCALEFACTOR_COMMENT = "factor by which to re-scale the simulated values.  necessary when "
		+ "simulation runs with something different from 100%.  needs to be adapted manually" ;
	
	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(OUTPUTFORMAT, COUNTS_OUTPUTFORMAT_COMMENT ) ;
		comments.put(DISTANCEFILTER,  COUNTS_DISTANCEFILTER_COMMENT ) ;
		comments.put(DISTANCEFILTERCENTERNODE, COUNTS_DISTANCEFILTERCENTERNODE_COMMENT ) ;
		comments.put(COUNTSINPUTFILENAME, COUNTSINPUTFILENAME_COMMENT ) ;
		comments.put(COUNTSSCALEFACTOR, COUNTSSCALEFACTOR_COMMENT ) ;
		comments.put(WRITECOUNTSINTERVAL, "Specifies how often the counts comparison should be calculated and written.");
		comments.put(AVERAGECOUNTSOVERITERATIONS, "Specifies over how many iterations the link volumes should be averaged that are used for the " +
				"counts comparison. Use 1 or 0 to only use the link volumes of a single iteration. This values cannot be larger than the value specified for " + WRITECOUNTSINTERVAL);
		comments.put(ANALYZEDMODES, "Transport modes that will be respected for the counts comparison. 'car' is default, which " +
				"includes also bussed from the pt simulation module. Use this parameter in combination with 'filterModes' = true!");
		comments.put(FILTERMODES, "If true, link counts from legs performed on modes not included in the 'analyzedModes' parameter are ignored.");
		return comments;
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
	
	public int getWriteCountsInterval() {
		return writeCountsInterval;
	}
	
	public void setWriteCountsInterval(int writeCountsInterval) {
		this.writeCountsInterval = writeCountsInterval;
	}
	
	public int getAverageCountsOverIterations() {
		return averageCountsOverIterations;
	}
	
	public void setAverageCountsOverIterations(int averageCountsOverIterations) {
		this.averageCountsOverIterations = averageCountsOverIterations;
	}
	
	public boolean isFilterModes() {
		return this.filterModes;
	}
	
	public void setFilterModes(final boolean filterModes) {
		this.filterModes = filterModes;
	}
	
	public String getAnalyzedModes() {
		return this.analyzedModes;
	}
	
	public void setAnalyzedModes(final String analyzedModes) {
		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
	}
}