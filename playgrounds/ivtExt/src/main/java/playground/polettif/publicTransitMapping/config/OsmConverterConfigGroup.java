/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.publicTransitMapping.config;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.publicTransitMapping.osm.OsmMultimodalNetworkConverter;
import playground.polettif.publicTransitMapping.osm.lib.OsmTag;
import playground.polettif.publicTransitMapping.osm.lib.OsmValue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Config group for osm conversion {@link OsmMultimodalNetworkConverter}
 * @author polettif
 */
public class OsmConverterConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "OsmConverter";

	private static final String OSM_FILE ="osmFile";
	private static final String OUTPUT_NETWORK_FILE ="outputNetworkFile";
	private static final String OUTPUT_COORDINATE_SYSTEM ="outputCoordinateSystem";
	private static final String KEEP_PATHS ="keepPaths";
	public static final String MAX_LINK_LENGTH = "maxLinkLength";
	public static final String SCALE_MAX_SPEED = "scaleMaxSpeed";
	public static final String GUESS_FREE_SPEED = "guessFreeSpeed";

	private String osmFile;
	private String outputNetworkFile;
	private String outputCoordinateSystem;
	private CoordinateTransformation coordinateTransformation = new IdentityTransformation();

	private double maxLinkLength = 500.0;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;
	private boolean guessFreeSpeed = false;

	@StringGetter(OSM_FILE)
	public String getOsmFile() {
		return osmFile;
	}
	@StringSetter(OSM_FILE)
	public void setOsmFile(String osmFile) {
		this.osmFile = osmFile;
	}

	@StringGetter(KEEP_PATHS)
	public boolean getKeepPaths() {
		return keepPaths;
	}
	@StringSetter(KEEP_PATHS)
	public void setKeepPaths(boolean keepPaths) {
		this.keepPaths = keepPaths;
	}

	@StringGetter(GUESS_FREE_SPEED)
	public boolean getGuessFreeSpeed() {
		return guessFreeSpeed;
	}
	@StringSetter(GUESS_FREE_SPEED)
	public void setGuessFreeSpeed(boolean guessFreeSpeed) {
		this.guessFreeSpeed = guessFreeSpeed;
	}

	@StringGetter(MAX_LINK_LENGTH)
	public double getMaxLinkLength() {
		return maxLinkLength;
	}
	@StringSetter(MAX_LINK_LENGTH)
	public void setMaxLinkLength(double maxLinkLength) {
		this.maxLinkLength = maxLinkLength;
	}

	@StringGetter(SCALE_MAX_SPEED)
	public boolean getScaleMaxSpeed() {
		return scaleMaxSpeed;
	}
	@StringSetter(SCALE_MAX_SPEED)
	public void setScaleMaxSpeed(boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}


	@StringGetter(OUTPUT_NETWORK_FILE)
	public String getOutputNetworkFile() {
		return outputNetworkFile;
	}
	@StringSetter(OUTPUT_NETWORK_FILE)
	public void setOutputNetworkFile(String outputNetworkFile) {
		this.outputNetworkFile = outputNetworkFile;
	}
	@StringGetter(OUTPUT_COORDINATE_SYSTEM)
	public String getOutputCoordinateSystem() {
		return outputCoordinateSystem;
	}
	@StringSetter(OUTPUT_COORDINATE_SYSTEM)
	public void setOutputCoordinateSystem(String outputCoordinateSystem) {
		if(outputCoordinateSystem != null) {
			this.outputCoordinateSystem = outputCoordinateSystem;
			this.coordinateTransformation = TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem);
		}
	}

	public OsmConverterConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(OSM_FILE,
				"The path to the osm file.");
		map.put(KEEP_PATHS,
				"Sets whether the detailed geometry of the roads should be retained in the conversion or not.\n" +
				"\t\tKeeping the detailed paths results in a much higher number of nodes and links in the resulting MATSim network.\n" +
				"\t\tNot keeping the detailed paths removes all nodes where only one road passes through, thus only real intersections\n" +
				"\t\tor branchings are kept as nodes. This reduces the number of nodes and links in the network, but can in some rare\n" +
				"\t\tcases generate extremely long links (e.g. for motorways with only a few ramps every few kilometers).\n" +
				"\t\tDefaults to <code>false</code>.");
		map.put(GUESS_FREE_SPEED,
				"If true: The first two digits of the maxspeed tag are used if it cannot be parsed (e.g. \"50; 80\" or similar).");
		map.put(SCALE_MAX_SPEED,
				"In case the speed limit allowed does not represent the speed a vehicle can actually realize, e.g. by constrains of\n" +
				"\t\ttraffic lights not explicitly modeled, a kind of \"average simulated speed\" can be used.\n" +
				"\t\tDefaults to false. Set true to scale the speed limit down by the value specified by the wayValues)");
		return map;
	}

	public CoordinateTransformation getCoordinateTransformation() {
		return this.coordinateTransformation;
	}

	/**
	 * @return A new default OsmConverter config
	 */
	public static OsmConverterConfigGroup createDefaultConfig() {
		Set<String> carSingleton = Collections.singleton("car");
		Set<String> railSingleton = Collections.singleton("rail");

		OsmConverterConfigGroup defaultConfig = new OsmConverterConfigGroup();
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.MOTORWAY, 		OsmTag.HIGHWAY, 2, 120.0 / 3.6, 1.0, 2000, 	true, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.MOTORWAY,		OsmTag.HIGHWAY, 2, 120.0 / 3.6, 1.0, 2000, 	true, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.MOTORWAY_LINK,	OsmTag.HIGHWAY, 1, 80.0 / 3.6, 1.0, 1500, 	true, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.TRUNK,			OsmTag.HIGHWAY, 1, 80.0 / 3.6, 1.0, 2000, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.TRUNK_LINK,		OsmTag.HIGHWAY, 1, 50.0 / 3.6, 1.0, 1500, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.PRIMARY,		OsmTag.HIGHWAY, 1, 80.0 / 3.6, 1.0, 1500, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.PRIMARY_LINK,	OsmTag.HIGHWAY, 1, 60.0 / 3.6, 1.0, 1500, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.SECONDARY,		OsmTag.HIGHWAY, 1, 60.0 / 3.6, 1.0, 1000, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.TERTIARY,		OsmTag.HIGHWAY, 1, 50.0 / 3.6, 1.0, 600, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.MINOR,			OsmTag.HIGHWAY, 1, 40.0 / 3.6, 1.0, 600, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.UNCLASSIFIED,	OsmTag.HIGHWAY, 1, 50.0 / 3.6, 1.0, 600, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.RESIDENTIAL,	OsmTag.HIGHWAY, 1, 30.0 / 3.6, 1.0, 600, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.LIVING_STREET,	OsmTag.HIGHWAY, 1, 15.0 / 3.6, 1.0, 300, 	false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.SERVICE,		OsmTag.HIGHWAY, 1, 15.0 / 3.6, 1.0, 200, 	false, carSingleton));

		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.RAIL,			OsmTag.RAILWAY,	1, 160.0 / 3.6, 1.0, 100, false, railSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.TRAM,			OsmTag.RAILWAY,	1, 40.0 / 3.6, 1.0, 100, true, railSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(OsmValue.LIGHT_RAIL,		OsmTag.RAILWAY,	1, 80.0 / 3.6, 1.0, 100, false, railSingleton));

		return defaultConfig;
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case OsmWayParams.SET_NAME :
				return new OsmWayParams();
			default:
				throw new IllegalArgumentException("Unknown parameterset name!");
		}
	}

}
