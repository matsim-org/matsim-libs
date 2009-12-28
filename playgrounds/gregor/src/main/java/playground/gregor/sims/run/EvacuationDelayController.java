package playground.gregor.sims.run;

import java.util.List;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetGenerator;

import playground.gregor.sims.evacuationdelay.DelayedEvacuationPopulationLoader;

public class EvacuationDelayController extends Controler {

	private List<Building> buildings;

	public EvacuationDelayController(final String[] args) {
		super(args);
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
		
		return new DelayedEvacuationPopulationLoader(this.buildings,this.scenarioData).getPopulation();
	}
	
	public static void main(final String[] args) {
		final Controler controler = new EvacuationDelayController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
