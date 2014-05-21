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

import java.util.Map;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * For increased modularity, the different scenario elements
 * (subpopulatons, networks...) are merged at import. This config group
 * allows to configure this.
 *
 * @author thibautd
 */
public class ScenarioMergingConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "scenarioMerging";

	private String freightPopulationId = "freight";
	private String freightPlansFile = null;
	private String freightFacilitiesFile = null;

	private String crossBorderPopulationId = "crossBorder";
	private String crossBorderPlansFile = null;
	private String crossBorderFacilities = null;

	private String ptSubnetworkFile = null;

	private String thinnedTransitRouterNetworkFile = null;

	public ScenarioMergingConfigGroup( ) {
		super( GROUP_NAME );
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> comments = super.getComments();

		comments.put( "freightPopulationId" , "the ID to use to define the replanning modules for the freight subpopulation. May be the same as for crossborder if convenient." );
		comments.put( "crossBorderPopulationId" , "the ID to use to define the replanning modules for the crossborder subpopulation. May be the same as for freight, if convenient." );

		comments.put( "ptSubnetworkFile" , "the file containing the PT-specific part of the network, to allow changing the road network or pt network independently. This works only because our buses do not drive on car links!" );
	
		comments.put( "thinnedTransitRouterNetworkFile" , "the file containing the pre-processed transit router network. This is a performance pre-processing, which is by no means mandatory." );

		return comments;
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

	@StringGetter( "crossBorderFacilities" )
	public String getCrossBorderFacilities() {
		return this.crossBorderFacilities;
	}

	@StringSetter( "crossBorderFacilities" )
	public void setCrossBorderFacilities(String crossBorderFacilities) {
		this.crossBorderFacilities = crossBorderFacilities;
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


}

