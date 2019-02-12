package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.ArrayList;
import java.util.List;


public class ChangeableNetworkCreator {
	private Scenario sc;
	private TravelTimeCalculator tcc ;
	private List<NetworkChangeEvent> networkChangeEvents;
	private static final int ENDTIME = 30*3600;
	private static final int TIMESTEP = 15*60;
	private static final String NETWORKFILE = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz";
	private static final String EVENTSFILE =  "C:/Users/Joschka/Documents/runs-svn/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.events.xml.gz";
	private static final String CHANGEFILE = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/changeevents100.xml.gz";
//	 private final String NETWORKFILE = "C:/local_jb/cottbus/network.xml.gz";
//	 private final String EVENTSFILE =  "C:/local_jb/cottbus/1212.0.events_nosig.xml.gz";
//	 private final String CHANGEFILE = "C:/local_jb/cottbus/changeevents.xml";
	
	private static final double MINIMUMFREESPEED = 3;

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
		for (Link l : network.getLinks().values()){
			if (l.getId().toString().startsWith("pt")) continue;
			
			double length = l.getLength();
			double previousTravelTime=l.getLength()/l.getFreespeed()	;	
			
			for (double time = 0; time<ENDTIME ; time = time+TIMESTEP){
				
				double newTravelTime = tcc2.getLinkTravelTimes().getLinkTravelTime(l, time, null, null);
				if (newTravelTime != previousTravelTime){
					NetworkChangeEvent nce = new NetworkChangeEvent(time);
					nce.addLink(l);
					double newFreespeed = length / newTravelTime;
					if (newFreespeed < MINIMUMFREESPEED) newFreespeed = MINIMUMFREESPEED;
					ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, newFreespeed);
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
		TravelTimeCalculator tc = new TravelTimeCalculator( sc.getNetwork(), ttccg );
		manager.addHandler(tc);
		new MatsimEventsReader(manager).readFile(EVENTSFILE);
		return tc;
	}

	public List<NetworkChangeEvent> getNetworkChangeEvents() {
		return networkChangeEvents;
	}
	
	
	


}
