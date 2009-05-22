package playground.gregor.sims.run;

import java.util.List;

import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;

import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.BuildingsShapeReader;
import playground.gregor.sims.evacbase.EvacuationNetGenerator;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;
import playground.gregor.sims.evacuationdelay.DelayedEvacuationPopulationLoader;

public class EvacuationDelayController extends Controler {

	private List<Building> buildings;

	public EvacuationDelayController(final String[] args) {
		super(args);
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
		
		return new DelayedEvacuationPopulationLoader(this.buildings,this.network,this.config).getPopulation();
	}
	
	public static void main(final String[] args) {
		final Controler controler = new EvacuationDelayController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
