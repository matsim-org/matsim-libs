package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.population.filters.AbstractPersonFilter;

import playground.mmoyo.utils.DataLoader;

public class PtLegDetector extends AbstractPersonFilter{
	
	@Override
	public boolean judge(final Person person) {
		boolean ptLegFound= false;
		int i=0;
		do{
			Plan plan = person.getPlans().get(i++);
			int j=0;
			do{
				PlanElement pe = plan.getPlanElements().get(j++); 
				if (pe instanceof Leg) {
					Leg leg= (Leg)pe;
					ptLegFound = TransportMode.pt.equals(leg.getMode()) || ptLegFound;
				}
			}while(ptLegFound == false && j <plan.getPlanElements().size());
		}while(ptLegFound == false && i <person.getPlans().size());
		return ptLegFound;
	}

	public static void main(String[] args) {
		String popFilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.selected.cleanedXYlinks.xml.gz";
		DataLoader dataLoader  = new DataLoader();
		Population pop = dataLoader.readPopulation(popFilePath); 
		PtLegDetector detector = new PtLegDetector();
		for (Person person : pop.getPersons().values()){
			System.out.println(detector.judge(person));
		}
	}
	
}
