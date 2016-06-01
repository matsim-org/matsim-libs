package saleem.p0;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;

// For Generic Junctions

public class GenericP0ControlListener implements StartupListener, IterationStartsListener,IterationEndsListener, ShutdownListener {
	public NetworkImpl network;
	GenericP0ControlHandler handler;
	Scenario scenario;
	public GenericP0ControlListener(Scenario scenario, NetworkImpl network){
		this.network = network;
		this.scenario=scenario;
	}
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		//handler = new P0QueueDelayControl(network, event.getIteration());
		handler.initialise();//To avoid creating objects every time, to save memory
	    network.setNetworkChangeEvents(handler.events);
//	    GenericP0ControlHandler.events.removeAll(GenericP0ControlHandler.events);
		
	}
	public void populateInitialAbsolutePressureDifference(){
	}
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		handler.writeCapacitiestoFiles();//For writing capacities at last iteration, written usually when P0 is not applied
		handler.writeDelaystoFiles();//For writing delays at last iteration, written usually when P0 is not applied
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		handler.populatelastCapacities();//To keep track of capacities on the last day
		handler.printCapacityStats();
		handler.printDelayStats();
		handler.printAverageDelayOverLast20Iters();
		handler.addDailyAverageAbsPresDiff();//To keep track of the daily average absolute pressure difference among links
		handler.addDailyAverageAbsPres();//To keep tracks of the daily average absolute pressure on inlinks
		handler.plotStats();
	}
	@Override
	public void notifyStartup(StartupEvent event) {
		ArrayList<Link> inLinks = new ArrayList<Link>();
		ArrayList<Link> outLinks = new ArrayList<Link>();
		Map<Id<Link>,Link> links = network.getLinks();
		Iterator iterator = links.keySet().iterator();
		while(iterator.hasNext()){
			Link link = links.get(iterator.next());
			if(link.getId().toString().contains("In")){
				inLinks.add(link);
			}
			if(link.getId().toString().contains("Out")){
				outLinks.add(link);
			}
		}
		handler = new GenericP0ControlHandler(scenario, inLinks, outLinks, network);
	    event.getServices().getEvents().addHandler(handler);
	    handler.readCapacitiesFromFiles();//For already written Non P0 Capacities
	    handler.readDelaysFromFiles();//For already written Non P0 delays
	}
}
