package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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

  public double calcCarDistance(ActivityImpl activityImpl2)
  {
    return CoordUtils.calcDistance(getFromStation().getCoord(), activityImpl2.getCoord());
  }

  public double calcAccessDistance(ActivityImpl fromAct) {
    Coord fromCoord = fromAct.getCoord();
    Coord stationCoord = this.fromStation.getCoord();
    return 
      (CoordUtils.calcDistance(stationCoord, fromCoord) * 1.5D);
  }

  public double calcEgressDistance(ActivityImpl toAct)
  {
    return 
      (CoordUtils.calcDistance(toAct.getCoord(), getToStation().getCoord()) * 1.5D);
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
