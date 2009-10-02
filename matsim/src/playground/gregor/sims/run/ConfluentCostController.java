package playground.gregor.sims.run;

import java.util.List;

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostCalculator;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;

import playground.gregor.sims.confluent.LinkPenalty;
import playground.gregor.sims.confluent.LinkPenaltyCalculatorII;
import playground.gregor.sims.confluent.RiskAverseCostCalculator;

public class ConfluentCostController extends Controler {
	private List<Building> buildings;
	public ConfluentCostController(String[] args) {
		super(args);
	}
	
	
	
	@Override
	protected void setUp() {
		
		LinkPenalty lpc = new LinkPenaltyCalculatorII(this.network);
		this.events.addHandler(lpc);
		addControlerListener(lpc);
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = TravelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		}
		
		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr  = new FloodingReader(netcdf);
		
		RiskCostCalculator rc = new RiskCostFromFloodingData(this.network,fr,getEvents(),this.scenarioData.getConfig().evacuation().getBufferSize());
		
		
		this.travelCostCalculator = new RiskAverseCostCalculator(this.travelTimeCalculator,lpc,rc); 
		
		super.setUp();
	}



	@Override
	protected NetworkLayer loadNetwork() {
		NetworkLayer net =  super.loadNetwork();
		new EvacuationNetGenerator(net,this.config).run();
		return net;
	}

	@Override
	protected PopulationImpl loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
		}
		
		return new EvacuationPopulationFromShapeFileLoader(this.buildings,this.scenarioData).getPopulation();
	}
	
	public static void main(final String[] args) {
		final Controler controler = new ConfluentCostController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
