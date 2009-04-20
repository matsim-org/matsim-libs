package playground.gregor.sims.shelters;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;
import org.matsim.core.trafficmonitoring.TravelTimeAggregatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMap;

import playground.gregor.sims.shelters.signalsystems.ShelterDoorBlockerSetup;
import playground.gregor.sims.shelters.signalsystems.ShelterInputCounter;
import playground.gregor.sims.socialcost.MarginalTravelCostCalculatorII;
import playground.gregor.sims.socialcost.SocialCostCalculator;
import playground.gregor.sims.socialcost.SocialCostCalculatorMultiLink;



public class ShelterEvacuationControllerSCMultiKink extends Controler {

	private List<Building> buildings = null;
	private Set<Link> shelterLinks;
	private HashMap<Id,Building> shelterLinkMapping;
	private EvacuationShelterNetLoader esnl;
	
	final private static Logger log = Logger.getLogger(ShelterEvacuationControllerSCMultiKink.class);

	public ShelterEvacuationControllerSCMultiKink(final String[] args) {
		super(args);
		//TODO remove
//		this.setOverwriteFiles(true);
	}

	
	

	@Override
	protected void setUp() {
		//signalsystems
		this.config.network().setLaneDefinitionsFile("nullnull");
		
//		//TODO remove
//		this.config.controler().setLastIteration(0);
		super.setUp();
		
		ShelterInputCounter sic = new ShelterInputCounter(this.network,this.shelterLinkMapping);
		this.events.addHandler(sic);
		this.addControlerListener(sic);
		
		
//		//link penalty
//		this.travelCostCalculator = new PenaltyLinkCostCalculator(this.travelTimeCalculator,sic);
//		this.strategyManager = loadStrategyManager();
		
		
//		signalsystems
		this.addControlerListener(new ShelterDoorBlockerSetup());
		this.getQueueSimulationListener().add(sic);
		
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		SocialCostCalculator sc = new SocialCostCalculatorMultiLink(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize(), this.travelTimeCalculator, this.population);
		
		this.events.addHandler(sc);
		this.getQueueSimulationListener().add(sc);
		this.travelCostCalculator = new MarginalTravelCostCalculatorII(this.travelTimeCalculator,sc);
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
	}




	@Override
	protected NetworkLayer loadNetwork() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		this.esnl = new EvacuationShelterNetLoader(this.buildings,this.config);
		NetworkLayer net = this.esnl.getNetwork();
		this.getWorld().setNetworkLayer(net);
		this.getWorld().complete();
		this.shelterLinks = this.esnl.getShelterLinks();
		this.shelterLinkMapping = this.esnl.getShelterLinkMapping();
		
		return net;
	}

	@Override
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		EvacuationPopulationLoader epl = new EvacuationPopulationLoader(this.buildings,this.network,this.shelterLinks,this.config);
		Population pop = epl.getPopulation();
		this.esnl.generateShelterLinks();
		return pop;
	}
	
	private HashMap<Id,Building> getShelterLinkMapping() {
		return this.shelterLinkMapping;
	}

	public static void main(final String[] args) {
		final Controler controler = new ShelterEvacuationControllerSCMultiKink(args);
		controler.run();
		System.exit(0);
	}

}
