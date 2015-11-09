package tutorial.programming.example07ControlerListener;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;




public class RunControlerListenerExample {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {"examples/tutorial/programming/example7-config.xml"};
		//Create an instance of the controler and
		Controler controler = new Controler(config);
		//add an instance of this class as ControlerListener
//		controler.addControlerListener(new MyControlerListener());
		//call run() to start the simulation
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(MyEventHandler.class);
				this.addControlerListenerBinding().to(MyControlerListener.class);
			}
		});
		
		
		controler.run();
	}


}
