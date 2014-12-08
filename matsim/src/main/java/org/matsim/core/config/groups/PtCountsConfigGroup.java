/* *********************************************************************** *
 * project: org.matsim.*
 * TransitConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * @author nagel
 */
public class PtCountsConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "ptCounts";

	private static final String OUTPUTFORMAT = "outputformat";
	private static final String DISTANCEFILTER = "distanceFilter";
	private static final String DISTANCEFILTERCENTERNODE = "distanceFilterCenterNode";
	private static final String OCCUPANCY_COUNTS_INPUT_FILENAME = "inputOccupancyCountsFile";
	private static final String BOARD_COUNTS_INPUT_FILENAME = "inputBoardCountsFile";
	private static final String ALIGHT_COUNTS_INPUT_FILENAME = "inputAlightCountsFile";
	private static final String COUNTSSCALEFACTOR = "countsScaleFactor";
	private static final String PT_COUNTS_INTERVAL = "ptCountsInterval" ;

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
	private String occupancyCountsFileName = null;
	private String boardCountsFileName = null;
	private String alightCountsFileName = null;
	/**
	 * the scaling for the counts
	 */
	private double countsScaleFactor = 1.0;
	private int ptCountsInterval = 10 ;

	public PtCountsConfigGroup() {
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
		} else if (OCCUPANCY_COUNTS_INPUT_FILENAME.equals(key)) {
			return getOccupancyCountsFileName();
		} else if (BOARD_COUNTS_INPUT_FILENAME.equals(key)) {
			return getBoardCountsFileName();
		} else if (ALIGHT_COUNTS_INPUT_FILENAME.equals(key)) {
			return getAlightCountsFileName();
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			return Double.toString(getCountsScaleFactor());
		} else if (PT_COUNTS_INTERVAL.equals(key)) {
			return Integer.toString(getPtCountsInterval());
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
		} else if (OCCUPANCY_COUNTS_INPUT_FILENAME.equals(key)) {
			setOccupancyCountsFileName(value);
		} else if (BOARD_COUNTS_INPUT_FILENAME.equals(key)) {
			setBoardCountsFileName(value);
		} else if (ALIGHT_COUNTS_INPUT_FILENAME.equals(key)) {
			setAlightCountsFileName(value);
		} else if (COUNTSSCALEFACTOR.equals(key)) {
			this.setCountsScaleFactor(Double.parseDouble(value));
		} else if (PT_COUNTS_INTERVAL.equals(key)) {
			this.setPtCountsInterval(Integer.parseInt(value));
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
		this.addParameterToMap(map, OCCUPANCY_COUNTS_INPUT_FILENAME);
		this.addParameterToMap(map, BOARD_COUNTS_INPUT_FILENAME);
		this.addParameterToMap(map, ALIGHT_COUNTS_INPUT_FILENAME);
		this.addParameterToMap(map, COUNTSSCALEFACTOR);
		this.addParameterToMap(map, PT_COUNTS_INTERVAL);
		return map;
	}
	
	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(OUTPUTFORMAT, CountsConfigGroup.COUNTS_OUTPUTFORMAT_COMMENT ) ;
		comments.put(DISTANCEFILTER,  CountsConfigGroup.COUNTS_DISTANCEFILTER_COMMENT ) ;
		comments.put(DISTANCEFILTERCENTERNODE, CountsConfigGroup.COUNTS_DISTANCEFILTERCENTERNODE_COMMENT ) ;
		comments.put(OCCUPANCY_COUNTS_INPUT_FILENAME, "input file containing the occupancy counts for pt" ) ;
		comments.put(ALIGHT_COUNTS_INPUT_FILENAME, "input file containing the alighting (getting off) counts for pt" ) ;
		comments.put(BOARD_COUNTS_INPUT_FILENAME, "input file containing the boarding (getting on) counts for pt" ) ;
		comments.put(COUNTSSCALEFACTOR, CountsConfigGroup.COUNTSSCALEFACTOR_COMMENT ) ;
		comments.put(PT_COUNTS_INTERVAL, "every how many iterations (starting with 0) counts comparisons are generated" );
		return comments;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Double getDistanceFilter() {
		return distanceFilter;
	}

	public void setDistanceFilter(Double distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	public String getDistanceFilterCenterNode() {
		return distanceFilterCenterNode;
	}

	public void setDistanceFilterCenterNode(String distanceFilterCenterNode) {
		this.distanceFilterCenterNode = distanceFilterCenterNode;
	}

	public String getOccupancyCountsFileName() {
		return occupancyCountsFileName;
	}

	public void setOccupancyCountsFileName(String occupancyCountsFileName) {
		this.occupancyCountsFileName = occupancyCountsFileName;
	}

	public String getBoardCountsFileName() {
		return boardCountsFileName;
	}

	public void setBoardCountsFileName(String boardCountsFileName) {
		this.boardCountsFileName = boardCountsFileName;
	}

	public String getAlightCountsFileName() {
		return alightCountsFileName;
	}

	public void setAlightCountsFileName(String alightCountsFileName) {
		this.alightCountsFileName = alightCountsFileName;
	}

	public double getCountsScaleFactor() {
		return countsScaleFactor;
	}

	public void setCountsScaleFactor(double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}

	public int getPtCountsInterval() {
		return ptCountsInterval;
	}

	public void setPtCountsInterval(int ptCountsInterval) {
		this.ptCountsInterval = ptCountsInterval;
	}


}
