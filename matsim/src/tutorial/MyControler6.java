package tutorial;

import org.matsim.controler.Controler;
import org.matsim.utils.vis.netvis.NetVis;


public class MyControler6 extends Controler {

	private MyEventHandler eventHandler;

	public MyControler6(String[] args) {
		super(args);
		//so we don't have to delete the output directory
		this.setOverwriteFiles(true);

	}

	@Override
	protected void setup() {
		//when overwriting a Controler method we should always call the appropriate super.??() first
		super.setup();
		//create and add the event handler for events of the mobility simulation
		this.eventHandler = new MyEventHandler(this.population.getPersons().size());
		this.events.addHandler(this.eventHandler);
	}

	@Override
	protected void loadCoreListeners() {
		//when overwriting a Controler method we should always call the appropriate super.??() first
		super.loadCoreListeners();
		//the core listener needs access to the event handler to get the data for
		//the graphs
		MyControlerListener listener = new MyControlerListener(this.eventHandler);
		//register listener as ControlerListener
		this.addControlerListener(listener);
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//set a default config for convenience...
		String [] config = {"./examples/tutorial/multipleIterations.xml"};
		//Create an instance of the custom controler and call run() to start the simulation
		new MyControler6(config).run();
		//open snapshot of the 10th iteration
		String[] visargs = {"output/ITERS/it.10/Snapshot"};
		NetVis.main(visargs);
	}

}
