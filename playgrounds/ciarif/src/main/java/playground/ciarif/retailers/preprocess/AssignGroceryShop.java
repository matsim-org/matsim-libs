package playground.ciarif.retailers.preprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import playground.ciarif.retailers.utils.ActivityDifferentiator;

public class AssignGroceryShop {
	private final ScenarioImpl scenario = new ScenarioImpl();
	private final Population plans = scenario.getPopulation();
	private final ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
	private final NetworkImpl network = scenario.getNetwork();

	private String plansfilePath;
	private String facilitiesfilePath;
	private String networkfilePath;
	//private final String outpath = "../../matsim/output/preprocess/";
	private final String outpath = "/data/matsim/ciarif/output/preprocess/";
	
	private final static Logger log = Logger.getLogger(AssignGroceryShop.class);

	public static void main(String [] args) {
		Gbl.startMeasurement();
		final AssignGroceryShop assigner = new AssignGroceryShop();
		assigner.run(args [0]);
		Gbl.printElapsedTime();
	}

	public void run(String inputFile) {
		this.init(inputFile);
		ActivityDifferentiator differentiator = new ActivityDifferentiator(this.scenario);
		differentiator.run();
		this.writePlans();
		this.writeFacilities();
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
		
		new PopulationWriter(this.plans, this.network).write(this.outpath + "plans0.xml");
	}
	
	private void writeFacilities () {
		new FacilitiesWriter(this.facilities).write(this.outpath + "facilities0.xml");
	}

}
