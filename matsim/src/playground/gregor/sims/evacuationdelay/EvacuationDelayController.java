package playground.gregor.sims.evacuationdelay;

import java.util.List;

import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;

import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.BuildingsShapeReader;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;
import playground.gregor.sims.shelters.ShelterEvacuationController;

public class EvacuationDelayController extends Controler {

	private List<Building> buildings;

	public EvacuationDelayController(final String[] args) {
		super(args);
	}
	
	@Override
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		EvacuationPopulationFromShapeFileLoader epl = new EvacuationPopulationFromShapeFileLoader(this.buildings,this.network,this.config);
		Population pop = epl.getPopulation();
//		this.esnl.generateShelterLinks();
		return pop;
	}
	
	public static void main(final String[] args) {
		final Controler controler = new ShelterEvacuationController(args);
		controler.run();
		System.exit(0);
	}
}
