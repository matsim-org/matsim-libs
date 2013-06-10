package playground.pieter.pseudosim;

import org.matsim.core.controler.Controler;


public class NoPseudoSim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
//		c.addControlerListener(new SimpleAnnealer());
//		StochasticRouterFactory str = new StochasticRouterFactory();
//		str.setBeta(4);
//		c.setLeastCostPathCalculatorFactory(str);
//		c.setTravelDisutilityFactory(new CapacityFavoringStochasticCostCalculatorFactory());
//		c.addControlerListener(new SimpleAnnealer());
		c.run();
		System.exit(0);
		
	}

}
