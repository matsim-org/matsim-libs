package playground.gregor.sims.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.evacuation.socialcost.MarginalTravelCostCalculatorII;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;

public class SocialCostTest extends Controler {

	private Link l0;
	private Link l5;

	public SocialCostTest(String[] args) {
		super(args);
		this.setOverwriteFiles(true);
	}
	
	@Override
	protected void setUp() {
		super.setUp();
//		this.scenarioData.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		this.scenarioData.getConfig().simulation().setFlowCapFactor(10.);
		this.scenarioData.getConfig().simulation().setStorageCapFactor(10.);
		this.scenarioData.getConfig().controler().setLastIteration(400);
		this.scenarioData.getConfig().travelTimeCalculator().setTraveltimeBinSize(1);
//		this.scenarioData.getConfig().evacuation().setSampleSize("1.");
//		this.scenarioData.getConfig().simulation().setSnapshotFormat("otfvis");
//		this.scenarioData.getConfig().simulation().setSnapshotPeriod(1);
//		
//		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
//		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
//		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize(), getEvents());
		
		this.events.addHandler(sc);
		this.travelCostCalculator = new MarginalTravelCostCalculatorII(this.travelTimeCalculator,sc);
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
	}
	
	@Override
	protected NetworkLayer loadNetwork() {
		
		NetworkLayer net = this.scenarioData.getNetwork();
		net.setCapacityPeriod(1);
		net.setEffectiveCellSize(0.26);
		net.setEffectiveLaneWidth(0.71);
		
		Node ns0 = net.createAndAddNode(new IdImpl(0),new CoordImpl(-1,0));
		Node ns1 = net.createAndAddNode(new IdImpl("s"),new CoordImpl(0,0));
		Node n0 = net.createAndAddNode(new IdImpl(1),new CoordImpl(4,0));
		Node n1 = net.createAndAddNode(new IdImpl(2),new CoordImpl(10,0));
		Node n2 = net.createAndAddNode(new IdImpl(3),new CoordImpl(10,-4));
		Node nt0 = net.createAndAddNode(new IdImpl("t"),new CoordImpl(14,0));
		Node nt1 = net.createAndAddNode(new IdImpl(6),new CoordImpl(15,0));

		this.l0 = net.createAndAddLink(new IdImpl(0), ns0, ns1, 1, 1, 100, 1);
		net.createAndAddLink(new IdImpl(0+100000), ns1, ns0, 1, 1, 100, 1);
		Link l1 = net.createAndAddLink(new IdImpl(1), ns1, n0, 4, 1, 1, 1);
		net.createAndAddLink(new IdImpl(1+100000), n0, ns1, 4, 1, 1, 1);
		Link l2 = net.createAndAddLink(new IdImpl(2), n0, n1, 10, 1, 1./3., 1);
		net.createAndAddLink(new IdImpl(2+100000), n1, n0, 10, 1./3., 1, 1);
		Link l3 = net.createAndAddLink(new IdImpl(3), n0, n2, 8, 1, 1, 1);
		net.createAndAddLink(new IdImpl(3+100000), n2, n0, 8, 1, 1, 1);
		Link l4 = net.createAndAddLink(new IdImpl(4), n2, n1, 4, 1, 1, 1);
		net.createAndAddLink(new IdImpl(4+100000), n1, n2, 4, 1, 1, 1);
		Link l5 = net.createAndAddLink(new IdImpl(5), n1, nt0, 4, 1, 4, 1);
		net.createAndAddLink(new IdImpl(5+100000), nt0, n1, 4, 1, 4, 1);
		this.l5 = net.createAndAddLink(new IdImpl(6), nt0, nt1, 1, 1, 4, 1);
		net.createAndAddLink(new IdImpl(6+100000), nt1, nt0, 1 , 1, 4, 1);
		
		
//		this.scenarioData.setNetwork(net);
//		this.getWorld().setNetworkLayer(net);
//		this.getWorld().complete();
//		this.shelterLinks = this.esnl.getShelterLinks();

		
		return net;
	}
	
	@Override
	protected Population loadPopulation() {

		Population pop = this.scenarioData.getPopulation();
		
		PopulationFactory pb = new PopulationFactoryImpl(this.scenarioData);
		int id = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j <10 ; j++) {
				Person p = pb.createPerson(new IdImpl(id++));
				Plan plan = pb.createPlan();
				Activity act1 = pb.createActivityFromLinkId("h", this.l0.getId());
				act1.setEndTime(i);
				Leg leg = pb.createLeg(TransportMode.car);
				Activity act2 = pb.createActivityFromLinkId("h", this.l5.getId());
				plan.addActivity(act1);
				plan.addLeg(leg);
				plan.addActivity(act2);
				p.addPlan(plan);
				pop.addPerson(p);
			}
		}
		return pop;
	}
	
	public static void main(final String[] args) {
		final Controler controler = new SocialCostTest(args);
		controler.run();
		System.exit(0);
	}

}
