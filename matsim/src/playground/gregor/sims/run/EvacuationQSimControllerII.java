package playground.gregor.sims.run;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;

import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.BuildingsShapeReader;
import playground.gregor.sims.evacbase.EvacuationNetGenerator;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;

public class EvacuationQSimControllerII extends Controler {

	final private static Logger log = Logger.getLogger(EvacuationQSimControllerII.class);
	
	private List<Building> buildings;

	public EvacuationQSimControllerII(String[] args) {
		super(args);
	}
	@Override
	protected void setUp() {
		log.info("generating initial evacuation net... ");
		new EvacuationNetGenerator(this.network,this.config).run();
		log.info("done");

		super.setUp();
	}
	
	
	
	@Override
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		EvacuationPopulationFromShapeFileLoader epl = new EvacuationPopulationFromShapeFileLoader(this.buildings,this.network,this.config);
		Population pop = epl.getPopulation();
		return pop;
	}
	
	public static void main(final String[] args) {
		final Controler controler = new ShelterEvacuationController(args);
		controler.run();
		System.exit(0);
	}	

}
