package playground.benjamin;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.run.OTFVis;
import org.matsim.utils.vis.netvis.NetVis;

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
//		String[] array = new String[3];
//		String[] arrayfix = {"eins", "zwei", "undsoweiter"};
//		org.matsim.run.Controler.main(new String[] {"examples/tutorial/singleIteration.xml"});
		Controler c = new Controler("examples/equil/configOTF.xml");
		c.setOverwriteFiles(true);
		c.run();
		
		int lastIteration = c.getConfig().controler().getLastIteration();
		
//		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/Snapshot";
		String out = c.getConfig().controler().getOutputDirectory() + "/ITERS/it."+lastIteration+"/"+lastIteration+".otfvis.mvi";
		
		String[] visargs = {out};
		
		OTFVis.main(new String[] {out});
	}

	
	
}
