package playground.andreas.bln;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;

/** Create numberOfcopies additional persons, keep all their attributes, but change
 *  the act's coords, so that new coord is
 *  a) in a perimeter with radius specified,
 *  b) new coords are equally distributed.
 *  
 *  New person isn't a clone eyt, but will be changed from copy to copy.
 *  Thus, every copy will determine its new coords from its predecessor.
 * 
 * @author aneumann
 *
 */
public class DuplicatePlans extends NewPopulation {

	private Person tmpPerson = null;
	private int numberOfCopies = 49;
	private double radius = 1000.0; // meter
	
	public DuplicatePlans(Population plans, String filename) {
		super(plans, filename);
	}

	@Override
	public void run(Person person) {
		// Keep old person untouched
		this.popWriter.writePerson(person);
		Id personId = person.getId();		
		
		for (int i = 1; i < this.numberOfCopies + 1; i++) {
			
			this.tmpPerson = person;
			this.tmpPerson.setId(new IdImpl(personId.toString() + "X" + i));
			shuffleCoordsOfTmpPerson();
			this.popWriter.writePerson(this.tmpPerson);
			
		}		
		
	}
	
	private void shuffleCoordsOfTmpPerson(){

		Plan plan = this.tmpPerson.getPlans().get(0);
		
		for (PlanElement planElement : plan.getPlanElements()) {
			if(planElement instanceof Activity){
				Activity act = (Activity) planElement;
				
				double x = -0.5 + MatsimRandom.getRandom().nextDouble();
				double y = -0.5 + MatsimRandom.getRandom().nextDouble();
				
				double scale = Math.sqrt((this.radius * this.radius)/(x * x + y * y)) * MatsimRandom.getRandom().nextDouble();
				
				act.setCoord(new CoordImpl(act.getCoord().getX() + x * scale, act.getCoord().getY() + y * scale));
			
			}
		}

	}
	
	private Person copyPerson(Person person){
		
		
		
		return person;
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plans3.xml.gz";
		String outPlansFile = "./plans3_5x.xml.gz";

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		Population inPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inPlansFile);

		DuplicatePlans dp = new DuplicatePlans(inPop, outPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
