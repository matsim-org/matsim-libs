package playground.mmoyo.utils;

import java.io.File;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import playground.mmoyo.algorithms.PlanScoreNullifier;

/**Select plans according to a given index*/
public class PlanSelectedChanger extends AbstractPersonAlgorithm{
	int planSelectedIndex;
	
	public PlanSelectedChanger(){
		
	}
	
	public PlanSelectedChanger(int planSelectedIndex){
		this.planSelectedIndex = planSelectedIndex;
	}
	
	@Override
	public void run(Person person){
		if (person.getPlans().size()>=planSelectedIndex){
			((PersonImpl)person).setSelectedPlan( person.getPlans().get(planSelectedIndex) );
		}
	}
	
	public static void main(String[] args) {
		String plansFile = "../mmoyo/output/taste/inputPlans/500.plans_fromCalibM44.xml.gz";
		String netFilePath =   "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		DataLoader dataLoader = new DataLoader ();
		Population pop = dataLoader.readPopulation(plansFile);

		//change selected plan
		new PlanSelectedChanger(3).run(pop);
		
		//nullify scores
		new PlanScoreNullifier().run(pop);
		
		//write population
		System.out.println("writing output plan file...");
		NetworkImpl net = (NetworkImpl)dataLoader.readNetwork(netFilePath);
		PopulationWriter popwriter = new PopulationWriter(pop, net);
		File file = new File(plansFile);
		popwriter.write(file.getParent() + "/" + file.getName() + "PlanSelectedChanged.xml.gz") ;
		System.out.println("done");
	}

}
