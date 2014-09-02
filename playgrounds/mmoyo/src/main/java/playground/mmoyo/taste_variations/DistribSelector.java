package playground.mmoyo.taste_variations;

import java.io.File;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.mmoyo.utils.DataLoader;

/** selects the plan with a predefined distribution according to plan index*/
public class DistribSelector implements PlanSelector{
	
	public DistribSelector(){
		MatsimRandom.getLocalInstance();	
		MatsimRandom.reset(4711 + 5);
		MatsimRandom.reset(4711 - 5);
		MatsimRandom.reset(4711 + 10);
		MatsimRandom.reset(2015);
	}
	
	@Override
	public Plan selectPlan(final HasPlansAndId<Plan, Person> person) {
		double random= MatsimRandom.getRandom().nextDouble();
		int selIndex=1;
		
		if(random> 0.69){
			selIndex=3;
		}else if(random>0.42){
			selIndex=2;
		}else if(random>0.21){
			selIndex=1;
		}else{
			selIndex=0;
		}
		return person.getPlans().get(selIndex);
	}
	
	public static void main(String[] args) {
		final String plansFile = "../mmoyo/output/taste/inputPlans/500.plans_fromCalibM44.xml.gz";
		final String netFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		final String TAB = "\t";

		DataLoader dLoader = new DataLoader();
		Population pop= dLoader.readPopulation(plansFile);
		
		DistribSelector randomTest = new DistribSelector();
		
		for (Person person: pop.getPersons().values()){
			Plan selectedPlan =  randomTest.selectPlan(person);
			int i = person.getPlans().indexOf(selectedPlan);
			System.out.print (i + TAB);
			
			//set the new selected plan
			((PersonImpl)person).setSelectedPlan(selectedPlan);
		}
		
		//write plan with changed selected Plans
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, dLoader.readNetwork(netFile));
		File file = new File (plansFile);
		String changedPopFile = file.getParent() + File.separatorChar + file.getName() + "probDistribSelectPlan6.xml.gz"; 
		popwriter.write(changedPopFile);
		System.out.println("done");
	}
}
