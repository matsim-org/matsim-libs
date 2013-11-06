package playground.balac.onewaycarsharing.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class FtCarSharingRoute extends GenericRouteImpl
{
  public static final char SEPARATOR = 61;
  public static final String IDENTIFIER = "ft";
  public static final double CROW_FLY_FACTOR = 1.3D;
  private CarSharingStation toStation;
  private CarSharingStation fromStation;
  private double carTime;

  public FtCarSharingRoute(Id startLinkId, Id endLinkId )
  {
    super(startLinkId, endLinkId);
  }

  public FtCarSharingRoute(Id startLinkId, Id endLinkId, CarSharingStation fromStation, CarSharingStation toStation)
  {
    super(startLinkId, endLinkId);
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

  public double calcAccessDistance(Coord coord) {
   
    Coord stationCoord = this.fromStation.getCoord();
    return 
      (CoordUtils.calcDistance(stationCoord, coord) * CROW_FLY_FACTOR);
  }

  public double calcEgressDistance(Coord coord)
  {
    return 
      (CoordUtils.calcDistance(coord, getToStation().getCoord()) * CROW_FLY_FACTOR);
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
