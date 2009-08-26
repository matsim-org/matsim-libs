package playground.wrashid.PSF.converter.addingParkings;

import java.util.List;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;

import playground.andreas.bln.NewPopulation;
import playground.wrashid.tryouts.plan.KeepOnlyMIVPlans;

/*
 * add parking to plans (leg + activitites)
 */
public class AddParkingsToPlans extends AbstractPersonAlgorithm {

	protected PopulationWriter popWriter;
	
	public AddParkingsToPlans(PopulationImpl inPop, String outputPlansFile) {
		this.popWriter = new PopulationWriter(inPop, outputPlansFile, "v4");
		this.popWriter.writeStartPlans();
	}

	public static void main(String[] args) {
		String inputPlansFile = "test/input/playground/wrashid/PSF/converter/addParkings/plans2.xml";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		String outputPlansFile = "output/plans4.xml";
		
		String outputFacilitiesFile = "output/facilities4.xml";

		// generate facilities file, this is needed by the config file...
		//GenerateParkingFacilities.generateParkingFacilties(inputPlansFile,networkFile,outputFacilitiesFile);
		// generate plans
		generatePlanWithParkingActs(inputPlansFile,networkFile,outputPlansFile);
	}
	
	/**
	 * As input this method receives a parking (file?), without parkings acts
	 * (and related lets) and adds these.
	 */
	public static void generatePlanWithParkingActs(String inputPlansFile, String networkFile, String outputPlansFile) {
		
		String[] args=new String[1];
		args[0]="test/input/playground/wrashid/PSF/converter/addParkings/config4.xml";
		Config config = Gbl.createConfig(args);
		//World world = Gbl.createWorld();
		
		PopulationImpl inPop = new PopulationImpl();
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);
		
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inputPlansFile);
		
		AddParkingsToPlans dp = new AddParkingsToPlans(inPop, outputPlansFile);
		dp.run(inPop);
		dp.popWriter.writeEndPlans();
	}

	@Override
	public void run(PersonImpl person) {
		//person.getSelectedPlan().
	
		Plan plan=person.getSelectedPlan();
		
		List<PlanElement> pe=plan.getPlanElements();
		
		// CONTINUE HERE
		/*
		for (int i=0;){
			
		}pe.
		*/
		
		this.popWriter.writePerson(person);
	}
	
	

}
