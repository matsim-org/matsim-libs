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
	public static final int LEVEL_PATH = 9;

	/**
	 * Assume for links with max speed lower than 51km/h to be in urban areas.
	 * The free speed is reduced by this factor to account for traffic lights, ROW, etc.
	 *
	 * @see #calculateSpeedIfSpeedTag(double)
	 */
	public static final double DEFAULT_FREESPEED_FACTOR = 0.9;

	/**
	 * Increase lane capacity for links shorter than this value, assuming they are crossing.
	 */
	public static final double DEFAULT_ADJUST_CAPACITY_LENGTH = 50;

	final int hierarchyLevel;
	final double lanesPerDirection;
	final double freespeed;
	final double laneCapacity;
	final boolean oneway;

	public LinkProperties(int hierarchyLevel, double lanesPerDirection, double freespeed, double laneCapacity, boolean oneway) {
		this.hierarchyLevel = hierarchyLevel;
		this.lanesPerDirection = lanesPerDirection;
		this.freespeed = freespeed;
		this.laneCapacity = laneCapacity;
		this.oneway = oneway;
	}

	public int getHierarchyLevel() {
		return hierarchyLevel;
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
	 * Like {@link #getLaneCapacity(double, LinkProperties, double)}, but with default length.
	 * @see #DEFAULT_ADJUST_CAPACITY_LENGTH
	 */
	public static double getLaneCapacity(double linkLength, LinkProperties properties) {
		return getLaneCapacity(linkLength, properties, DEFAULT_ADJUST_CAPACITY_LENGTH);
	}

	/**
	 * Lane capacity adjusted if link might be a crossing.
	 */
	public static double getLaneCapacity(double linkLength, LinkProperties properties, double adjustCapacityLength) {
		double capacityFactor = linkLength < adjustCapacityLength ? 2 : 1;
		return properties.laneCapacity * capacityFactor;
	}

    /**
     * Calculate free speed of a link based on heuristic if it is an urban link.
     */
	public static double calculateSpeedIfSpeedTag(double maxSpeed, double freeSpeedFactor) {
        double urbanSpeedFactor = maxSpeed < 51 / 3.6 ? freeSpeedFactor : 1.0;
        return maxSpeed * urbanSpeedFactor;
    }

	/**
	 * Like {@link #calculateSpeedIfSpeedTag(double, double)}, but with default value as factor.
	 */
	public static double calculateSpeedIfSpeedTag(double maxSpeed) {
		return calculateSpeedIfSpeedTag(maxSpeed, DEFAULT_FREESPEED_FACTOR);
	}

	/**
	 * For links with unknown max speed we assume that links with a length of less than 300m are urban links. For urban
	 * links with a length of 0m the speed is 10km/h. For links with a length of 300m the speed is the default freespeed
	 * property for that highway type. For links with a length between 0 and 300m the speed is interpolated linearly.
	 * (2.778m/s ~ 10km/h)
	 *
	 * All links longer than 300m the default freesped property is assumed
	 */
	public static double calculateSpeedIfNoSpeedTag(double linkLength, LinkProperties properties) {
		if (properties.hierarchyLevel > LinkProperties.LEVEL_MOTORWAY && properties.hierarchyLevel <= LinkProperties.LEVEL_TERTIARY
				&& linkLength <= 300) {
			return ((2.7778 + (properties.freespeed - 2.7778) / 300 * linkLength));
		}
		return properties.freespeed;
	}
}
