package org.matsim.contrib.carsharing.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;

public class CarsharingRoute extends AbstractRoute {
/*package*/ final static String ROUTE_TYPE = "carsharing";
	
	private String routeDescription = null;
	private boolean oldRoute = false;
	private boolean keepthecar = false;
	private String vehicleType = null;
	private String company = null;
	
	
	public CarsharingRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	@Override
	public String getRouteDescription() {
		return this.routeDescription + " keepthecar=" + keepthecar + " company=" + company;
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		this.routeDescription = routeDescription;
		
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	public boolean isKeepthecar() {
		return keepthecar;
	}

	public void setKeepthecar(boolean keepthecar) {
		this.oldRoute = true;
		this.keepthecar = keepthecar;
	}

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.oldRoute = true;

		this.vehicleType = vehicleType;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.oldRoute = true;

		this.company = company;
	}

	public boolean isOldRoute() {
		return oldRoute;
	}

}
