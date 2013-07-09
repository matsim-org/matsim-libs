package playground.pieter.pseudosimulation;

import playground.pieter.pseudosimulation.controler.PSimControler;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		PSimControler c = new PSimControler(args);
		c.setOverwriteFiles(true);
		c.setCreateGraphs(false);
		c.run();
		System.exit(0);
		
	}

}
