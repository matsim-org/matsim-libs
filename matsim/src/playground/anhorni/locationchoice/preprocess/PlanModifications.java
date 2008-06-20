package playground.anhorni.locationchoice.preprocess;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;

public class PlanModifications {

	private Plans plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	private String outputpath="";
	private Modifier modifier=null;

	private final static Logger log = Logger.getLogger(PlanModifications.class);

	/**
	 * @param:
	 * - path to plans file
	 * - path to network file
	 * - path to facilities file
	 */
	public static void main(final String[] args) {

		/*
		if (args.length < 3 || args.length > 3 ) {
			System.out.println("Too few arguments. Exit");
			System.exit(1);
		}

		String plansfilePath=args[0];
		String networkfilePath=args[1];
		String facilitiesfilePath=args[2];
		*/

		String plansfilePath="./input/plans.xml.gz";
		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";


		PlanModifications plansModifier=new PlanModifications();
		LocationModifier locationmodifier=new LocationModifier();
		plansModifier.init(plansfilePath, networkfilePath, facilitiesfilePath, locationmodifier);
		plansModifier.runLocationModification();

		FacilitiesV3Modifier facilitiesV3Modifier=new FacilitiesV3Modifier();
		plansModifier.init(plansfilePath, networkfilePath, facilitiesfilePath, facilitiesV3Modifier);
		plansModifier.runAssignFacilitiesV3();

	}


	public void runLocationModification() {

			this.outputpath="./output/plans_randomized.xml.gz";
			this.modifier.modify(0);
			this.writePlans();

			this.outputpath="./output/plans_oneloc.xml.gz";
			this.modifier.modify(1);
			this.writePlans();

			this.outputpath="./output/plans_onelocinarea.xml.gz";
			this.modifier.modify(2);
			this.writePlans();
	}

	public void runAssignFacilitiesV3() {
		this.outputpath="./output/plans_facilitiesV3.xml.gz";
		this.modifier.modify(0);
		this.writePlans();
	}


	public void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, Modifier modifier) {

		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		this.facilities=new Facilities();
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");


		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");

		this.modifier=modifier;
		this.modifier.init(this.plans, this.network, this.facilities);
		log.info("init modifier done");
	}

	private void writePlans() {
		new PlansWriter(this.plans, this.outputpath , "v4", 1.0).write();
		log.info("plans written to: " + this.outputpath);
	}




}
