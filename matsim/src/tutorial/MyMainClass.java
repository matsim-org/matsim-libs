package tutorial;

import org.matsim.controler.Controler;
import org.matsim.utils.vis.netvis.NetVis;


public class MyMainClass {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {"./examples/tutorial/multipleIterations.xml"};
		//Create an instance of the controler and
		Controler controler = new Controler(config);
		//so we don't have to delete the output directory
		controler.setOverwriteFiles(true);
		//add an instance of this class as ControlerListener
		controler.addControlerListener(new MyControlerListener());
		//call run() to start the simulation
		controler.run();
		//open snapshot of the 10th iteration
		String[] visargs = {"output/ITERS/it.10/Snapshot"};
		NetVis.main(visargs);
	}


}
