package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.matrices.Entry;
import playground.meisterk.kti.router.SwissHaltestelle;
import org.matsim.api.core.v01.BasicLocation;

public class FtPtRoute extends GenericRouteImpl
{
  public static final char SEPARATOR = 61;
  public static final String IDENTIFIER = "kti";
  public static final double CROW_FLY_FACTOR = 1.5D;
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo;
  private SwissHaltestelle fromStop;
  private BasicLocation fromMunicipality;
  private BasicLocation toMunicipality;
  private SwissHaltestelle toStop;
  private Double inVehicleTime;

  public FtPtRoute(Id startLinkId, Id endLinkId, PlansCalcRouteFtInfo plansCalcRouteFtInfo)
  {
    super(startLinkId, endLinkId);

    this.fromStop = null;
    this.fromMunicipality = null;
    this.toMunicipality = null;
    this.toStop = null;
    this.inVehicleTime = null;

    this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
  }

  public FtPtRoute(Id startLinkId, Id endLinkId, PlansCalcRouteFtInfo plansCalcRouteFtInfo, SwissHaltestelle fromStop, BasicLocation fromMunicipality, BasicLocation toMunicipality, SwissHaltestelle toStop)
  {
    this(startLinkId, endLinkId, plansCalcRouteFtInfo);
    this.fromStop = fromStop;
    this.fromMunicipality = fromMunicipality;
    this.toMunicipality = toMunicipality;
    this.toStop = toStop;
    this.inVehicleTime = Double.valueOf(calcInVehicleTime());
  }

  public String getRouteDescription()
  {
    if (this.fromStop == null) {
      return super.getRouteDescription();
    }
    String routeDescription = 
      "kti=" + 
      this.fromStop.getId() + '=' + 
      this.fromMunicipality.getId() + '=' + 
      Double.toString(this.inVehicleTime.doubleValue()) + '=' + 
      this.toMunicipality.getId() + '=' + 
      this.toStop.getId();

    return routeDescription;
  }

  public void setRouteDescription(Id startLinkId, String routeDescription, Id endLinkId)
  {
    super.setRouteDescription(startLinkId, routeDescription, endLinkId);
  }

  public double calcInVehicleDistance()
  {
    return (CoordUtils.calcDistance(getFromStop().getCoord(), getToStop().getCoord()) * 1.5D);
  }

  protected double calcInVehicleTime()
  {
    Entry matrixEntry = this.plansCalcRouteFtInfo.getPtTravelTimes().getEntry(this.fromMunicipality.getId(), this.toMunicipality.getId());
    if (matrixEntry == null) {
      throw new RuntimeException("No entry found for " + this.fromMunicipality.getId() + " --> " + this.toMunicipality.getId());
    }

    double travelTime = matrixEntry.getValue() * 60.0D;

    if (Double.isNaN(travelTime)) {
      travelTime = calcInVehicleDistance() / this.plansCalcRouteFtInfo.getFtConfigGroup().getIntrazonalPtSpeed();
    }

    return travelTime;
  }

  public double calcAccessEgressDistance(ActivityImpl fromAct, ActivityImpl toAct)
  {
    return 
      ((CoordUtils.calcDistance(fromAct.getCoord(), getFromStop().getCoord()) + 
      CoordUtils.calcDistance(toAct.getCoord(), getToStop().getCoord())) * 
      1.5D);
  }

  public SwissHaltestelle getFromStop()
  {
    return this.fromStop;
  }

  public BasicLocation getFromMunicipality() {
    return this.fromMunicipality;
  }

  public BasicLocation getToMunicipality() {
    return this.toMunicipality;
  }

  public SwissHaltestelle getToStop() {
    return this.toStop;
  }

  public Double getInVehicleTime() {
    return this.inVehicleTime;
  }
}