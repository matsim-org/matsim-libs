package playground.gregor.sims.run;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationBuilderImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorBuilder;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.flooding.FloodingReader;

import playground.gregor.sims.shelters.linkpenaltyII.ShelterInputCounterLinkPenalty;

public class SheltersTest extends Controler{

	private final HashMap<Id,Building> shelterLinkMapping = new HashMap<Id, Building>();
	private LinkImpl l0;
	private LinkImpl el1;
	
	public SheltersTest(String[] args) {
		super(args);
		this.setOverwriteFiles(true);
	}

	

	@Override
	protected void setUp() {
		
		
//		//TODO remove
//		this.config.controler().setLastIteration(0);
		super.setUp();
		
		this.scenarioData.getConfig().simulation().setFlowCapFactor(1.);
		this.scenarioData.getConfig().simulation().setStorageCapFactor(1.);
		this.scenarioData.getConfig().evacuation().setSampleSize("1.");
		this.scenarioData.getConfig().simulation().setSnapshotFormat("otfvis");
		this.scenarioData.getConfig().simulation().setSnapshotPeriod(1);
		
		
		ShelterInputCounterLinkPenalty si = new ShelterInputCounterLinkPenalty(this.network,this.shelterLinkMapping,getEvents());
		this.events.addHandler(si);
		
//		//link penalty
//		this.travelCostCalculator = new PenaltyLinkCostCalculator(this.travelTimeCalculator,sic);
//		this.strategyManager = loadStrategyManager();
		
		
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = TravelTimeCalculatorBuilder.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		}
		
		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr  = new FloodingReader(netcdf,true);
		
//		RiskCostCalculator rc = new RiskCostFromFloodingData(this.network,fr,getEvents());
//		this.events.addHandler(rc);
		
		this.travelCostCalculator = new ShelterLinkPenaltyRiskCostTravelCost(this.travelTimeCalculator,si,null,null);
		
	}


	


	@Override
	protected NetworkLayer loadNetwork() {
		
		NetworkLayer net = new NetworkLayer();
		net.setCapacityPeriod(1);
//		net.setEffectiveCellSize(0.26);
//		net.setEffectiveLaneWidth(0.71);
		
		NodeImpl n0 = net.createNode(new IdImpl(0),new CoordImpl(0,0));
		NodeImpl n1 = net.createNode(new IdImpl(1),new CoordImpl(0,100));
		NodeImpl n2 = net.createNode(new IdImpl(2),new CoordImpl(0,200));
		NodeImpl n3 = net.createNode(new IdImpl(3),new CoordImpl(0,300));
		NodeImpl n4 = net.createNode(new IdImpl(4),new CoordImpl(0,2300));
		NodeImpl en1 = net.createNode(new IdImpl("en1"),new CoordImpl(300,400));
		NodeImpl en2 = net.createNode(new IdImpl("en2"),new CoordImpl(400,400));
		NodeImpl sna = net.createNode(new IdImpl("sn1a"),new CoordImpl(100,200));
		NodeImpl snb = net.createNode(new IdImpl("sn1b"),new CoordImpl(200,200));
		this.l0 = net.createLink(new IdImpl(0), n0, n1, 100, 16.6, 100, 1);
		LinkImpl l1 = net.createLink(new IdImpl(1), n1, n2, 100, 16.6, 1, 1);
		LinkImpl l2 = net.createLink(new IdImpl(2), n2, n3, 100, 16.6, 1, 1);
		LinkImpl l3 = net.createLink(new IdImpl(3), n3, n4, 2000, 16.6, 1, 1);
		this.el1 = net.createLink(new IdImpl("el1"), en1, en2, 100, 10000., 10000., 1);
		LinkImpl el2 = net.createLink(new IdImpl("el2"), n4, en1, 100, 10000., 10000., 1);
		LinkImpl sl1a = net.createLink(new IdImpl("sl1a"), n2, sna, 100, 16.6, 1, 1);
		LinkImpl sl1b = net.createLink(new IdImpl("sl1b"), sna, snb, 100, 16.6, 0.1, 1);
		LinkImpl sl1c = net.createLink(new IdImpl("sl1c"), snb, en1, 100, 10000., 10000., 1);
		
		Building b = new Building(new IdImpl("b0"),0,0,0,10,1,1,null);
		this.shelterLinkMapping.put(sl1b.getId(), b);
		this.scenarioData.setNetwork(net);
		this.getWorld().setNetworkLayer(net);
		this.getWorld().complete();
//		this.shelterLinks = this.esnl.getShelterLinks();

		
		return net;
	}

	@Override
	protected PopulationImpl loadPopulation() {


		PopulationImpl pop = this.scenarioData.getPopulation();
		
		PopulationBuilder pb = new PopulationBuilderImpl(this.scenarioData);
		for (int i = 0; i < 100; i++) {
			Person p = pb.createPerson(new IdImpl(i));
			Plan plan = pb.createPlan();
			Activity act1 = pb.createActivityFromLinkId("h", this.l0.getId());
			act1.setEndTime(0);
			Leg leg = pb.createLeg(TransportMode.car);
			Activity act2 = pb.createActivityFromLinkId("h", this.el1.getId());
			plan.addActivity(act1);
			plan.addLeg(leg);
			plan.addActivity(act2);
			p.addPlan(plan);
			pop.addPerson(p);
		}
		return pop;
	}
	
//	private HashMap<Id,Building> getShelterLinkMapping() {
//		return this.shelterLinkMapping;
//	}

	public static void main(final String[] args) {
		final Controler controler = new SheltersTest(args);
		controler.run();
		System.exit(0);
	}
}
