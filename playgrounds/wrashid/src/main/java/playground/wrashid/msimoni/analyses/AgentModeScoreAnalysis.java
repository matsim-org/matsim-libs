package playground.wrashid.msimoni.analyses;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class AgentModeScoreAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String networkFile = "C:/data/workspace3/matsim/output/equil/output_network.xml.gz";
		String eventsFile = "C:/data/workspace3/matsim/output/equil/ITERS/it.10/10.events.xml.gz";
		String plansFile = "C:/data/workspace3/matsim/output/equil/output_plans.xml.gz";
		
		Coord center = new CoordImpl(-25000.0, 0.0); 

		double radiusInMeters = 500000;
		
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile);
		Map<Id, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, 0.0);
		
		Population population = scenario.getPopulation();

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		DidCarEnterCordon didCarEnterCordon = new DidCarEnterCordon(links);
		
		events.addHandler(didCarEnterCordon);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
		for (Person p : population.getPersons().values()) {
			Plan selectedPlan = p.getSelectedPlan();
			Coord firstActvityCoordinate = ((Activity) selectedPlan.getPlanElements().get(0)).getCoord();
			
			int modeUsedForEnteringCordonIsCar=didCarEnterCordon.getAgentWhoEnteredCordon().contains(p.getId())?1:0;
			int cordonEntered=0;
			
			if (modeUsedForEnteringCordonIsCar==1){
				cordonEntered=1;
			}
			
			boolean hasOutSideCordonActivity=false;
			boolean hasInsideCordonActivity=false;
			for (PlanElement pe:selectedPlan.getPlanElements()){
				if (pe instanceof Activity){
					Coord coordAct = ((Activity) pe).getCoord();
					if (GeneralLib.getDistance(center, coordAct)>=radiusInMeters){
						hasOutSideCordonActivity=true;
					} else {
						hasInsideCordonActivity=true;
					}
				}
			}
			
			if (hasOutSideCordonActivity && hasInsideCordonActivity){
				cordonEntered=1;
			}
			
			System.out.println(p.getId() + "\t" + selectedPlan.getScore() + "\t" + firstActvityCoordinate.getX() + "\t" + firstActvityCoordinate.getY() + "\t" + cordonEntered + "\t" + modeUsedForEnteringCordonIsCar);
		}
		
	}
	
	
	private static class DidCarEnterCordon implements  LinkEnterEventHandler{

		
		private Map<Id, Link> links;
		private HashSet<Id> agentWhoEnteredCordon=new HashSet<Id>();


		public DidCarEnterCordon(Map<Id, Link> links){
			this.links = links;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

	
		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (links.containsKey(event.getLinkId())){
				getAgentWhoEnteredCordon().add(event.getPersonId());
			}
		}

		public HashSet<Id> getAgentWhoEnteredCordon() {
			return agentWhoEnteredCordon;
		}

		
	}

}
