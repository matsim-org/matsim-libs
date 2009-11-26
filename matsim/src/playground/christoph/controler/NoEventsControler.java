package playground.christoph.controler;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;

/*
 * Controler that does not process Events. They may be a bottleneck when
 * running the QueueSimulation parallel on multiple threads.
 */
public class NoEventsControler extends Controler{

	public NoEventsControler(String[] args)
	{
		super(args);
	}

	@Override
	protected void runMobSim()
	{
//		super.getEvents().clearHandlers();
		super.events = new MyEventsManagerImpl();
		super.runMobSim();
	}
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final NoEventsControler controler = new NoEventsControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
	
	public class MyEventsManagerImpl extends EventsManagerImpl {
		
		@Override
		public void processEvent(final Event event) {
//			this.counter++;
//			if (this.counter == this.nextCounterMsg) {
//				this.nextCounterMsg *= 2;
//				printEventsCount();
//			}
//			computeEvent(event);
		}
	}
}
