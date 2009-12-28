package playground.andreas.bln.pop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Filter persons, not using a specific TransportMode.
 * 
 * @author aneumann
 * 
 */
public class SetPersonCoordsToBoundingBox extends NewPopulation {
	private static final Logger log = Logger.getLogger(SetPersonCoordsToBoundingBox.class);
	private int planswritten = 0;
	private int personshandled = 0;
	private Coord minXY;
	private Coord maxXY;

	public SetPersonCoordsToBoundingBox(PopulationImpl plans, String filename, Coord minXY, Coord maxXY) {
		super(plans, filename);
		this.minXY = minXY;
		this.maxXY = maxXY;
	}

	@Override
	public void run(Person person) {
		
		this.personshandled++;
		
		if(person.getPlans().size() != 1){
			System.err.println("Person got more than one plan");
		} else {
			
			Plan plan = person.getPlans().get(0);
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof ActivityImpl){
					ActivityImpl act = (ActivityImpl) planElement;
					double x = Double.NaN;
					double y = Double.NaN;
					
					if(act.getCoord().getX() < this.minXY.getX()){
						x  = this.minXY.getX();
					}
					if(act.getCoord().getX() > this.maxXY.getX()){
						x = this.maxXY.getX();
					}
					if(act.getCoord().getY() < this.minXY.getY()){
						y = this.minXY.getY();
					}
					if(act.getCoord().getY() > this.maxXY.getY()){
						y = this.maxXY.getY();
					}
					
					if(act.getCoord().getX() >= this.minXY.getX() && act.getCoord().getX() <= this.maxXY.getX()){
						x = act.getCoord().getX();
					}
					if(act.getCoord().getY() >= this.minXY.getY() && act.getCoord().getY() <= this.maxXY.getY()){
						y = act.getCoord().getY();
					}
					
					if(x == Double.NaN || y == Double.NaN){
						System.err.print("Coords are null");
					}
					
					act.setCoord(new CoordImpl(x, y));					
				}
			}
			
			this.popWriter.writePerson(person);
			this.planswritten++;
			
		}

	}
	
	protected void printStatistics() {
		log.info("Finished: " + this.personshandled + " persons handled; " + this.planswritten);
	}
	
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		
		ScenarioImpl sc = new ScenarioImpl();
		Gbl.setConfig(sc.getConfig());

		String networkFile = "D:/Berlin/BVG/berlin-bvg09/net/miv_small/m44_344_small_ba.xml.gz";
		String inPlansFile = "D:/Berlin/BVG/berlin-bvg09/pop/baseplan_900s_subset.xml.gz";
		String outPlansFile = "./baseplan_900s_subset_bb.xml.gz";
		//xrange(4590999.0,4606021.0); yrange(5805999.0,5822001.0)
		Coord minXY = new CoordImpl(4590999.0, 5805999.0);
		Coord maxXY = new CoordImpl(4606021.0, 5822001.0);

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		PopulationImpl inPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(inPlansFile);

		SetPersonCoordsToBoundingBox dp = new SetPersonCoordsToBoundingBox(inPop, outPlansFile, minXY, maxXY);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
