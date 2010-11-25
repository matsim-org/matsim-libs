package playground.ciarif.flexibletransports.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

public class FtConfigGroup extends Module
{
  public static final String GROUP_NAME = "ft";
  private static final Logger logger = Logger.getLogger(FtConfigGroup.class);

  public FtConfigGroup()
  {
    super("ft");
    for (FtConfigParameter param : FtConfigParameter.values()) {
      param.setActualValue(param.getDefaultValue());
      super.addParam(param.getParameterName(), param.getDefaultValue());
    }
  }

  public void addParam(String param_name, String value)
  {
    boolean validParameterName = false;

    for (FtConfigParameter param : FtConfigParameter.values())
    {
      if (param.getParameterName().equals(param_name)) {
        param.setActualValue(value);
        super.addParam(param_name, value);
        validParameterName = true;
      }

    }

    if (!(validParameterName))
      logger.warn("Unknown parameter name in module planomat: \"" + param_name + "\". It is ignored.");
  }

  public double getConstBike()
  {
    return Double.parseDouble(FtConfigParameter.CONST_BIKE.getActualValue());
  }

  public String getCarSharingStationsFilename()
  {
    return FtConfigParameter.CS_STATIONS_FILENAME.getActualValue();
  }

  public void setCarSharingStationsFilename(String ptHaltestellenFilename) {
    FtConfigParameter.PT_HALTESTELLEN_FILENAME.setActualValue(ptHaltestellenFilename);
  }

  public String getPtHaltestellenFilename() {
    return FtConfigParameter.PT_HALTESTELLEN_FILENAME.getActualValue();
  }

  public void setPtHaltestellenFilename(String ptHaltestellenFilename) {
    FtConfigParameter.PT_HALTESTELLEN_FILENAME.setActualValue(ptHaltestellenFilename);
  }

  public String getPtTraveltimeMatrixFilename() {
    return FtConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.getActualValue();
  }

  public void setPtTraveltimeMatrixFilename(String ptTraveltimeMatrixFilename) {
    FtConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.setActualValue(ptTraveltimeMatrixFilename);
  }

  public String getWorldInputFilename() {
    return FtConfigParameter.WORLD_INPUT_FILENAME.getActualValue();
  }

  public void setWorldInputFilename(String worldInputFilename) {
    FtConfigParameter.WORLD_INPUT_FILENAME.setActualValue(worldInputFilename);
  }

  public boolean isUsePlansCalcRouteFT() {
    return Boolean.parseBoolean(FtConfigParameter.USE_PLANS_CALC_ROUTE_FT.getActualValue());
  }

  public void setUsePlansCalcRouteFT(boolean usePlansCalcRouteFT) {
    FtConfigParameter.USE_PLANS_CALC_ROUTE_FT.setActualValue(Boolean.toString(usePlansCalcRouteFT));
  }

  public boolean isUsePlansCalcRouteKti() {
    return Boolean.parseBoolean(FtConfigParameter.USE_PLANS_CALC_ROUTE_KTI.getActualValue());
  }

  public void setUsePlansCalcRouteKti(boolean usePlansCalcRouteKti) {
    FtConfigParameter.USE_PLANS_CALC_ROUTE_KTI.setActualValue(Boolean.toString(usePlansCalcRouteKti));
  }

  public void setDistanceCostCar(double newValue) {
    FtConfigParameter.DISTANCE_COST_CAR.setActualValue(Double.toString(newValue));
  }

  public double getDistanceCostCar() {
    return Double.parseDouble(FtConfigParameter.DISTANCE_COST_CAR.getActualValue());
  }

  public double getDistanceCostPtNoTravelCard() {
    return Double.parseDouble(FtConfigParameter.DISTANCE_COST_PT.getActualValue());
  }

  public void setDistanceCostPtNoTravelCard(double newValue) {
    FtConfigParameter.DISTANCE_COST_PT.setActualValue(Double.toString(newValue));
  }

