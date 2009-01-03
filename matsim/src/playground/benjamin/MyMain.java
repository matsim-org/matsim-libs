package playground.benjamin;

import org.matsim.controler.Controler;
import org.matsim.run.OTFVis;

public class MyMain {

//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
////		Controler c = new Controler("examples/equil/config.xml");
//		Controler c = new Controler("examples/equil/config.xml");
//		c.setOverwriteFiles(true);
//		c.run();
//		
//		int lastIteration = c.getConfig().controler().getLastIteration();
//		
//		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
//		
//		String[] visargs = {out};
//		Gbl.reset();
//		NetVis.main(visargs);
//		
//		
//	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Controler c = new Controler("examples/equil/config.xml");
//		org.matsim.run.Controler.main(new String[] {"examples/tutorial/singleIteration.xml"});
		String equilExampleConfig = "examples/equil/configOTF.xml";
		
//		String oneRouteNoModeTest = "../studies/bkickhoefer/oneRouteNoModeTest/config.xml";
		String oneRouteNoModeTest = "../studies/bkickhoefer/oneRouteNoModeTest/config.xml";
//		String oneRouteTwoModeTest = "../studies/bkickhoefer/oneRouteTwoModeTest/config.xml";

//		String config = equilExampleConfig;
		String config = oneRouteNoModeTest;
//		String config = oneRouteTwoModeTest;
		
		Controler c = new Controler(config);
		c.setOverwriteFiles(true);
		c.run();
		
		int lastIteration = c.getConfig().controler().getLastIteration();
		
//		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/"+lastIteration+".otfvis.mvi";
		
		String[] visargs = {out};
		
		OTFVis.main(new String[] {out});	
	}

	
	
}
