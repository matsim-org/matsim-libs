package org.matsim.contrib.pseudosimulation;

import org.matsim.core.controler.Controler;


public class NoPseudoSim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
        c.getConfig().controler().setCreateGraphs(false);
        c.run();
		
	}

}
