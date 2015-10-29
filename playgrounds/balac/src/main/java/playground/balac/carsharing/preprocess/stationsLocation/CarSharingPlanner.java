package playground.balac.carsharing.preprocess.stationsLocation;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.MutableScenario;

import playground.balac.carsharing.preprocess.membership.MembershipMain;
import playground.balac.carsharing.preprocess.membership.SupplySideModel;
import playground.balac.carsharing.preprocess.membership.IO.CarSharingStationsSummaryWriter;
import playground.balac.carsharing.preprocess.membership.strategies.CSmembershipMaximizationStrategy;
import playground.balac.carsharing.preprocess.membership.strategies.LocationStrategy;
import playground.balac.carsharing.router.CarSharingStations;

public class CarSharingPlanner
  implements LocationPlanner
{
  private LocationStrategy locationStrategy;
  private CarSharingStations csStations;
  private SupplySideModel model;
  private MutableScenario scenario;
  //private CarSharingStationsSummaryWriter cSSSWriter = new CarSharingStationsSummaryWriter("C:/Users/balacm/Desktop/CarSharing/CS_StationsSummary.txt");

  private CarSharingStationsSummaryWriter cSSSWriter = new CarSharingStationsSummaryWriter("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/CarsharingStationLocations/CS_StationsSummary_13_new.txt");
  private static final Logger log = Logger.getLogger(MembershipMain.class);

  public CarSharingPlanner(MutableScenario scenario, SupplySideModel model, CarSharingStations csStations) {
    this.scenario = scenario;
    this.model = model;
    this.csStations = csStations;
  }

  public void runStrategy() throws IOException
  {
    ArrayList<Integer> array = this.locationStrategy.findOptimalLocations(this.csStations, this.model);
  }

  public void writeCSSummary(CarSharingStations carStations) {
    this.cSSSWriter.write(this.csStations);
  }

  public void init()
  {
    this.locationStrategy = new CSmembershipMaximizationStrategy(this.scenario);
    this.cSSSWriter.write(this.csStations);
  }
}