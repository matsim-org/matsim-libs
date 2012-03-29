package tutorial.programming.example07ControlerListener;

import org.matsim.core.controler.Controler;




public class MyMainClass {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {"examples/tutorial/config/example5-config.xml"};
		//Create an instance of the controler and
		Controler controler = new Controler(config);
		//so we don't have to delete the output directory
		controler.setOverwriteFiles(true);
		//add an instance of this class as ControlerListener
		controler.addControlerListener(new MyControlerListener());
		//call run() to start the simulation
		controler.run();
	}


}
