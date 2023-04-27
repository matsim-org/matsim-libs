package ch.sbb.matsim.contrib.railsim.prototype.supply;

/**
 * The possible link types in the network.
 *
 * @author Merlin Unterfinger
 */
enum LinkType {
	/**
	 * Links inside the stations.
	 */
	STOP("stp"),
	/**
	 * Links in the depot and connecting the depot.
	 */
	DEPOT("dpt"),
	/**
	 * Links on the route between two stations.
	 */
	ROUTE("rte");

	private final String abbreviation;

	LinkType(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public String getAbbreviation() {
		return abbreviation;
	}
}
