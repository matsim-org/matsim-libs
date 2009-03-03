package playground.anhorni.locationchoice.preprocess;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.misc.Counter;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldConnectLocations;
import org.matsim.world.algorithms.WorldMappingInfo;

public class PlanRemoveUnselected {

	private Population plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities =null;
	private String outputpath="";

	private final static Logger log = Logger.getLogger(PlanRemoveUnselected.class);

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

		PlanRemoveUnselected plansModifier=new PlanRemoveUnselected();
		plansModifier.init(plansfilePath, networkfilePath, facilitiesfilePath, worldfilePath);
		plansModifier.runModifications();
	}

	private void runModifications() {

		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();
				person.removeUnselectedPlans();
		}
		log.info("runModifications done.");

		this.outputpath="./output/plans_randomizedzhlocs_cleaned.xml.gz";
		this.writePlans();
	}



	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, final String worldfilePath) {


		System.out.println("  create world ... ");
		World world = Gbl.createWorld();
		//final MatsimWorldReader worldReader = new MatsimWorldReader(this.world);
		//worldReader.readFile(worldfilePath);
		System.out.println("  done.");


		this.network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities=(Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		world.complete();
		new WorldCheck().run(world);
		new WorldConnectLocations().run(world);
		new WorldMappingInfo().run(world);
		new WorldCheck().run(world);
		log.info("world checking done.");


		this.plans=new PopulationImpl(false);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans, this.network);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
		log.info(this.plans.getPersons().size() + " persons");
	}

	private void writePlans() {
		new PopulationWriter(this.plans, this.outputpath , "v4", 1.0).write();
		log.info("plans written to: " + this.outputpath);
	}

}
