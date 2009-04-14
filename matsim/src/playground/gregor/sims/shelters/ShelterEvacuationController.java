package playground.gregor.sims.shelters;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;


public class ShelterEvacuationController extends Controler {

	private List<Building> buildings = null;
	private Set<Link> shelterLinks;
	
	final private static Logger log = Logger.getLogger(ShelterEvacuationController.class);

	public ShelterEvacuationController(final String[] args) {
		super(args);
	}

	

	@Override
	protected NetworkLayer loadNetwork() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		EvacuationShelterNetLoader esnl = new EvacuationShelterNetLoader(this.buildings,this.config);
		NetworkLayer net = esnl.getNetwork();
		this.getWorld().setNetworkLayer(net);
		this.getWorld().complete();
		this.shelterLinks = esnl.getShelterLinks();
		
		
		return net;
	}

	@Override
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config.evacuation().getBuildingsFile());
		}
		EvacuationPopulationLoader epl = new EvacuationPopulationLoader(this.buildings,this.network,this.shelterLinks,this.config);
		return epl.getPopulation();
	}

	public static void main(final String[] args) {
		final Controler controler = new ShelterEvacuationController(args);
		controler.run();
		System.exit(0);
	}

}
