package playground.sergioo.eventAnalysisTools2012;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sergioo.eventAnalysisTools2012.gui.DynamicVariableSizeSelectionNetworkPainter;
import playground.sergioo.eventAnalysisTools2012.gui.VariableSizeSelectionNetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleDynamicNetworkWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow;

public class EWNoHomeAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler {

	//Classes
	private class PTStage {
	
		//Attributes
		public Id<Link> linkId;
		public double beginInstant = -1;
		public double beginTravelInstant = -1;
		public double finishTravelInstant = -1;
	
	}
	
	//Attributes
	public Map<Id<Person>, List<PTStage>> publicTransportStages = new HashMap<Id<Person>, List<PTStage>>();
	
	//Methods
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		List<PTStage> stages = publicTransportStages.get(event.getPersonId());
		if(stages!=null)
			stages.get(publicTransportStages.get(event.getPersonId()).size()-1).beginTravelInstant = event.getTime();
	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(event.getLegMode().equals("pt"))
			publicTransportStages.get(event.getPersonId()).get(publicTransportStages.get(event.getPersonId()).size()-1).finishTravelInstant = event.getTime();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().equals("pt")) {
			List<PTStage> stages = publicTransportStages.get(event.getPersonId());
			if(stages==null) {
				stages = new ArrayList<PTStage>();
				publicTransportStages.put(event.getPersonId(), stages);
			}
			PTStage stage = new PTStage();
			stage.beginInstant = event.getTime();
			stage.linkId = event.getLinkId();
			stages.add(stage);
		}
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	//Main
	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).parse("./data/MATSim-Sin-2.0/input/network/singapore6.xml");
		EWNoHomeAnalyzer publicTranportTeleportAnalizer = new EWNoHomeAnalyzer();
		EventsManager events = (EventsManager)EventsUtils.createEventsManager();
		events.addHandler(publicTranportTeleportAnalizer);
		new MatsimEventsReader(events).readFile("./data/EWService/0.eventsOptimal.xml.gz");
		System.out.println("Events read");
		int numPersons = 0, numPersonsNoHome = 0, numPtStages = 0, numPtNoHomeStages = 0;
		Map<Id<Link>, Double> linkWeights = new HashMap<Id<Link>, Double>();
		Collection<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collection<Double> startTimes = new ArrayList<Double>();
		Collection<Double> endTimes = new ArrayList<Double>();
		for(List<PTStage> stages:publicTranportTeleportAnalizer.publicTransportStages.values()) {
			numPersons++;
			boolean noHome = false;
			for(PTStage stage:stages) {
				numPtStages++;
				if(stage.beginTravelInstant==-1 && stage.finishTravelInstant==-1) {
					numPtNoHomeStages++;
					noHome = true;
					linkWeights.put(stage.linkId, (linkWeights.get(stage.linkId)==null?0:linkWeights.get(stage.linkId))+1);
					linkIds.add(stage.linkId);
					startTimes.add(stage.beginInstant);
					endTimes.add(Double.MAX_VALUE);
				}
			}
			if(noHome)
				numPersonsNoHome++;
		}
		System.out.println(numPersons+":"+numPersonsNoHome+" "+numPtStages+":"+numPtNoHomeStages);
		VariableSizeSelectionNetworkPainter networkPainter = new VariableSizeSelectionNetworkPainter(scenario.getNetwork());
		networkPainter.setlinkWeights(linkWeights);
		JFrame window = new SimpleNetworkWindow("Links where people is waiting", networkPainter);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		DynamicVariableSizeSelectionNetworkPainter networkPainter2 = new DynamicVariableSizeSelectionNetworkPainter(scenario.getNetwork(), 900, 30*3600);
		networkPainter2.setlinkWeights(linkIds, startTimes, endTimes);
		JFrame window2 = new SimpleDynamicNetworkWindow("Links where people is waiting dynamic", networkPainter2);
		window2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window2.setVisible(true);
	}

}
