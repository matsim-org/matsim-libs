package playground.mmoyo.PTCase2;

import java.util.Iterator;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.router.util.LeastCostPathCalculator.Path;

public class PTTester {
	private final PTOb pt;

	public PTTester(final PTOb pt) {
		super();
		this.pt = pt;
	}

	public void countRoutes(){
		int acts=0;
		int routes=0;
		int intPersonNum=0;
		Population population = new PopulationImpl(false);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(this.pt.getPlansFile());

		for (Person person: population.getPersons().values()) {
			//Person person = population.getPerson("1003717");
			System.out.println(intPersonNum + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean val =false;
			Act lastAct = null;
			Act thisAct= null;

			for (Iterator<BasicActImpl> iter= plan.getIteratorAct(); iter.hasNext();) {
		    	thisAct= (Act)iter.next();
		    	if (val) {
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();
		    		int distToWalk= distToWalk(person.getAge());
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

	private int distToWalk(final int personAge){
		int distance=0;
		if (personAge>=60)distance=300;
		if (personAge>=40 || personAge<60)distance=400;
		if (personAge>=18 || personAge<40)distance=800;
		if (personAge<18)distance=300;
		return distance;
	}


}
