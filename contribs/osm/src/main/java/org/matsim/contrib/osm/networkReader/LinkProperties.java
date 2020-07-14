package org.matsim.contrib.osm.networkReader;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds MATSim internal link properties mapped to OSM hierarchy levels.
 */
public class LinkProperties {

	public static final int LEVEL_MOTORWAY = 1;
	public static final int LEVEL_TRUNK = 2;
	public static final int LEVEL_PRIMARY = 3;
	public static final int LEVEL_SECONDARY = 4;
	public static final int LEVEL_TERTIARY = 5;
	public static final int LEVEL_UNCLASSIFIED = 6;
	public static final int LEVEL_RESIDENTIAL = 7;
	public static final int LEVEL_LIVING_STREET = 8;

	final int hierachyLevel;
	final double lanesPerDirection;
	final double freespeed;
	final double laneCapacity;
	final boolean oneway;

	public LinkProperties(int hierachyLevel, double lanesPerDirection, double freespeed, double laneCapacity, boolean oneway) {
		this.hierachyLevel = hierachyLevel;
		this.lanesPerDirection = lanesPerDirection;
		this.freespeed = freespeed;
		this.laneCapacity = laneCapacity;
		this.oneway = oneway;
	}

	public int getHierachyLevel() {
		return hierachyLevel;
	}

	public double getLanesPerDirection() {
		return lanesPerDirection;
	}

	public double getFreespeed() {
		return freespeed;
	}

	public double getLaneCapacity() {
		return laneCapacity;
	}

	public boolean isOneway() {
		return oneway;
	}

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
        return new LinkProperties(LEVEL_UNCLASSIFIED, 1, 15 / 3.6, 600, false);
	}

	static LinkProperties createResidential() {
        return new LinkProperties(LEVEL_RESIDENTIAL, 1, 15 / 3.6, 600, false);
	}

	static LinkProperties createLivingStreet() {
        return new LinkProperties(LEVEL_LIVING_STREET, 1, 10 / 3.6, 300, false);
	}

	/**
	 * Creates default link properties.
	 */
    public static Map<String, LinkProperties> createLinkProperties() {
		ConcurrentMap<String, LinkProperties> result = new ConcurrentHashMap<>();
		result.put(OsmTags.MOTORWAY, createMotorway());
		result.put(OsmTags.MOTORWAY_LINK, createMotorwayLink());
		result.put(OsmTags.TRUNK, createTrunk());
		result.put(OsmTags.TRUNK_LINK, createTrunkLink());
		result.put(OsmTags.PRIMARY, createPrimary());
		result.put(OsmTags.PRIMARY_LINK, createPrimaryLink());
		result.put(OsmTags.SECONDARY, createSecondary());
		result.put(OsmTags.SECONDARY_LINK, createSecondaryLink());
		result.put(OsmTags.TERTIARY, createTertiary());
		result.put(OsmTags.TERTIARY_LINK, createTertiaryLink());
		result.put(OsmTags.UNCLASSIFIED, createUnclassified());
		result.put(OsmTags.RESIDENTIAL, createResidential());
		result.put(OsmTags.LIVING_STREET, createLivingStreet());
		return result;
	}


	/**
	 * Lane capacity adjusted if link might be a crossing.
	 */
	public static double getLaneCapacity(double linkLength, LinkProperties properties) {
		double capacityFactor = linkLength < 100 ? 2 : 1;
		return properties.laneCapacity * capacityFactor;
	}

    /**
     * Calculate free speed of a link based on heuristic if it is an urban link.
     */
	public static double calculateSpeedIfSpeedTag(double maxSpeed) {
        double urbanSpeedFactor = maxSpeed <= 51 / 3.6 ? 0.5 : 1.0; // assume for links with max speed lower than 51km/h to be in urban areas. Reduce speed to reflect traffic lights and suc
        return maxSpeed * urbanSpeedFactor;
    }

	/**
	 * For links with unknown max speed we assume that links with a length of less than 300m are urban links. For urban
	 * links with a length of 0m the speed is 10km/h. For links with a length of 300m the speed is the default freespeed
	 * property for that highway type. For links with a length between 0 and 300m the speed is interpolated linearly.
	 *
	 * All links longer than 300m the default freesped property is assumed
	 */
	public static double calculateSpeedIfNoSpeedTag(double linkLength, LinkProperties properties) {
		if (properties.hierachyLevel > LinkProperties.LEVEL_MOTORWAY && properties.hierachyLevel <= LinkProperties.LEVEL_TERTIARY
				&& linkLength < 300) {
			return ((10 + (properties.freespeed - 10) / 300 * linkLength) / 3.6);
		}
		return properties.freespeed;
	}
}
