package playground.gregor.sims.run;

import java.util.List;

import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorBuilder;

import playground.gregor.flooding.FloodingReader;
import playground.gregor.flooding.RiskCostFromFloodingData;
import playground.gregor.sims.confluent.CostCalculator;
import playground.gregor.sims.confluent.LinkPenalty;
import playground.gregor.sims.confluent.LinkPenaltyCalculator;
import playground.gregor.sims.confluent.LinkPenaltyCalculatorII;
import playground.gregor.sims.confluent.LinkPenaltyCalculatorIII;
import playground.gregor.sims.confluent.RiskAverseCostCalculator;
import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.BuildingsShapeReader;
import playground.gregor.sims.evacbase.EvacuationNetGenerator;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;
import playground.gregor.sims.riskaversion.RiskCostCalculator;

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
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = new TravelTimeCalculatorBuilder(this.config.travelTimeCalculator()).createTravelTimeCalculator(this.network, (int)endTime);
		}
		
		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr  = new FloodingReader(netcdf);
		
		RiskCostCalculator rc = new RiskCostFromFloodingData(this.network,fr);
		
		
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
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		
		return new EvacuationPopulationFromShapeFileLoader(this.buildings,this.network,this.config).getPopulation();
	}
	
	public static void main(final String[] args) {
		final Controler controler = new ConfluentCostController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
