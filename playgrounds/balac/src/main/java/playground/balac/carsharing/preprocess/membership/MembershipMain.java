package playground.balac.carsharing.preprocess.membership;

import java.io.BufferedReader;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class MembershipMain
{
  private final ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
  private final Population plans = this.scenario.getPopulation();
  private final ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) this.scenario.getActivityFacilities();
  private final Network network = this.scenario.getNetwork();
  private String plansfilePath;
  private String facilitiesfilePath;
  private String networkfilePath;
  private static final Logger log = Logger.getLogger(MembershipMain.class);

  public static void main(String[] args) {
    Gbl.startMeasurement();
    MembershipMain membershipMain = new MembershipMain();
    membershipMain.run(args[0]);
    Gbl.printElapsedTime();

  }

  public void run(String inputFile) {
    init(inputFile);
    MembershipAssigner membershipAssigner = new MembershipAssigner(this.scenario);
    membershipAssigner.run();
    writePlans();
  }

  private void readInputFile(String pathsFile) {
    try {
      FileReader fileReader = new FileReader(pathsFile);
      BufferedReader bufferedReader = new BufferedReader(fileReader);

      this.networkfilePath = bufferedReader.readLine();
      this.facilitiesfilePath = bufferedReader.readLine();
      this.plansfilePath = bufferedReader.readLine();
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
  }

  private void writePlans()
  {
	  //new PopulationWriter(this.plans, this.network).writeFileV4("C:/Users/balacm/Desktop/" + "plansCarSharing_greaterzurich_2x_cars.xml.gz");
	 new PopulationWriter(this.plans, this.network).writeFileV4("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/FreeFloatingTransportation2014/" + "plansCarSharing_25%_cars.xml.gz");
	  
  }
}