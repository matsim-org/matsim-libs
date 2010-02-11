package playground.gregor.sims.run;

import java.util.List;

import org.matsim.core.controler.Controler;
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
	protected void loadData() {
		super.loadData();
		new EvacuationNetGenerator(this.network, this.config).run();
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile(),this.config.evacuation().getSampleSize());
		}

		new DelayedEvacuationPopulationLoader(this.buildings,this.scenarioData).getPopulation(); // this actually CREATES the population
	}

	public static void main(final String[] args) {
		final Controler controler = new EvacuationDelayController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}
