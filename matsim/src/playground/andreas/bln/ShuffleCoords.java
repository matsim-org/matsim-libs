package playground.andreas.bln;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;

/** 
 * Change the coords of a given plan for every person, except the original one.
 * It is assumed that the original persons has a digit only Id.  
 * 
 * @author aneumann
 *
 */
public class ShuffleCoords extends NewPopulation {

	private double radius; // meter
	
	public ShuffleCoords(Population plans, String filename, double radius) {
		super(plans, filename);
		this.radius = radius;
	}

	@Override
	public void run(PersonImpl person) {
		
		try {
			// Keep old person untouched
			Double.parseDouble(person.getId().toString());
			this.popWriter.writePerson(person);
		} catch (Exception e) {
			// clones need to be handled
			
			PlanImpl plan = person.getPlans().get(0);
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof ActivityImpl){
					ActivityImpl act = (ActivityImpl) planElement;
					
					double x = -0.5 + MatsimRandom.getRandom().nextDouble();
					double y = -0.5 + MatsimRandom.getRandom().nextDouble();
					
					double scale = Math.sqrt((this.radius * this.radius)/(x * x + y * y)) * MatsimRandom.getRandom().nextDouble();
					
					act.setCoord(new CoordImpl(act.getCoord().getX() + x * scale, act.getCoord().getY() + y * scale));
				}
			}
			
			this.popWriter.writePerson(person);
		}		
	}
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./plan_korridor_50x.xml.gz";
		String outPlansFile = "./plan_korridor_50x_sc.xml.gz";

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		Population inPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inPlansFile);

		ShuffleCoords shuffleCoords = new ShuffleCoords(inPop, outPlansFile, 10.0);
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
