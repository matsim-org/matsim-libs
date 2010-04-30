package tutorial.programming.example7ControlerListener;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFClientSwing;




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
		
		Scenario sc = controler.getScenario() ;
		Config cf = sc.getConfig() ;
		String dir = cf.controler().getOutputDirectory();
		new OTFClientSwing("file:" + dir + "/ITERS/it.10/10.otfvis.mvi").run();
	}


}
