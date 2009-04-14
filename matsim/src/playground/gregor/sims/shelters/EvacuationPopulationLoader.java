package playground.gregor.sims.shelters;

import java.util.List;
import java.util.Set;

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

public class EvacuationPopulationLoader {

	
	private final List<Building> buildings;
	private final NetworkLayer network;
	private final Set<Link> shelterLinks;
	private final Config config;
	private TravelCost tc;
	
	private Population pop = null;
	private QuadTree<Link> quadTree;

	public EvacuationPopulationLoader(List<Building> buildings,	NetworkLayer network, Set<Link> shelterLinks, Config config) {
		this.buildings = buildings;
		this.network = network;
		this.shelterLinks = shelterLinks;
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
		
		Scenario scenario = this.config.evacuation().getScanrio();
		Population pop = new PopulationImpl();
		
		Link saveLink = this.network.getLink("el1");
		
		double endTime = Double.NaN;
		if (scenario == Scenario.day) {
			endTime = 12 * 3600;
		} else if (scenario == Scenario.night) {
			endTime = 3 * 3600;
		}
		
		double sample = this.config.plans().getOutputSample();
		
		int count = 0;
		for (Building building : this.buildings) {
			
			Link link = this.quadTree.get(building.getGeo().getCentroid().getX(), building.getGeo().getCentroid().getY());
			
			
			int i = 0;
			if (scenario == Scenario.day) {
				i = (int) Math.round(building.getPopDay() * sample);
			} else if (scenario == Scenario.night){
				i = (int) Math.round(building.getPopNight() * sample);
			}
			
			for (int j = 0; j < i; j++) {
				Person pers = new PersonImpl(new IdImpl(count++));
				Plan plan = new PlanImpl(pers);
				
				Activity act = new ActivityImpl("h",link);
				act.setEndTime(endTime);
				plan.addActivity(act);
				Leg leg = new org.matsim.core.population.LegImpl(TransportMode.car);
				plan.addLeg(leg);
				Activity act2 = new ActivityImpl("h", saveLink);
				plan.addActivity(act2);
				
				router.run(plan);
				pers.addPlan(plan);
				pop.addPerson(pers);
				
			}
		}
		
		this.pop = pop;
		new PopulationWriter(pop,"pop.xml").write();
		return pop;
		
	}

	private void buildQuadTree() {
		this.quadTree = new QuadTree<Link>(640000,670000,9880000,9900000);
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
	public void setTravelCostCalculator(final TravelCost tc) {
		this.tc  = tc;
	}
}
