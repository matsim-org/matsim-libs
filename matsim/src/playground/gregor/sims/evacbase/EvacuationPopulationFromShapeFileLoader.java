package playground.gregor.sims.evacbase;

import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.EvacuationConfigGroup.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;

public class EvacuationPopulationFromShapeFileLoader {

	
	private final List<Building> buildings;
	private final NetworkLayer network;
	protected final Config config;
	private TravelCost tc;
	
	private Population pop = null;
	private QuadTree<Link> quadTree;
	private Scenario scenario;
	private double sample;

	public EvacuationPopulationFromShapeFileLoader(List<Building> buildings,	NetworkLayer network, Config config) {
		this.buildings = buildings;
		this.network = network;
		this.config = config;
	}
	
	public Population getPopulation() {

		if (this.pop != null) {
			return this.pop;
		}
		
		buildQuadTree();
		
		PlansCalcRoute router;
		if (this.tc != null) {
			router = new PlansCalcRoute(this.network, this.tc, new FreespeedTravelTimeCost());
		} else {
			router = new PlansCalcRoute(this.network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());	
		}
		
		this.scenario = this.config.evacuation().getScanrio(); 
		Population pop = new PopulationImpl();
		
		Link saveLink = this.network.getLink("el1");
		EvacuationStartTimeCalculator time = getEndCalculatorTime();
		

		
		this.sample = this.config.simulation().getFlowCapFactor();
		
		int count = 0;
		for (Building building : this.buildings) {
//			if (count >= 1) {
//				break;
//			}
//			if (!building.isQuakeProof() ) {
//				continue;
//			}
			Coordinate c = building.getGeo().getCoordinate();
			Link link = this.quadTree.get(c.x,c.y);
//			Link link = this.network.getNearestLink(new CoordImpl(c.x,c.y));
			
			

			
			int numOfPers = getNumOfPersons(building);
//			i=1;
			
			for (int i = 0; i < numOfPers; i++) {
				Person pers = new PersonImpl(new IdImpl(count++));
				Plan plan = new PlanImpl(pers);
				
				Activity act = new ActivityImpl("h",link);
				act.setEndTime(time.getEvacuationStartTime(act));
				plan.addActivity(act);
				Leg leg = new org.matsim.core.population.LegImpl(TransportMode.car);
				plan.addLeg(leg);
				Activity act2 = new ActivityImpl("h", saveLink);
				plan.addActivity(act2);
				
//				router.run(plan);
				pers.addPlan(plan);
				pop.addPerson(pers);
				
			}
		}
		
		this.pop = pop;
		new PopulationWriter(pop,"pop.xml").write();
		return pop;
		
	}

	protected int getNumOfPersons(Building building) {
		int pers = 0;
		if (scenario == Scenario.day) {
			pers = (int) Math.round(building.getPopDay());
		} else if (scenario == Scenario.night){
			pers = (int) Math.round(building.getPopNight());
		}
		int removed = 0;
		for (int i = 0; i < pers; i++) {
			if (MatsimRandom.getRandom().nextDouble() > this.sample) {
				removed++;
			}
		}
		pers -= removed;
		
		
		if (building.isQuakeProof()) {
			building.setShelterSpace(Math.max(0, building.getShelterSpace()-pers));
			return 0;
		}
		return pers;
	}

	protected EvacuationStartTimeCalculator getEndCalculatorTime() {
		double endTime = Double.NaN;
		if (this.scenario == Scenario.day) {
			endTime = 12 * 3600;
		} else if (this.scenario == Scenario.night) {
			endTime = 3 * 3600;
		}
		return new StaticEvacuationStartTimeCalculator(endTime);
	}

	private void buildQuadTree() {
		this.quadTree = new QuadTree<Link>(0,0,700000,9990000);
		for (Link link : this.network.getLinks().values()) {
			if (link.getId().toString().contains("el") || link.getId().toString().contains("s") ) {
				continue;
			}
			this.quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
		}
		
		
	}

	/**
	 * This method allows to set a travel cost calculator. If not set a free speed travel cost calculator
	 * will be instantiated automatically  
	 * @param tc
	 */
	@Deprecated
	public void setTravelCostCalculator(final TravelCost tc) {
		this.tc  = tc;
	}
}
