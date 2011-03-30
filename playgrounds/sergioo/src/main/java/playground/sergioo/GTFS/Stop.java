package playground.sergioo.GTFS;

import org.matsim.api.core.v01.Coord;

public class Stop {
	
	//Attributes
	/**
	 * The location
	 */
	private Coord point;
	/**
	 * The link Id
	 */
	private String linkId;
	/**
	 * The name
	 */
	private String name;
	/**
	 * If blocks other vehicles
	 */
	private boolean blocks;
	/**
	 * The route type
	 */
	private Route.RouteTypes routeType;
	
	//Methods
	/**
	 * @param point
	 * @param name
	 * @param blocks
	 */
	public Stop(Coord point, String name, boolean blocks) {
		super();
		this.point = point;
		this.name = name;
		this.blocks = blocks;
	}
	/**
	 * @return the linkId
	 */
	public String getLinkId() {
		return linkId;
	}
	/**
	 * @param linkId the linkId to set
	 */
	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}
	/**
	 * @return the point
	 */
	public Coord getPoint() {
		return point;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the blocks
	 */
	public boolean isBlocks() {
		return blocks;
	}
	/**
	 * @return the routeType
	 */
	public Route.RouteTypes getRouteType() {
		return routeType;
	}
	/**
	 * @param routeType the routeType to set
	 */
	public void setRouteType(Route.RouteTypes routeType) {
		this.routeType = routeType;
	}
	
}
