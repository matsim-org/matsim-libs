package playground.ciarif.flexibletransports.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class MembershipMain {
	
		private final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		private final Population plans = scenario.getPopulation();
		private final ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		private final NetworkImpl network = scenario.getNetwork();
		private String plansfilePath;
		private final String outpath = "../../matsim/output/preprocess/";
		//private final String outpath = "/data/matsim/ciarif/output/preprocess/";
		private String facilitiesfilePath;
		private String networkfilePath;
		
		private final static Logger log = Logger.getLogger(MembershipMain.class);

		public static void main(String [] args) {
			Gbl.startMeasurement();
			final MembershipMain membershipAssigner = new MembershipMain();
			membershipAssigner.run(args [0]);
			Gbl.printElapsedTime();
		}

		public void run(String inputFile) {
			this.init(inputFile);
			MembershipModel membershipModel = new MembershipModel(this.scenario);
			membershipModel.run();
			this.writePlans();
		}

		private void readInputFile(final String pathsFile) {
			try {
				FileReader fileReader = new FileReader(pathsFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				this.networkfilePath = bufferedReader.readLine();
				this.facilitiesfilePath = bufferedReader.readLine();
				this.plansfilePath = bufferedReader.readLine();

				bufferedReader.close();
				fileReader.close();

			} catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}

		private void init(String inputFile) {
			String pathsFile = inputFile;
			this.readInputFile(pathsFile);
			
			log.info("reading the facilities ...");
			new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesfilePath);

			log.info("reading the network ...");
			new MatsimNetworkReader(this.scenario).readFile(networkfilePath);
			
			log.info("  reading file " + plansfilePath);
			final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
			plansReader.readFile(plansfilePath);
		}

		private void writePlans() {
			
			new PopulationWriter(this.plans, this.network).write(this.outpath + "plansCarSharing.xml");
		}
}
