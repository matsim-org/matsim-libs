package playground.mmoyo.algorithms;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;

import playground.mmoyo.utils.DataLoader;

public class LegTypeCounter {
	private final static Logger log = Logger.getLogger(LegTypeCounter.class);
	
	public void run(final Population pop, String mode ){
		int numAgentWcarlegs=0;
		for (Person person : pop.getPersons().values()){
			boolean hasCarLeg = false;
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof LegImpl) {
						Leg leg = (LegImpl)pe;
						if(leg.getMode().equals(mode)){
							 hasCarLeg = (hasCarLeg || true);
						}
					}
				}
			}
			if (hasCarLeg){numAgentWcarlegs++;}
		}
		
		log.info("agents = " + pop.getPersons().size());
		log.info("agents with car leg = " + numAgentWcarlegs);
	}
	
	public static void main(String[] args) {
		String popFilePath;
		String mode;
		if (args.length>0){
			popFilePath = args[0];
			mode = args[0];
		}else{
			popFilePath = "../../input/2plansCleaned.xml"; // ;"../playgrounds/mmoyo/output/precalculation/routed3150/allRoutes3150.xml";
			mode = TransportMode.pt;
		}
		
		DataLoader dataLoader = new DataLoader();
		Population pop = dataLoader.readPopulation(popFilePath);
		new LegTypeCounter().run(pop, mode );
	}

}