package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

import java.util.Objects;

import static org.matsim.contrib.bicycle.BicycleLinkSpeedCalculatorDefaultImpl.hasNotAttribute;

public class BicycleParamsDefaultImpl implements BicycleParams {

	// At first glance it seems like this method could be combined with method computeSurfaceFactor,
//	but almost all values for the different surface types are different from each other
//It is not clear, where the values for different surface types are taken from, it should be referenced here, but it is not. -sme0125
	@Override
	public double getComfortFactor( String surface ) {

		// This method included another if/els branch with some conditions on road types which could never be reached. The following comment was
		// written above this branch. Deleting it, because I don't know what it was supposed to do. janek may '23
		//
		// For many primary and secondary roads, no surface is specified because they are by default assumed to be is asphalt.
		// For tertiary roads street this is not true, e.g. Friesenstr. in Kreuzberg

		if (surface == null) return 1.0;

		return switch (surface) {
			case "paved", "asphalt" -> 1.0;
			case "concrete:lanes" -> .95;
			case "concrete_plates", "concrete:plates", "fine_gravel" -> .9;
			case "paving_stones", "paving_stones:35", "paving_stones:30" -> .8;
			case "compacted" -> .7;
			case "unpaved", "asphalt;paving_stones:35", "bricks", "gravel", "ground" -> .6;
			case "sett", "cobblestone;flattened", "cobblestone:flattened" -> .5;
			case "cobblestone", "stone", "grass", "compressed", "paving_stones:3" -> .4;
			case "cobblestone (bad)", "dirt", "earth", "wood", "pebblestone", "sand" -> .3;
			case "concrete" -> .1;
			default -> .85;
		};
	}

	@Override
	public double getInfrastructureFactor( String type, String cyclewaytype ) {

		// The method was unreadable before, so I hope I got the logic right. basically this differentiates between explicit cycleway tags, where the
		// road type has an influence on the factor, i.e. cycling along a primary road without cycle lane is less attractive compared to a tertiary road.
		// On the other hand if cycleways are present the factor is always 0.95, exept the cycle tracks has steps (horrible) or the road type is a
		// cycleway anyway (very nice)
		// in case there is no road type a medium factor of 0.85 is assigned
		// janek may '23

		if (type == null) return 0.85;
		if (hasNoCycleway(cyclewaytype)) {
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

	@Override
	public double computeSurfaceFactor(Link link) {
		if (hasNotAttribute(link, BicycleUtils.WAY_TYPE)
			|| BicycleUtils.CYCLEWAY.equals(link.getAttributes().getAttribute(BicycleUtils.WAY_TYPE))
			|| hasNotAttribute(link, BicycleUtils.SURFACE)
		) {
			return 1.0;
		}

		//so, the link is NOT a cycleway, and has a surface attribute
		String surface = (String) link.getAttributes().getAttribute(BicycleUtils.SURFACE);
		switch (Objects.requireNonNull(surface)) {
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

	@Override
	public double getGradient_pct( Link link ) {

		if (!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()) return 0.;

		var fromZ = link.getFromNode().getCoord().getZ();
		var toZ = link.getToNode().getCoord().getZ();
		var gradient = (toZ - fromZ) / link.getLength() * 100;
		// No positive utility for downhill, only negative for uphill
		return Math.max(0, gradient);
	}


	// define how you identify a cycleway based on OSM tags
	private static boolean hasNoCycleway( String cyclewayType ) {
		return (cyclewayType == null || cyclewayType.equals("no") || cyclewayType.equals("none"));
	}


}
