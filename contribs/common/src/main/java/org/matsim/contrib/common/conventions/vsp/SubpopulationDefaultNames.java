package org.matsim.contrib.common.conventions.vsp;

public class SubpopulationDefaultNames{
	/**
	 * Is used for the small-scale commercial person traffic trips.
	 */
	public static final String SUBPOP_COM_PERSON = "commercialPersonTraffic";
	/**
	 * Is used for the service trips of the small-scale commercial person traffic. The assumption is that for this subpopulation the usage of pt could be possible, because no material has to be carried.
	 */
	public static final String SUBPOP_COM_PERSON_SERVICE = "commercialPersonTraffic_service";
	/**
	 * Is used for the goods traffic trips of the small-scale commercial traffic.
	 */
	public static final String SUBPOP_GOODS = "goodsTraffic";
	/**
	 * Is used for the long-distance freight trips.
	 */
	public static final String SUBPOP_LONG_DISTANCE_FREIGHT = "longDistanceFreight";
	public static final String SUBPOP_PERSON = "person";

	/**
	 * @deprecated -- this was used early, but should not be used in newer VSP matsim scenarios.  kai, apr/26, with input from RE and KMT
	 */
	@Deprecated
	public static final String SUBPOP_FREIGHT = "freight";
}
