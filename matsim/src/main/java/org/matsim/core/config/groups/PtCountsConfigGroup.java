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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 */
public final class PtCountsConfigGroup extends ReflectiveConfigGroup {

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

	@StringGetter( OUTPUTFORMAT )
	public String getOutputFormat() {
		return outputFormat;
	}

	@StringSetter( OUTPUTFORMAT )
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	@StringGetter( DISTANCEFILTER )
	public Double getDistanceFilter() {
		return distanceFilter;
	}

	@StringSetter( DISTANCEFILTER )
	public void setDistanceFilter(Double distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	@StringGetter( DISTANCEFILTERCENTERNODE )
	public String getDistanceFilterCenterNode() {
		return distanceFilterCenterNode;
	}

	@StringSetter( DISTANCEFILTERCENTERNODE )
	public void setDistanceFilterCenterNode(String distanceFilterCenterNode) {
		this.distanceFilterCenterNode = distanceFilterCenterNode;
	}

	@StringGetter( OCCUPANCY_COUNTS_INPUT_FILENAME )
	public String getOccupancyCountsFileName() {
		return occupancyCountsFileName;
	}

	@StringSetter( OCCUPANCY_COUNTS_INPUT_FILENAME )
	public void setOccupancyCountsFileName(String occupancyCountsFileName) {
		this.occupancyCountsFileName = occupancyCountsFileName;
	}

	@StringGetter( BOARD_COUNTS_INPUT_FILENAME )
	public String getBoardCountsFileName() {
		return boardCountsFileName;
	}

	@StringSetter( BOARD_COUNTS_INPUT_FILENAME )
	public void setBoardCountsFileName(String boardCountsFileName) {
		this.boardCountsFileName = boardCountsFileName;
	}

	@StringGetter( ALIGHT_COUNTS_INPUT_FILENAME )
	public String getAlightCountsFileName() {
		return alightCountsFileName;
	}

	@StringSetter( ALIGHT_COUNTS_INPUT_FILENAME )
	public void setAlightCountsFileName(String alightCountsFileName) {
		this.alightCountsFileName = alightCountsFileName;
	}

	@StringGetter( COUNTSSCALEFACTOR )
	public double getCountsScaleFactor() {
		return countsScaleFactor;
	}

	@StringSetter( COUNTSSCALEFACTOR )
	public void setCountsScaleFactor(double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}

	@StringGetter( PT_COUNTS_INTERVAL )
	public int getPtCountsInterval() {
		return ptCountsInterval;
	}

	@StringSetter( PT_COUNTS_INTERVAL )
	public void setPtCountsInterval(int ptCountsInterval) {
		this.ptCountsInterval = ptCountsInterval;
	}


}
