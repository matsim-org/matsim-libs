package playground.mmoyo.PTCase2;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import playground.mmoyo.Pedestrian.Walk;

/**
 * counts the found paths for a plan
 * @param ptOb pt Object containing all important elements or route search: timetable, network.
 * */
public class PTTester {
	private Walk walk = new Walk();
	private final PTOb ptOb;

	public PTTester(final PTOb ptOb) {
		super();
		this.ptOb = ptOb;
	}

	public void countRoutes(){
		int acts=0;
		int routes=0;
		int intPersonNum=0;
		Population population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, ptOb.getPtNetworkLayer());
		plansReader.readFile(this.ptOb.getPlansFile());

		for (Person person: population.getPersons().values()) {
			//Person person = population.getPerson("1003717");
			System.out.println(intPersonNum + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean val =false;
			Activity lastAct = null;
			Activity thisAct= null;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					thisAct= (Activity) pe;
					if (val) {
						Coord lastActCoord = lastAct.getCoord();
						Coord actCoord = thisAct.getCoord();
						int distToWalk= walk.distToWalk(person.getAge());
						Path path = this.ptOb.getPtRouter2().findPTPath(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
						if(path!=null){
							routes++;
						}
					}
					lastAct = thisAct;
					val=true;
					acts++;
				}
			}
			intPersonNum++;
		}
		System.out.println("acts:" + acts + " routes:"+ routes);
	}
}
