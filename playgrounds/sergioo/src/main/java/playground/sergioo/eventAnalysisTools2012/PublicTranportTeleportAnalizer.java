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

import playground.sergioo.eventAnalysisTools2012.gui.VariableSizeSelectionNetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow;

public class PublicTranportTeleportAnalizer implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler {

	//Classes
	private class PTStage {
	
		//Attributes
		public boolean teleported = true;
		public Id<Link> linkId;
		public double beginInstant;
		public double beginTravelInstant;
		public double finishTravelInstant;
	
	}
	
	//Attributes
	public Map<Id<Person>, List<PTStage>> publicTransportStages = new HashMap<Id<Person>, List<PTStage>>();
	
	//Methods
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		List<PTStage> stages = publicTransportStages.get(event.getPersonId());
		if(stages!=null) {
			PTStage stage = stages.get(publicTransportStages.get(event.getPersonId()).size()-1);
			stage.teleported = false;
			stage.beginTravelInstant = event.getTime();
		}
	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(event.equals("pt")) {
			PTStage stage = publicTransportStages.get(event.getPersonId()).get(publicTransportStages.get(event.getPersonId()).size()-1);
			stage.finishTravelInstant = event.getTime();
		}
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
		new MatsimNetworkReader(scenario).parse("./data/MATSim-Sin-2.0/input/network/singapore7.xml");
		PublicTranportTeleportAnalizer publicTranportTeleportAnalizer = new PublicTranportTeleportAnalizer();
		EventsManager events = (EventsManager)EventsUtils.createEventsManager();
		events.addHandler(publicTranportTeleportAnalizer);
		new MatsimEventsReader(events).readFile("./data/MATSim-Sin-2.0/output/ITERS/it.50/50.events.xml.gz");
		System.out.println("Events read");
		int numPersons = 0, numPersonsTeleported = 0, numPtStages = 0, numPtTeleportedStages = 0;
		Map<Id<Link>, Double> linkWeights = new HashMap<Id<Link>, Double>();
		Collection<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collection<Double> startTimes = new ArrayList<Double>();
		Collection<Double> endTimes = new ArrayList<Double>();
		for(List<PTStage> stages:publicTranportTeleportAnalizer.publicTransportStages.values()) {
			numPersons++;
			boolean teleported = false;
			for(PTStage stage:stages) {
				numPtStages++;
				if(stage.teleported) {
					numPtTeleportedStages++;
					teleported = true;
					linkWeights.put(stage.linkId, (linkWeights.get(stage.linkId)==null?0:linkWeights.get(stage.linkId))+1);
					linkIds.add(stage.linkId);
					startTimes.add(stage.beginInstant);
					endTimes.add(stage.beginTravelInstant);
				}
			}
			if(teleported)
				numPersonsTeleported++;
		}
		System.out.println(numPersons+":"+numPersonsTeleported+" "+numPtStages+":"+numPtTeleportedStages);
		VariableSizeSelectionNetworkPainter networkPainter = new VariableSizeSelectionNetworkPainter(scenario.getNetwork());
		networkPainter.setlinkWeights(linkWeights);
		JFrame window = new SimpleNetworkWindow("Links where people is waiting", networkPainter);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		/*DynamicVariableSizeSelectionNetworkPainter networkPainter2 = new DynamicVariableSizeSelectionNetworkPainter(scenario.getNetwork());
		networkPainter2.setlinkWeights(linkIds, startTimes, endTimes);
		JFrame window2 = new SimpleDynamicNetworkWindow("Links where people is waiting dynamic", networkPainter2);
		window2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window2.setVisible(true);*/
	}

}
