package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

import java.util.Set;

import static org.matsim.contrib.bicycle.BicycleLinkSpeedCalculatorDefaultImpl.hasNotAttribute;

public class BicycleParamsDefaultImpl implements BicycleParams {

	// At first glance it seems like this method could be combined with method computeSurfaceFactor,
//	but almost all values for the different surface types are different from each other
//It is not clear, where the values for different surface types are taken from, it should be referenced here, but it is not. -sme0125
	@Override
	public double getComfortFactor(String surface) {

		// This method included another if/els branch with some conditions on road types which could never be reached. The following comment was
		// written above this branch. Deleting it, because I don't know what it was supposed to do. janek may '23
		//
		// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
		// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg

		if (surface == null) return 1.0;

		return switch (surface) {
			case "paved", "asphalt" -> 1.0;
			case "concrete", "concrete:lanes" -> .95;
			case "concrete_plates", "concrete:plates", "fine_gravel" -> .9;
			case "paving_stones", "paving_stones:35", "paving_stones:30" -> .8;
			case "compacted" -> .7;
			case "unpaved", "asphalt;paving_stones:35", "bricks", "gravel", "ground" -> .6;
			case "sett", "cobblestone;flattened", "cobblestone:flattened" -> .5;
			case "cobblestone", "stone", "grass", "compressed", "paving_stones:3" -> .4;
			case "cobblestone (bad)", "dirt", "earth", "wood", "pebblestone", "sand" -> .3;
			default -> .85;
		};
	}

	@Override
	public double getInfrastructureFactor(String type, String infrastructureValue,
										  BicycleUtils.BicycleInfraAttribute infrastructureAttribute) {
		return switch (infrastructureAttribute) {
			case cycleway -> getInfrastructureFactorForCycleway(type, infrastructureValue);
			case bicycle_infra -> getInfrastructureFactorForBicycleInfra(type, infrastructureValue);
		};
	}


	private double getInfrastructureFactorForCycleway(String type, String cyclewayType) {
		if (type == null) return 0.85;
		if (type.equals("cycleway")) return 1.0;
		if (hasNoCycleway(cyclewayType)) {
			return switch (type) {
				case "trunk" -> 0.05;
				case "primary", "primary_link" -> 0.1;
				case "secondary", "secondary_link" -> 0.3;
				case "tertiary", "tertiary_link" -> 0.4;
				case "unclassified" -> 0.9;
				default -> 0.95;
			};
		} else {
			return switch (type) {
				case "cycleway", "path" -> 1.0;
				case "steps" -> 0.1;
				default -> 0.95;
			};
		}
	}

	private double getInfrastructureFactorForBicycleInfra(String type, String bicycleInfra) {
		if (bicycleInfra != null) {
			switch (bicycleInfra) {
				case "CYCLEWAY_ISOLATED":
				case "CYCLEWAY_ADJOINING_OR_ISOLATED":
				case "CYCLEWAY_ON_HIGHWAY_PROTECTED":
				case "FOOT_AND_CYCLEWAY_SEGREGATED_ISOLATED":
				case "FOOT_AND_CYCLEWAY_SHARED_ISOLATED":
					return 1.0;

				case "CYCLEWAY_ADJOINING":
				case "CYCLEWAY_ON_HIGHWAY_EXCLUSIVE":
				case "FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING":
				case "FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED":
				case "BICYCLE_ROAD":
					return 0.95;

				case "BICYCLE_ROAD_VEHICLE_DESTINATION":
					return 0.90;

				case "CYCLEWAY_ON_HIGHWAY_ADVISORY":
				case "CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE":
				case "FOOT_AND_CYCLEWAY_SHARED_ADJOINING":
				case "FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED": // TODO!
					return 0.85;



				case "FOOTWAY_BICYCLE_YES_ISOLATED":
				case "FOOTWAY_BICYCLE_YES_ADJOINING":
				case "FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED":
				case "PEDESTRIAN_AREA_BICYCLE_YES":
				case "CYCLEWAY_LINK":
					return 0.80;

				case "SHARED_BUS_LANE_BUS_WITH_BIKE":
				case "CROSSING":
				case "NEEDS_CLARIFICATION":
					return 0.75;

				case "NONE":
					break;

				default:
					return 0.80;
			}
		}

		return getHighwayOnlyInfrastructureFallback(type);
	}

	private double getHighwayOnlyInfrastructureFallback(String type) {
		if (type == null) return 0.85;

		return switch (type) {
			case "trunk" -> 0.05;
			case "primary", "primary_link" -> 0.1;
			case "secondary", "secondary_link" -> 0.3;
			case "tertiary", "tertiary_link" -> 0.4;
			case "unclassified" -> 0.9;  // this should be lower
			default -> 0.95; //this pretty high values applys to all the residential streets, not perfect
		};
	}

