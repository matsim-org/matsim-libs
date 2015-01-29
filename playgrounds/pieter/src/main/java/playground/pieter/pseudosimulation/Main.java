package playground.pieter.pseudosimulation;

import playground.pieter.pseudosimulation.controler.PSimControler;


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
