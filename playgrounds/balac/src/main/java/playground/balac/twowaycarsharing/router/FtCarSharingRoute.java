package playground.balac.twowaycarsharing.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class FtCarSharingRoute extends GenericRouteImpl
{
  public static final char SEPARATOR = 61;
  public static final String IDENTIFIER = "ft";
  public static final double CROW_FLY_FACTOR = 1.5D;
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo;
  private CarSharingStations carStations;
  private CarSharingStation toStation;
  private CarSharingStation fromStation;
  private double carTime;

  public FtCarSharingRoute(Id startLinkId, Id endLinkId, PlansCalcRouteFtInfo plansCalcRouteFtInfo)
  {
    super(startLinkId, endLinkId);
    this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
    this.carStations = plansCalcRouteFtInfo.getCarStations();
  }

  public FtCarSharingRoute(Id startLinkId, Id endLinkId, PlansCalcRouteFtInfo plansCalcRouteFtInfo, CarSharingStation fromStation, CarSharingStation toStation)
  {
    super(startLinkId, endLinkId);
    this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
    this.carStations = plansCalcRouteFtInfo.getCarStations();
    this.toStation = toStation;
    this.fromStation = fromStation;
  }

  public String getRouteDescription()
  {
    if (this.toStation == null) {
      return super.getRouteDescription();
    }
    String routeDescription = 
      "ft=" + 
      this.toStation.getId() + '=' + 
      Double.toString(this.carTime) + '=' + 
      this.fromStation.getId();

    return routeDescription;
  }

  public void setRouteDescription(Id startLinkId, String routeDescription, Id endLinkId)
  {
    super.setRouteDescription(startLinkId, routeDescription, endLinkId);
  }

  public double calcCarDistance(Coord coord)
  {
    return CoordUtils.calcDistance(getFromStation().getCoord(), coord);
  }

  public double calcAccessDistance(Coord coord) {
   
    Coord stationCoord = this.fromStation.getCoord();
    return 
      (CoordUtils.calcDistance(stationCoord, coord) * 1.5D);
  }

  public double calcEgressDistance(Coord coord)
  {
    return 
      (CoordUtils.calcDistance(coord, getToStation().getCoord()) * 1.5D);
  }

  public CarSharingStation getFromStation()
  {
    return this.fromStation;
  }

  public CarSharingStation getToStation() {
    return this.toStation;
  }

  public Double getInVehicleTime() {
    return Double.valueOf(this.carTime);
  }
}
