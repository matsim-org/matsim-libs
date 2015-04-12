package org.matsim.contrib.pseudosimulation;

import org.matsim.contrib.pseudosimulation.controler.PSimControler;


public class Main {

	/**
	 * @param args - The name of the config file for the psim run.
	 */
	public static void main(String[] args) {
		PSimControler c = new PSimControler(args);
        c.getMATSimControler().getConfig().controler().setCreateGraphs(false);
        c.getMATSimControler().run();
		
	}

}
