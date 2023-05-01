package ch.sbb.matsim.contrib.railsim.prototype.supply;

/**
 * Route direction
 * <p>
 * The two possible direction of the routes.
 *
 * @author Merlin Unterfinger
 */
public enum RouteDirection {
	/**
	 * The route starts at the origin and ends at the destination.
	 */
	FORWARD("F"),
	/**
	 * The route starts at the destination and ends at the origin.
	 */
	REVERSE("R");

	private final String abbreviation;

	RouteDirection(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

}
