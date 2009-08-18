package playground.anhorni.Nelson;

public class Trip {	
	private String id;
	private Route[] routes = new Route[61];
	
	private int routeChoice;
	
	public Trip(String id) {
		this.id = id;
	}
	
	public void setRoute(int index, Route route) {
		this.routes[index] = route;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Route[] getRoutes() {
		return routes;
	}
	public void setRoutes(Route[] routes) {
		this.routes = routes;
	}
	public int getRouteChoice() {
		return routeChoice;
	}
	public void setRouteChoice(int routeChoice) {
		this.routeChoice = routeChoice;
	}
}
