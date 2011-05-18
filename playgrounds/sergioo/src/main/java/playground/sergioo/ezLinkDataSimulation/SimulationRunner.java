package playground.sergioo.ezLinkDataSimulation;

import org.matsim.core.controler.Controler;

public class SimulationRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler("./data/ezLinkDataSimulation/config.xml");
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
