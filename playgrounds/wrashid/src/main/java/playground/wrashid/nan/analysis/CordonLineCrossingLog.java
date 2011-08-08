package playground.wrashid.nan.analysis;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.list.Lists;

public class CordonLineCrossingLog {

	public static void main(String[] args) {
		String basePath="H:/data/experiments/ARTEMIS/output/run2/";
		String plansFile=basePath + "output_plans.xml.gz";
		String networkFile=basePath + "output_network.xml.gz";
		String facilititiesPath=basePath + "output_facilities.xml.gz";
		String eventsFileName = basePath + "ITERS/it.50/50.events.txt.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();

		CordonVolumeCounter cordonVolumeCounter=new CordonVolumeCounter(scenario);
		eventsManager.addHandler(cordonVolumeCounter);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);
		
		GeneralLib.generateSimpleHistogram("c:/tmp/cordonLeaving.png", Lists.getArray(cordonVolumeCounter.cordonLeavingTimes), 24);
		GeneralLib.generateSimpleHistogram("c:/tmp/cordonEntering.png", Lists.getArray(cordonVolumeCounter.cordonEnteringTimes), 24);
		
	}
	
	private static class CordonVolumeCounter implements LinkEnterEventHandler, AgentDepartureEventHandler {

		IntegerValueHashMap<Id> currentLegIndex=new IntegerValueHashMap<Id>(-1);
		private ScenarioImpl scenarioImpl;
		public LinkedList<Double> cordonLeavingTimes=new LinkedList<Double>();
		public LinkedList<Double> cordonEnteringTimes=new LinkedList<Double>();
		HashMap<Id, Integer> lastLegIndexRegisteredForCrossing=new HashMap<Id, Integer>();
		HashMap<Id, Coord> previousLinkCoordinate=new HashMap<Id, Coord>();
		

		public CordonVolumeCounter(ScenarioImpl scenario) {
			this.scenarioImpl = scenario;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id personId = event.getPersonId();
			Coord prevLink=previousLinkCoordinate.get(personId);
			Coord curLink=getLinkCoordinate(event.getLinkId());
			
			if (prevLink==null){
				previousLinkCoordinate.put(personId, getLinkCoordinate(event.getLinkId()));
				return;
			}
			
			Activity prevAct=(Activity) scenarioImpl.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(currentLegIndex.get(personId)-1);
			Activity nextAct=(Activity) scenarioImpl.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(currentLegIndex.get(personId)+1);
			
			if (CordonTripCountAnalyzer.isInsideCordon(prevAct) && !CordonTripCountAnalyzer.isInsideCordon(nextAct)){
				if (CordonTripCountAnalyzer.isInsideCordon(prevLink) && !CordonTripCountAnalyzer.isInsideCordon(curLink)){
					if (lastLegIndexRegisteredForCrossing.get(personId)!=currentLegIndex.get(personId)){
						cordonLeavingTimes.add(GeneralLib.projectTimeWithin24Hours(event.getTime()));
						lastLegIndexRegisteredForCrossing.put(personId, currentLegIndex.get(personId));
					}
				}
			}
			
			if (!CordonTripCountAnalyzer.isInsideCordon(prevAct) && CordonTripCountAnalyzer.isInsideCordon(nextAct)){
				if (!CordonTripCountAnalyzer.isInsideCordon(prevLink) && CordonTripCountAnalyzer.isInsideCordon(curLink)){
					if (lastLegIndexRegisteredForCrossing.get(personId)!=currentLegIndex.get(personId)){
						cordonEnteringTimes.add(GeneralLib.projectTimeWithin24Hours(event.getTime()));
						lastLegIndexRegisteredForCrossing.put(personId, currentLegIndex.get(personId));
					}
				}
			}
			
			previousLinkCoordinate.put(personId, getLinkCoordinate(event.getLinkId()));
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			currentLegIndex.incrementBy(event.getPersonId(),2);
			previousLinkCoordinate.put(event.getPersonId(), null);
			lastLegIndexRegisteredForCrossing.put(event.getPersonId(), -1);
		}

		
		
		private Coord getLinkCoordinate(Id linkId){
			return scenarioImpl.getNetwork().getLinks().get(linkId).getCoord();
		}
		
	}
	
}
