package playground.anhorni.locationchoice.preprocess;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldCheck;
import org.matsim.world.algorithms.WorldValidation;

public class PlanModifications {

	private Plans plans=null;
	private NetworkLayer network=null;
	private World world=null;
	private Facilities  facilities =null;
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

		// use facilities v3
		this.setModifier(new FacilitiesV3Modifier(this.plans, this.network, this.facilities));
		this.runAssignFacilitiesV3();

		// modify the activity locations
		this.setModifier(new LocationModifier(this.plans, this.network, this.facilities));
		this.runLocationModification();

	}

	private void setModifier(Modifier modifier) {
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


		System.out.println("  reading world xml file... ");
		this.world=new World();
		final MatsimWorldReader worldReader = new MatsimWorldReader(this.world);
		worldReader.readFile(worldfilePath);
		System.out.println("  done.");


		this.network = (NetworkLayer)this.world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities=(Facilities)this.world.createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		this.world.complete();
		new WorldCheck().run(this.world);
		new WorldBottom2TopCompletion().run(this.world);
		new WorldValidation().run(this.world);
		new WorldCheck().run(this.world);
		log.info("world checking done.");


		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
	}

	private void writePlans() {
		new PlansWriter(this.plans, this.outputpath , "v4", 1.0).write();
		log.info("plans written to: " + this.outputpath);
	}

}
