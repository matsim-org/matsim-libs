package playground.jbischoff.teach.events;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

public class MatsimClassControlerListener implements StartupListener, IterationEndsListener {

	MyEventHandler myEventHandler;
	 
	@Override
	public void notifyStartup(StartupEvent event) {
		myEventHandler = new MyEventHandler(); 
		event.getControler().getEvents().addHandler(myEventHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		myEventHandler.printPersonWithHighestWorkingTime();
	}

}