	// TODO Simon:
	//  * use smoothness and surface as fallback,
	//  * make sure to use the bicycle smoothness/surface when we have some separate infrasturture (cycleway:right=track and cycleway:right:surface=paving_stones)
	@Override
	public double computeSurfaceFactor(Link link, BicycleUtils.BicycleInfraAttribute attr) {
		// through the getter, which also finds the osm:-prefixed variant of the attribute
		String surface = BicycleUtils.getSurface(link);
		if (hasNotAttribute(link, BicycleUtils.WAY_TYPE)
			|| ignoresLinkSurface(link, attr)
			|| surface == null
		) {
			return 1.0;
		}

		//so, the link is NOT a cycleway, and has a surface attribute
		switch (surface) {
			case "paved":
			case "asphalt":
				return 1.0;

			case "cobblestone (bad)":
			case "grass":
				return 0.4;

			case "cobblestone;flattened":
			case "cobblestone:flattened":
			case "sett":
			case "earth":
				return 0.6;

			case "concrete":
			case "asphalt;paving_stones:35":
			case "compacted":
				return 0.9;

			case "concrete:lanes":
			case "concrete_plates":
			case "concrete:plates":
			case "paving_stones:3":
				return 0.8;

			case "paving_stones":
			case "paving_stones:35":
			case "paving_stones:30":
			case "compressed":
			case "bricks":
			case "stone":
			case "pebblestone":
			case "fine_gravel":
			case "gravel":
			case "ground":
				return 0.7;

			case "sand":
				return 0.2;

			default:
				return 0.5;
		}
	}

	/**
	 * bicycle_infra values of a track or path of its own, as opposed to a lane, bus lane or bicycle road on the carriageway
	 */
	private static final Set<String> SEPARATE_PATH_INFRA = Set.of(
		"CYCLEWAY_ADJOINING", "CYCLEWAY_ISOLATED", "CYCLEWAY_ADJOINING_OR_ISOLATED",
		"FOOT_AND_CYCLEWAY_SHARED_ADJOINING", "FOOT_AND_CYCLEWAY_SHARED_ISOLATED", "FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED",
		"FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING", "FOOT_AND_CYCLEWAY_SEGREGATED_ISOLATED", "FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED",
		"FOOTWAY_BICYCLE_YES_ADJOINING", "FOOTWAY_BICYCLE_YES_ISOLATED", "FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED"
	);

	/**
	 * highway types of ways built for walking or cycling; a link of such a type is the path itself
	 */
	private static final Set<String> PATH_TYPES = Set.of("cycleway", "path", "footway", "pedestrian", "track", "bridleway", "steps");

	/**
	 * Whether the surface factor leaves the link's surface out. With cycleway this is the legacy rule (highway=cycleway). With
	 * bicycle_infra it is the case where the link's surface is not the one the bicycle rides on: a track or path tagged on the
	 * road, so that the link (or its _bike twin) carries the type and surface of the road. A separately mapped path has its own
	 * surface, and on lanes, bus lanes, bicycle roads or in mixed traffic the bicycle rides on the road surface.
	 */
	private static boolean ignoresLinkSurface(Link link, BicycleUtils.BicycleInfraAttribute attr) {
		return switch (attr) {
			case cycleway -> "cycleway".equals(link.getAttributes().getAttribute(BicycleUtils.WAY_TYPE));
			case bicycle_infra -> {
				// null checks first: Set.of(..).contains(null) throws
				String infra = BicycleUtils.getBicycleInfrastructureValue(link, attr);
				String type = highwayType(link);
				yield infra != null && SEPARATE_PATH_INFRA.contains(infra) && (type == null || !PATH_TYPES.contains(type));
			}
		};
	}

	/**
	 * The link type without the "highway." prefix that networks converted from SUMO carry (e.g. highway.cycleway).
	 */
	private static String highwayType(Link link) {
		Object type = link.getAttributes().getAttribute(BicycleUtils.WAY_TYPE);
		if (type == null) return null;
		String highwayType = type.toString();
		if (highwayType.startsWith("highway.")) {
			highwayType = highwayType.substring("highway.".length());
		}
		int compound = highwayType.indexOf('|');
		return compound < 0 ? highwayType : highwayType.substring(0, compound);
	}

	@Override
	public double getGradient_pct(Link link) {

		if (!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()) return 0.;

		var fromZ = link.getFromNode().getCoord().getZ();
		var toZ = link.getToNode().getCoord().getZ();
		var gradient = (toZ - fromZ) / link.getLength() * 100;
		// No positive utility for downhill, only negative for uphill
		return Math.max(0, gradient);
	}


	// define how you identify a cycleway based on OSM tags
	private static boolean hasNoCycleway(String cyclewayType) {
		return (cyclewayType == null || cyclewayType.equals("no") || cyclewayType.equals("none"));
	}


}
