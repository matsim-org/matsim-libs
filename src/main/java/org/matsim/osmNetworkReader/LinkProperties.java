package org.matsim.osmNetworkReader;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@RequiredArgsConstructor
public class LinkProperties {

	public static final int LEVEL_MOTORWAY = 1;
	public static final int LEVEL_TRUNK = 2;
	public static final int LEVEL_PRIMARY = 3;
	public static final int LEVEL_SECONDARY = 4;
	public static final int LEVEL_TERTIARY = 5;
	public static final int LEVEL_SMALLER_THAN_TERTIARY = 6; // choose a better name

	final int hierachyLevel;
	final double lanesPerDirection;
	final double freespeed;
	final double laneCapacity;
	final boolean oneway;

	static LinkProperties createMotorway() {
		return new LinkProperties(LEVEL_MOTORWAY, 2, 120 / 3.6, 2000, true);
	}

	static LinkProperties createMotorwayLink() {
		return new LinkProperties(LEVEL_MOTORWAY, 1, 80 / 3.6, 1500, true);
	}

	static LinkProperties createTrunk() {
		return new LinkProperties(LEVEL_TRUNK, 1, 80 / 3.6, 2000, false);
	}

	static LinkProperties createTrunkLink() {
		return new LinkProperties(LEVEL_TRUNK, 1, 50 / 3.6, 1500, false);
	}

	static LinkProperties createPrimary() {
		return new LinkProperties(LEVEL_PRIMARY, 1, 80 / 3.6, 1500, false);
	}

	static LinkProperties createPrimaryLink() {
		return new LinkProperties(LEVEL_PRIMARY, 1, 60 / 3.6, 1500, false);
	}

	static LinkProperties createSecondary() {
		return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 800, false);
	}

	static LinkProperties createSecondaryLink() {
		return new LinkProperties(LEVEL_SECONDARY, 1, 30 / 3.6, 800, false);
	}

	static LinkProperties createTertiary() {
		return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 600, false);
	}

	static LinkProperties createTertiaryLink() {
		return new LinkProperties(LEVEL_TERTIARY, 1, 25 / 3.6, 600, false);
	}

	static LinkProperties createUnclassified() {
		return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 600, false);
	}

	static LinkProperties createResidential() {
		return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 15 / 3.6, 600, false);
	}

	static LinkProperties createLivingStreet() {
		return new LinkProperties(LEVEL_SMALLER_THAN_TERTIARY, 1, 10 / 3.6, 300, false);
	}

	static Map<String, LinkProperties> createLinkProperties() {
		Map<String, LinkProperties> result = new HashMap<>();
		result.put("motorway", createMotorway());
		result.put("motorway_link", createMotorwayLink());
		result.put("trunk", createTrunk());
		result.put("trunk_link", createTrunkLink());
		result.put("primary", createPrimary());
		result.put("primary_link", createPrimaryLink());
		result.put("secondary", createSecondary());
		result.put("secondary_link", createSecondaryLink());
		result.put("tertiary", createTertiary());
		result.put("tertiary_link", createTertiaryLink());
		result.put("unclassified", createUnclassified());
		result.put("residential", createResidential());
		result.put("living_street", createLivingStreet());
		return result;
	}
}
