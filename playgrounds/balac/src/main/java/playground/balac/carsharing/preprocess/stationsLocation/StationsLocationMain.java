package playground.balac.carsharing.preprocess.stationsLocation;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import playground.balac.carsharing.preprocess.membership.MembershipMain;
import playground.balac.carsharing.preprocess.membership.MembershipModel;
import playground.balac.carsharing.router.CarSharingStations;

public class StationsLocationMain
{
  private final MutableScenario scenario = (MutableScenario)ScenarioUtils.createScenario(ConfigUtils.createConfig());
  private final Population plans = this.scenario.getPopulation();
  private final Network network = this.scenario.getNetwork();
  private String plansfilePath;
  //private final String outpath = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/CarsharingStationLocations/";
  private String facilitiesfilePath;
  private String networkfilePath;
  private CarSharingStations carStations;
  private String stationsfilePath;
  private static final Logger log = Logger.getLogger(MembershipMain.class);

  public static void main(String[] args) throws IOException {
    Gbl.startMeasurement();
    StationsLocationMain stationsLocations = new StationsLocationMain();
    stationsLocations.run(args[0]);
    Gbl.printElapsedTime();
  }

  public void run(String inputFile) throws IOException {
    init(inputFile);
    log.info("TEST");
    CarSharingPlanner csPlanner = new CarSharingPlanner(this.scenario, new MembershipModel(), this.carStations);
    csPlanner.init();
    csPlanner.runStrategy();

    writePlans();
    
    
    csPlanner.writeCSSummary(this.carStations);
  }

  private void readInputFile(String pathsFile) {
    try {
      FileReader fileReader = new FileReader(pathsFile);
      BufferedReader bufferedReader = new BufferedReader(fileReader);

      this.networkfilePath = bufferedReader.readLine();
      this.facilitiesfilePath = bufferedReader.readLine();
      this.plansfilePath = bufferedReader.readLine();
      this.stationsfilePath = bufferedReader.readLine();
      bufferedReader.close();
      fileReader.close();
    }
    catch (IOException e) {
    }
  }

  private void init(String inputFile) {
    String pathsFile = inputFile;
    readInputFile(pathsFile);

    log.info("reading the facilities ...");
    new FacilitiesReaderMatsimV1(this.scenario).readFile(this.facilitiesfilePath);

    log.info("reading the network ...");
    new MatsimNetworkReader(this.scenario).readFile(this.networkfilePath);

    log.info("  reading file " + this.plansfilePath);
    PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
    plansReader.readFile(this.plansfilePath);

    log.info("Reading car stations...");
    this.carStations = new CarSharingStations(this.scenario.getNetwork());
    try {
      this.carStations.readFile(this.stationsfilePath);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("car stations = " + this.carStations);
    log.info("Reading car stations...done.");
  }

  private void writePlans()
  {
    new PopulationWriter(this.plans, this.network).write("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/CarsharingStationLocations/" + "plansCarSharing_13_new.xml");
  }
}