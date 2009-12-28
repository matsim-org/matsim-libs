package playground.gregor.sims.run;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;

import playground.gregor.sims.confluent.LinkPenaltyCalculatorII;

public class ConfluentCostController extends Controler {
	private List<Building> buildings;
	public ConfluentCostController(String[] args) {
		super(args);
	}
	
	
	
	@Override
	protected void setUp() {
		
		LinkPenaltyCalculatorII lpc = new LinkPenaltyCalculatorII(this.network, this.population);
		this.events.addHandler(lpc);
		addControlerListener(lpc);
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = this.getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		}
		
		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr = new FloodingReader(netcdf);
		fr.setReadTriangles(true);
		List<FloodingReader> frs = new ArrayList<FloodingReader>();
		frs.add(fr);

		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, frs,getEvents(),this.scenarioData.getConfig().evacuation().getBufferSize());
		
		PluggableTravelCostCalculator tc = new PluggableTravelCostCalculator(this.travelTimeCalculator);
		tc.addTravelCost(rc);
		tc.addTravelCost(lpc);
		this.travelCostCalculator = tc; 
		
		super.setUp();
	}



	@Override
	protected NetworkImpl loadNetwork() {
		NetworkImpl net =  super.loadNetwork();
		new EvacuationNetGenerator(net,this.config).run();
		return net;
	}

	@Override
	protected Population loadPopulation() {
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
