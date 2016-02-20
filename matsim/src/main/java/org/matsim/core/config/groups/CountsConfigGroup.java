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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author dgrether
 * @author mrieser
 */
public final class CountsConfigGroup extends ReflectiveConfigGroup {

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
	private static final String INPUT_CRS = "inputCRS";
	
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
	private String inputCRS = null;
	
	public CountsConfigGroup() {
		super(GROUP_NAME);
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

		comments.put( INPUT_CRS , "The Coordinates Reference System in which the coordinates are expressed in the input file." +
				" At import, the coordinates will be converted to the coordinate system defined in \"global\", and will" +
				"be converted back at export. If not specified, no conversion happens." );
		return comments;
	}

	@StringGetter( OUTPUTFORMAT )
	public String getOutputFormat() {
		return this.outputFormat;
	}

	@StringSetter( OUTPUTFORMAT )
	public void setOutputFormat(final String outputFormat) {
		this.outputFormat = outputFormat;
	}

	@StringGetter( DISTANCEFILTER )
	public Double getDistanceFilter() {
		return this.distanceFilter;
	}

	@StringSetter( DISTANCEFILTER )
	public void setDistanceFilter(final Double distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	@StringGetter( DISTANCEFILTERCENTERNODE )
	public String getDistanceFilterCenterNode() {
		return this.distanceFilterCenterNode;
	}

	@StringSetter( DISTANCEFILTERCENTERNODE )
	public void setDistanceFilterCenterNode(final String distanceFilterCenterNode) {
		this.distanceFilterCenterNode = distanceFilterCenterNode;
	}

	/**
	 * @return the filename of the counts file to be read in
	 */
	@StringGetter( COUNTSINPUTFILENAME )
	public String getCountsFileName() {
		return this.countsFileName;
	}

	/**
	 * @param countsFileName the filename of the counts file to be read in
	 */
	@StringSetter( COUNTSINPUTFILENAME )
	public void setCountsFileName(final String countsFileName) {
		this.countsFileName = countsFileName;
	}

	@StringGetter( COUNTSSCALEFACTOR )
	public double getCountsScaleFactor() {
		return this.countsScaleFactor;
	}

	@StringSetter( COUNTSSCALEFACTOR )
	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
	
	@StringGetter( WRITECOUNTSINTERVAL )
	public int getWriteCountsInterval() {
		return writeCountsInterval;
	}
	
	@StringSetter( WRITECOUNTSINTERVAL )
	public void setWriteCountsInterval(int writeCountsInterval) {
		this.writeCountsInterval = writeCountsInterval;
	}
	
	@StringGetter( AVERAGECOUNTSOVERITERATIONS )
	public int getAverageCountsOverIterations() {
		return averageCountsOverIterations;
	}
	
	@StringSetter( AVERAGECOUNTSOVERITERATIONS )
	public void setAverageCountsOverIterations(int averageCountsOverIterations) {
		this.averageCountsOverIterations = averageCountsOverIterations;
	}
	
	@StringGetter( FILTERMODES )
	public boolean isFilterModes() {
		return this.filterModes;
	}
	
	@StringSetter( FILTERMODES )
	public void setFilterModes(final boolean filterModes) {
		this.filterModes = filterModes;
	}
	
	@StringGetter( ANALYZEDMODES )
	public String getAnalyzedModes() {
		return this.analyzedModes;
	}
	
	@StringSetter( ANALYZEDMODES )
	public void setAnalyzedModes(final String analyzedModes) {
		this.analyzedModes = analyzedModes.toLowerCase(Locale.ROOT);
	}

	@StringGetter( INPUT_CRS )
	public String getInputCRS() {
		return inputCRS;
	}

	@StringSetter( INPUT_CRS )
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}
}
