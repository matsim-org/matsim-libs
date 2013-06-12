package playground.pieter.pseudosim;

import playground.pieter.pseudosim.controler.PSimControler;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		PSimControler c = new PSimControler(args);
		c.setOverwriteFiles(true);
		c.run();
		System.exit(0);
		
	}

}