  public double getDistanceCostPtUnknownTravelCard() {
    return Double.parseDouble(FtConfigParameter.DISTANCE_COST_PT_UNKNOWN.getActualValue());
  }

  public void setDistanceCostPtUnknownTravelCard(double newValue) {
    FtConfigParameter.DISTANCE_COST_PT_UNKNOWN.setActualValue(Double.toString(newValue));
  }

  public double getTravelingBike() {
    return Double.parseDouble(FtConfigParameter.TRAVELING_BIKE.getActualValue());
  }

  public void setTravelingBike(double newValue) {
    FtConfigParameter.TRAVELING_BIKE.setActualValue(Double.toString(newValue));
  }

  public double getConstCar() {
    return Double.parseDouble(FtConfigParameter.CONST_CAR.getActualValue());
  }

  public void setConstBike(double newValue) {
    FtConfigParameter.CONST_BIKE.setActualValue(Double.toString(newValue));
  }

  public void setConstCar(double newValue) {
    FtConfigParameter.CONST_CAR.setActualValue(Double.toString(newValue));
  }

  public double getTravelingRide() {
    return Double.parseDouble(FtConfigParameter.TRAVELING_RIDE.getActualValue());
  }

  public void setTravelingRide(double newValue) {
    FtConfigParameter.TRAVELING_RIDE.setActualValue(Double.toString(newValue)); }

  public void setConstRide(double newValue) {
    FtConfigParameter.CONST_RIDE.setActualValue(Double.toString(newValue));
  }

  public double getConstRide() {
    return Double.parseDouble(FtConfigParameter.CONST_RIDE.getActualValue());
  }

  public void setMarginalUtilityOfDistanceRide(double newValue) {
    FtConfigParameter.MARG_UTIL_DIST_RIDE.setActualValue(Double.toString(newValue));
  }

  public double getMarginalUtilityOfDistanceRide() {
    return Double.parseDouble(FtConfigParameter.MARG_UTIL_DIST_RIDE.getActualValue());
  }

  public void setDistanceCostRide(double newValue) {
    FtConfigParameter.DISTANCE_COST_RIDE.setActualValue(Double.toString(newValue)); }

  public double getDistanceCostRide() {
    return Double.parseDouble(FtConfigParameter.DISTANCE_COST_RIDE.getActualValue());
  }

  public void setConstPt(double newValue) {
    FtConfigParameter.CONST_PT.setActualValue(Double.toString(newValue));
  }

  public double getConstPt()
  {
    return Double.parseDouble(FtConfigParameter.CONST_PT.getActualValue());
  }

  public double getIntrazonalPtSpeed() {
    return Double.parseDouble(FtConfigParameter.INTRAZONAL_PT_SPEED.getActualValue());
  }

  public void setIntrazonalPtSpeed(double newValue) {
    FtConfigParameter.INTRAZONAL_PT_SPEED.setActualValue(Double.toString(newValue));
  }

  public static enum FtConfigParameter
  {
    CONST_BIKE, CS_STATIONS_FILENAME, PT_TRAVEL_TIME_MATRIX_FILENAME, PT_HALTESTELLEN_FILENAME, WORLD_INPUT_FILENAME, USE_PLANS_CALC_ROUTE_FT, USE_PLANS_CALC_ROUTE_KTI, DISTANCE_COST_CAR, DISTANCE_COST_PT, DISTANCE_COST_PT_UNKNOWN, TRAVELING_BIKE, CONST_CAR, TRAVELING_RIDE, CONST_RIDE, MARG_UTIL_DIST_RIDE, DISTANCE_COST_RIDE, CONST_PT, INTRAZONAL_PT_SPEED;

    private final String parameterName = null;
    private final String defaultValue = null;
    private String actualValue;

    public String getActualValue()
    {
      return this.actualValue;
    }

    public void setActualValue(String actualValue) {
      this.actualValue = actualValue;
    }

    public String getParameterName() {
      return this.parameterName;
    }

    public String getDefaultValue() {
      return this.defaultValue;
    }
  }
}