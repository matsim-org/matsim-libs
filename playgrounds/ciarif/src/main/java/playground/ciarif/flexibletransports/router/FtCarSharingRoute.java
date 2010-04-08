package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.matrices.Entry;
import org.matsim.world.Location;

import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;

public class FtCarSharingRoute extends KtiPtRoute{

	private CarSharingStations carStations;
	private CarSharingStation toStation;
	private CarSharingStation fromStation;
	private double carTime;

	public FtCarSharingRoute(Id startLinkId, Id endLinkId,PlansCalcRouteKtiInfo plansCalcRouteKtiInfo, CarSharingStations carStations) {
		super(startLinkId, endLinkId, plansCalcRouteKtiInfo);
		this.carStations = carStations;
	}
	@Override
	public String getRouteDescription() { //TODO: Check if this is necessary and modify it

		if (this.toStation == null) {
			return super.getRouteDescription();
		}
		String routeDescription =
			IDENTIFIER + SEPARATOR +
			this.toStation.getId() + SEPARATOR +
			Double.toString(this.carTime) + SEPARATOR +
			this.fromStation.getId();

		return routeDescription;

	}

	@Override
	public void setRouteDescription(
			Id startLinkId,
			String routeDescription,
			Id endLinkId) {

		super.setRouteDescription(startLinkId, routeDescription, endLinkId);
//		if (routeDescription.startsWith(IDENTIFIER)) {
//			String[] routeDescriptionArray = StringUtils.explode(routeDescription, SEPARATOR);
//			this.fromStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[1]));
//			this.fromMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[2]));
//			this.ptMatrixInvehicleTime = Double.parseDouble(routeDescriptionArray[3]);
//			this.toMunicipality = plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality").getLocation(new IdImpl(routeDescriptionArray[4]));
//			this.toStop = plansCalcRouteKtiInfo.getHaltestellen().getHaltestelle(new IdImpl(routeDescriptionArray[5]));
//		} else {
//			this.fromStop = null;
//			this.fromMunicipality = null;
//			this.toMunicipality = null;
//			this.toStop = null;
//		}

	}

	public double calcCarDistance(ActivityImpl activityImpl, ActivityImpl activityImpl2) {
		return CoordUtils.calcDistance(this.getFromStation().getCoord(), activityImpl2.getCoord());
	}

	protected double calcCarTime() {
		
		double travelTime = 0;
		//TODO: compute travel time in the loaded network

		return travelTime;

	}

	public double calcAccessDistance(final ActivityImpl fromAct, final ActivityImpl toAct) {

		return
		(CoordUtils.calcDistance(fromAct.getCoord(), this.getFromStop().getCoord()) +
		CoordUtils.calcDistance(toAct.getCoord(), this.getToStop().getCoord()))
		* CROW_FLY_FACTOR;

	}

	public CarSharingStation getFromStation() {
		return this.fromStation;
	}

	public Double getInVehicleTime() {
		return this.carTime;
	}
}
