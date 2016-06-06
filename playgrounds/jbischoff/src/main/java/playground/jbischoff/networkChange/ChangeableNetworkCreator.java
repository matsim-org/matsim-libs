package playground.jbischoff.networkChange;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class ChangeableNetworkCreator {
	private Scenario sc;
	private TravelTimeCalculator tcc ;
	private List<NetworkChangeEvent> networkChangeEvents;
	private final int ENDTIME = 30*3600;
	private final int TIMESTEP = 15*60;
	private final String NETWORKFILE = "D:/runs-svn/braunschweig/output/bs05/output_network.xml.gz";
	private final String EVENTSFILE =  "D:/runs-svn/braunschweig/output/bs05/output_events.xml.gz";
	private final String CHANGEFILE = "D:/runs-svn/braunschweig/output/bs05/bs05changeEvents.xml.gz";

	private final double MINIMUMFREESPEED = 3;

	public ChangeableNetworkCreator(){
		this.networkChangeEvents = new ArrayList<NetworkChangeEvent>();

	}
	public static void main(String[] args) {
		 ChangeableNetworkCreator ncg = new ChangeableNetworkCreator();

		ncg.run();
	
	}
	
	private void run() {
		prepareScen();
		tcc =  readEvents();
		createNetworkChangeEvents(sc.getNetwork(),tcc);
		new NetworkChangeEventsWriter().write(CHANGEFILE, networkChangeEvents);
	}
	
	public void createNetworkChangeEvents(Network network, TravelTimeCalculator tcc2) {
		NetworkChangeEventFactory factory = new NetworkChangeEventFactoryImpl();
		for (Link l : network.getLinks().values()){
			if ((l.getAllowedModes().size() == 1) && l.getAllowedModes().contains("pt"))				
				continue;
			
			double length = l.getLength();
			double previousTravelTime=l.getLength()/l.getFreespeed()	;	
			
			for (double time = 0; time<ENDTIME ; time = time+TIMESTEP){
				
				double newTravelTime = tcc2.getLinkTravelTimes().getLinkTravelTime(l, time, null, null);
				if (newTravelTime != previousTravelTime){
					NetworkChangeEvent nce = factory.createNetworkChangeEvent(time);
					nce.addLink(l);
					double newFreespeed = length / newTravelTime;
					if (newFreespeed < MINIMUMFREESPEED) newFreespeed = MINIMUMFREESPEED;
					ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE, newFreespeed);
					nce.setFreespeedChange(freespeedChange);
					
					
					this.networkChangeEvents.add(nce);
					previousTravelTime= newTravelTime;
				}
			}
		}
	}
	

	private void prepareScen() {
		
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(NETWORKFILE);
	}

	private TravelTimeCalculator readEvents (){
		EventsManager manager = EventsUtils.createEventsManager();
	
		TravelTimeCalculatorConfigGroup ttccg = new TravelTimeCalculatorConfigGroup();
		TravelTimeCalculator tc = new TravelTimeCalculator(sc.getNetwork(), ttccg);
		manager.addHandler(tc);
		new MatsimEventsReader(manager).readFile(EVENTSFILE);
		return tc;
	}

	public List<NetworkChangeEvent> getNetworkChangeEvents() {
		return networkChangeEvents;
	}
	
	
	


}
