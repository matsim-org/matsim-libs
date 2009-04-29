package playground.mmoyo.PTCase2;

import java.util.Iterator;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class PTTester {
	private Walk walk = new Walk();
	private final PTOb pt;

	public PTTester(final PTOb pt) {
		super();
		this.pt = pt;
	}

	public void countRoutes(){
		int acts=0;
		int routes=0;
		int intPersonNum=0;
		Population population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, pt.getPtNetworkLayer());
		plansReader.readFile(this.pt.getPlansFile());

		for (Person person: population.getPersons().values()) {
			//Person person = population.getPerson("1003717");
			System.out.println(intPersonNum + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean val =false;
			Activity lastAct = null;
			Activity thisAct= null;

			for (Iterator<BasicActivityImpl> iter= plan.getIteratorAct(); iter.hasNext();) {
		    	thisAct= (Activity)iter.next();
		    	if (val) {
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();
		    		int distToWalk= walk.distToWalk(person.getAge());
		    		Path path = this.pt.getPtRouter2().findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
		    		if(path!=null){
		    			routes++;
		    		}
				}//if val
				lastAct = thisAct;
				val=true;
				acts++;
			}
			intPersonNum++;
		}//for person
		System.out.println("acts:" + acts + " routes:"+ routes);
	}//countRoutes

}
