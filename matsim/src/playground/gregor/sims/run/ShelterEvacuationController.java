package playground.gregor.sims.run;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.shelters.EvacuationShelterNetLoader;

import playground.gregor.sims.shelters.socialcost.ShelterInputCounterSocialCost;




public class ShelterEvacuationController extends Controler {

	private List<Building> buildings = null;
	private HashMap<Id,Building> shelterLinkMapping;
	private EvacuationShelterNetLoader esnl;
	

	public ShelterEvacuationController(final String[] args) {
		super(args);
		this.setOverwriteFiles(true);
		
	}

	
	

	@Override
	protected void setUp() {
		
		
//		//TODO remove
//		this.config.controler().setLastIteration(0);
		super.setUp();
		
		ShelterInputCounterSocialCost si = new ShelterInputCounterSocialCost(this.scenarioData,this.shelterLinkMapping);
		this.events.addHandler(si);
		this.addControlerListener(si);
		
//		//link penalty
//		this.travelCostCalculator = new PenaltyLinkCostCalculator(this.travelTimeCalculator,sic);
//		this.strategyManager = loadStrategyManager();
		
		
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = this.getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		}
		
		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr  = new FloodingReader(netcdf);
		fr.setReadTriangles(true);
		
//		RiskCostCalculator rc = new RiskCostFromFloodingData(this.network,fr,getEvents());
//		this.events.addHandler(rc);
		
		this.travelCostCalculator = new ShelterSocialCostRiskCostTravelCost(this.travelTimeCalculator,si,null,null);
		
	}


	


	@Override
	protected NetworkLayer loadNetwork() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
		}
		this.esnl = new EvacuationShelterNetLoader(this.buildings,this.scenarioData);
		NetworkLayer net = this.esnl.getNetwork();
		this.getWorld().setNetworkLayer(net);
		this.getWorld().complete();
//		this.shelterLinks = this.esnl.getShelterLinks();
		this.shelterLinkMapping = this.esnl.getShelterLinkMapping();
		
		return net;
	}

	@Override
	protected PopulationImpl loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
		}
		EvacuationPopulationFromShapeFileLoader epl = new EvacuationPopulationFromShapeFileLoader(this.buildings,this.scenarioData);
		PopulationImpl pop = epl.getPopulation();
		this.esnl.generateShelterLinks();
		this.scenarioData.setPopulation(pop);
		return pop;
	}
	
//	private HashMap<Id,Building> getShelterLinkMapping() {
//		return this.shelterLinkMapping;
//	}

	public static void main(final String[] args) {
		final Controler controler = new ShelterEvacuationController(args);
		controler.run();
		System.exit(0);
	}

}
