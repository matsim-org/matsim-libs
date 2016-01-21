/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioMergingConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.generation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * For increased modularity, the different scenario elements
 * (subpopulatons, networks...) are merged at import. This config group
 * allows to configure this.
 *
 * @author thibautd
 */
public class ScenarioMergingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "scenarioMerging";

	private boolean performMerging = true;

	private double samplingRate = 0.1;

	private Coord dilutionCenter = new Coord(683518.0, 246836.0);
	private double dilutionRadiusKm = 30;
	private boolean performDilution = true;

	private String freightPopulationId = "freight";
	private String freightPlansFile = null;
	private String freightFacilitiesFile = null;

	private String crossBorderPopulationId = "crossBorder";
	private String crossBorderPlansFile = null;
	private String crossBorderFacilitiesFile = null;

	private String ptSubnetworkFile = null;

	private String thinnedTransitRouterNetworkFile = null;

	private Set<String> modesOfFacilityLinks = Collections.singleton( TransportMode.car );

	public ScenarioMergingConfigGroup( ) {
		super( GROUP_NAME );
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> comments = super.getComments();

		comments.put( "performMerging" , "if set to false, merging process will be skipped (meaning that the files defined here will be). Useful to re-read an output for re-start." );

		comments.put( "freightPopulationId" , "the ID to use to define the replanning modules for the freight subpopulation. May be the same as for crossborder if convenient." );
		comments.put( "crossBorderPopulationId" , "the ID to use to define the replanning modules for the crossborder subpopulation. May be the same as for freight, if convenient." );

		comments.put( "ptSubnetworkFile" , "the file containing the PT-specific part of the network, to allow changing the road network or pt network independently. This works only because our buses do not drive on car links!" );
	
		comments.put( "thinnedTransitRouterNetworkFile" , "the file containing the pre-processed transit router network. This is a performance pre-processing, which is by no means mandatory." );

		comments.put( "samplingRate" , "the proportion of the subpopulation to retain: those files are 100pct samples, the filtering is done at import. One can of course also create sample files and set this parameter to 1." );
		comments.put( "modesOfFacilityLinks" , "facilities will only be links to links having *ALL* of those modes allowed. Setting this to something else than only car makes sense for \"multimodal\" routing." );

		return comments;
	}

	@StringGetter( "performMerging" )
	public boolean isPerformMerging() {
		return this.performMerging;
	}

	@StringSetter( "performMerging" )
	public void setPerformMerging(final boolean performMerging) {
		this.performMerging = performMerging;
	}

	@StringGetter( "samplingRate" )
	public double getSamplingRate() {
		return this.samplingRate;
	}

	@StringSetter( "samplingRate" )
	public void setSamplingRate(final double samplingRate) {
		if ( samplingRate < 0 || samplingRate > 1 ) throw new IllegalArgumentException( samplingRate+" is not between 0 and 1" );
		this.samplingRate = samplingRate;
	}

	public Coord getDilutionCenter() {
		return this.dilutionCenter;
	}

	@StringGetter( "dilutionCenter" )
	private String getDilutionCenterString() {
		return this.dilutionCenter.getX() +";"+ this.dilutionCenter.getY();
	}

	public void setDilutionCenter(final Coord dilutionCenter) {
		this.dilutionCenter = dilutionCenter;
	}

	@StringSetter( "dilutionCenter" )
	private void setDilutionCenter(final String v) {
		final String[] xy = v.split( ";" );

		if ( xy.length != 2 ) throw new IllegalArgumentException( v+" is not in form x;y" );

		final double x = Double.parseDouble( xy[ 0 ] );
		final double y = Double.parseDouble( xy[ 1 ] );

		setDilutionCenter(new Coord(x, y));
	}

	@StringGetter( "dilutionRadiusKm" )
	public double getDilutionRadiusKm() {
		return this.dilutionRadiusKm;
	}

	public double getDilutionRadiusM() {
		return this.dilutionRadiusKm * 1000;
	}

	@StringSetter( "dilutionRadiusKm" )
	public void setDilutionRadiusKm(double dilutionRadiusKm) {
		this.dilutionRadiusKm = dilutionRadiusKm;
	}

	@StringGetter( "performDilution" )
	public boolean getPerformDilution() {
		return this.performDilution;
	}

	@StringSetter( "performDilution" )
	public void setPerformDilution(boolean performDilution) {
		this.performDilution = performDilution;
	}

	@StringGetter( "freightPopulationId" )
	public String getFreightPopulationId() {
		return this.freightPopulationId;
	}

	@StringSetter( "freightPopulationId" )
	public void setFreightPopulationId(String freightPopulationId) {
		this.freightPopulationId = freightPopulationId;
	}

	@StringGetter( "freightPlansFile" )
	public String getFreightPlansFile() {
		return this.freightPlansFile;
	}

	@StringSetter( "freightPlansFile" )
	public void setFreightPlansFile(String freightPlansFile) {
		this.freightPlansFile = freightPlansFile;
	}

	@StringGetter( "freightFacilitiesFile" )
	public String getFreightFacilitiesFile() {
		return this.freightFacilitiesFile;
	}

	@StringSetter( "freightFacilitiesFile" )
	public void setFreightFacilitiesFile(String freightFacilitiesFile) {
		this.freightFacilitiesFile = freightFacilitiesFile;
	}

	@StringGetter( "crossBorderPopulationId" )
	public String getCrossBorderPopulationId() {
		return this.crossBorderPopulationId;
	}

	@StringSetter( "crossBorderPopulationId" )
	public void setCrossBorderPopulationId(String crossBorderPopulationId) {
		this.crossBorderPopulationId = crossBorderPopulationId;
	}

	@StringGetter( "crossBorderPlansFile" )
	public String getCrossBorderPlansFile() {
		return this.crossBorderPlansFile;
	}

	@StringSetter( "crossBorderPlansFile" )
	public void setCrossBorderPlansFile(String crossBorderPlansFile) {
		this.crossBorderPlansFile = crossBorderPlansFile;
	}

	@StringGetter( "crossBorderFacilitiesFile" )
	public String getCrossBorderFacilitiesFile() {
		return this.crossBorderFacilitiesFile;
	}

	@StringSetter( "crossBorderFacilitiesFile" )
	public void setCrossBorderFacilitiesFile(String crossBorderFacilities) {
		this.crossBorderFacilitiesFile = crossBorderFacilities;
	}

	@StringGetter( "ptSubnetworkFile" )
	public String getPtSubnetworkFile() {
		return this.ptSubnetworkFile;
	}

	@StringSetter( "ptSubnetworkFile" )
	public void setPtSubnetworkFile(String ptSubnetworkFile) {
		this.ptSubnetworkFile = ptSubnetworkFile;
	}

	@StringGetter( "thinnedTransitRouterNetworkFile" )
	public String getThinnedTransitRouterNetworkFile() {
		return this.thinnedTransitRouterNetworkFile;
	}

	@StringSetter( "thinnedTransitRouterNetworkFile" )
	public void setThinnedTransitRouterNetworkFile(
			final String thinnedTransitRouterNetworkFile) {
		this.thinnedTransitRouterNetworkFile = thinnedTransitRouterNetworkFile;
	}

	public Set<String> getModesOfFacilityLinks() {
		return this.modesOfFacilityLinks;
	}

	@StringGetter( "modesOfFacilityLinks" )
	private String getModesOfFacilityLinksAsString() {
		return CollectionUtils.setToString( getModesOfFacilityLinks() );
	}

	public void setModesOfFacilityLinks(
			final Set<String> modesOfFacilityLinks) {
		this.modesOfFacilityLinks = modesOfFacilityLinks;
	}

	@StringSetter( "modesOfFacilityLinks" )
	private void setModesOfFacilityLinksAsString(
			final String modesOfFacilityLinks) {
		setModesOfFacilityLinks(
				CollectionUtils.stringToSet(
					modesOfFacilityLinks ) );
	}
}

