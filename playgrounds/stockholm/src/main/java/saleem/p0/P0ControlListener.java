package saleem.p0;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;

public class P0ControlListener implements IterationStartsListener,ShutdownListener {
	public NetworkImpl network;
	P0QueueDelayControl handler;
	public P0ControlListener(NetworkImpl network){
		this.network = network;
	}
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		handler = new P0QueueDelayControl(network);
	    event.getControler().getEvents().addHandler(handler);
		
	}
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		handler.plotStats();
		handler.printDelayStats();
		// TODO Auto-generated method stub
		
	}
}
