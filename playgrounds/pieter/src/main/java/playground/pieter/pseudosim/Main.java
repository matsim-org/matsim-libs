package playground.pieter.pseudosim;

import playground.pieter.pseudosim.controler.PseudoSimControler;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		PseudoSimControler c = new PseudoSimControler(args);
		c.setOverwriteFiles(true);
		c.run();
		System.exit(0);
		
	}

}
