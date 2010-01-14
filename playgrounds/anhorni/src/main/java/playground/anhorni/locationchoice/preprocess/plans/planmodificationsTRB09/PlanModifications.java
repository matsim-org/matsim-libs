package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldConnectLocations;
import org.matsim.world.algorithms.WorldMappingInfo;

public class PlanModifications {

	private PopulationImpl plans=null;
	private NetworkLayer network=null;
	private ActivityFacilitiesImpl  facilities =null;
	private String outputpath="";
	private Modifier modifier=null;

	private final static Logger log = Logger.getLogger(PlanModifications.class);

	/**
	 * @param:
	 * - path to plans file
	 */
	public static void main(final String[] args) {


		if (args.length < 1 || args.length > 1 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath=args[0];

		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";
		String worldfilePath="./input/world.xml";

		PlanModifications plansModifier=new PlanModifications();
		plansModifier.init(plansfilePath, networkfilePath, facilitiesfilePath, worldfilePath);
		plansModifier.runModifications();
	}

	private void runModifications() {

		// remove border crossing traffic
		this.setModifier(new RemoveBorderCrossingTraffic(this.plans, this.network, this.facilities));
		this.modifier.modify();

		// use facilities v3
		this.setModifier(new FacilitiesV3Modifier(this.plans, this.network, this.facilities));
		this.runAssignFacilitiesV3();

		// modify the activity locations
		this.setModifier(new LocationModifier(this.plans, this.network, this.facilities));
		this.runLocationModification();

	}

	private void setModifier(final Modifier modifier) {
		this.modifier=modifier;
	}

	private void runAssignFacilitiesV3() {
		this.outputpath="./output/plans_facilitiesV3.xml.gz";
		this.modifier.modify();
		this.writePlans();
	}

	private void runLocationModification() {
			this.outputpath="./output/plans_randomizedzhlocs.xml.gz";
			this.modifier.modify();
			this.writePlans();
	}

	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, final String worldfilePath) {

		ScenarioImpl scenario = new ScenarioImpl();

		System.out.println("  create world ... ");
		World world = scenario.getWorld();
		//final MatsimWorldReader worldReader = new MatsimWorldReader(this.world);
		//worldReader.readFile(worldfilePath);
		System.out.println("  done.");


		this.network = scenario.getNetwork();
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities = scenario.getActivityFacilities();
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		world.complete();
		new WorldCheck().run(world);
		new WorldConnectLocations().run(world);
		new WorldMappingInfo().run(world);
		new WorldCheck().run(world);
		log.info("world checking done.");


		this.plans=scenario.getPopulation();
		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
		log.info(this.plans.getPersons().size() + " persons");
	}

	private void writePlans() {
		new PopulationWriter(this.plans, this.network).writeFile(this.outputpath);
		log.info("plans written to: " + this.outputpath);
	}

}
