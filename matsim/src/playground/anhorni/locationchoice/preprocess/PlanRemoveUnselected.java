package playground.anhorni.locationchoice.preprocess;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Person;
import org.matsim.population.Plans;
import org.matsim.population.PlansReaderI;
import org.matsim.population.PlansWriter;
import org.matsim.utils.misc.Counter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class PlanRemoveUnselected {

	private Plans plans=null;
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
		Gbl.createWorld();
		//final MatsimWorldReader worldReader = new MatsimWorldReader(this.world);
		//worldReader.readFile(worldfilePath);
		System.out.println("  done.");


		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		Gbl.getWorld().complete();
		new WorldCheck().run(Gbl.getWorld());
		new WorldBottom2TopCompletion().run(Gbl.getWorld());
		new WorldValidation().run(Gbl.getWorld());
		new WorldCheck().run(Gbl.getWorld());
		log.info("world checking done.");


		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
		log.info(this.plans.getPersons().size() + " persons");
	}

	private void writePlans() {
		new PlansWriter(this.plans, this.outputpath , "v4", 1.0).write();
		log.info("plans written to: " + this.outputpath);
	}

}
