package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.matrices.Entry;
import org.matsim.world.Location;

import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class CarSharingRouter extends GenericRouteImpl {
	public static final char SEPARATOR = '=';
	public static final String IDENTIFIER = "kti";

	public static final double CROW_FLY_FACTOR = 1.5;

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private CarSharingStation fromStop = null;
	private Location fromMunicipality = null;
	private Location toMunicipality = null;
	private CarSharingStation toStop = null;
	private Double inVehicleTime = null;

	public CarSharingRouter(Id startLinkId, Id endLinkId, PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super(startLinkId, endLinkId);
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	public CarSharingRouter(
			Id startLinkId,
			Id endLinkId,
			PlansCalcRouteKtiInfo plansCalcRouteKtiInfo,
			CarSharingStation fromStop,
			Location fromMunicipality,
			Location toMunicipality,
			CarSharingStation toStop) {
		this(startLinkId, endLinkId, plansCalcRouteKtiInfo);
		this.fromStop = fromStop;
		this.fromMunicipality = fromMunicipality;
		this.toMunicipality = toMunicipality;
		this.toStop = toStop;
		this.inVehicleTime = this.calcInVehicleTime();
	}

	@Override
	public String getRouteDescription() {

		if (this.fromStop == null) {
			return super.getRouteDescription();
		}
		String routeDescription =
			IDENTIFIER + SEPARATOR +
			this.fromStop.getId() + SEPARATOR +
			this.fromMunicipality.getId() + SEPARATOR +
			Double.toString(this.inVehicleTime) + SEPARATOR +
			this.toMunicipality.getId() + SEPARATOR +
			this.toStop.getId();

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

	public double calcInVehicleDistance() { //TODO this part should be copied from the router for the car mode
		return CoordUtils.calcDistance(this.getFromStop().getCoord(), this.getToStop().getCoord()) * CROW_FLY_FACTOR;
	}

	protected double calcInVehicleTime() {

		Entry matrixEntry = this.plansCalcRouteKtiInfo.getPtTravelTimes().getEntry(this.fromMunicipality.getId(), this.toMunicipality.getId());
		if (matrixEntry == null) {
			throw new RuntimeException("No entry found for " + this.fromMunicipality.getId() + " --> " + this.toMunicipality.getId());
		}

		double travelTime = matrixEntry.getValue() * 60.0;
		/*
		 * A value of NaN in the travel time matrix indicates that the matrix contains no valid value for this entry.
		 * In this case, the travel time is calculated with the distance of the relation and an average speed.
		 */
		if (Double.isNaN(travelTime)) {
			travelTime = this.calcInVehicleDistance() / this.plansCalcRouteKtiInfo.getKtiConfigGroup().getIntrazonalPtSpeed();
		}

		return travelTime;

	}
	//TODO modify this part, at the moment it is just copied from the pt mode. In a first, simpler, representation it should have only an access time, and
	// not an egress time too. In the hypothesis of a "one way" car sharing option, the egress time should be kept.
	public double calcAccessEgressDistance(final ActivityImpl fromAct, final ActivityImpl toAct) {

		return
		(CoordUtils.calcDistance(fromAct.getCoord(), this.getFromStop().getCoord()) +
		CoordUtils.calcDistance(toAct.getCoord(), this.getToStop().getCoord()))
		* CROW_FLY_FACTOR;

	}

	public CarSharingStation getFromStop() {
		return fromStop;
	}

	public Location getFromMunicipality() {
		return fromMunicipality;
	}

	public Location getToMunicipality() {
		return toMunicipality;
	}

	public CarSharingStation getToStop() {
		return toStop;
	}

	public Double getInVehicleTime() {
		return this.inVehicleTime;
	}

}
