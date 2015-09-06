package playground.wrashid.msimoni.analyses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class AgentModeScoreAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String networkFile = "\\\\kosrae.ethz.ch\\ivt-home\\simonimi\\thesis\\JDEQSIM_tests\\tests\\output_no_pricing_v5_subtours_JDEQSim_working\\output_network.xml.gz";
		String eventsFile = "H:/thesis/pricing_tests/output_no_pricing_v5_subtours_JDEQSim_squeeze150_VC12pct_spreadtoll3/ITERS/it.50/50.events.xml.gz";
		String plansFile = "H:/thesis/pricing_tests/output_no_pricing_v5_subtours_JDEQSim_squeeze150_VC12pct_spreadtoll3" +
				"/output_plans.xml.gz";

		Coord center = new Coord(682548.0, 247525.5);

		double radiusInMeters = 1500;
		
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile);
		Map<Id<Link>, Link> links = LinkSelector.selectLinks(scenario.getNetwork(), center, radiusInMeters, 0.0);
		
		Population population = scenario.getPopulation();

		EventsManager events = EventsUtils.createEventsManager();

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
			
			ArrayList<Activity> a = new ArrayList<Activity>();
			
			for (PlanElement pe:selectedPlan.getPlanElements()){
				if (pe instanceof Activity){
					Coord coordAct = ((Activity) pe).getCoord();
					if (GeneralLib.getDistance(center, coordAct)>=radiusInMeters){
						hasOutSideCordonActivity=true;
						
						
					} else {
						hasInsideCordonActivity = true;
						
						a.add((Activity) pe);
						
					}
				}
			}
			
			if (hasOutSideCordonActivity && hasInsideCordonActivity){
				cordonEntered=1;
			}
			
			if (cordonEntered == 0)
				System.out.println(p.getId() + "\t" + selectedPlan.getScore() + "\t" + firstActvityCoordinate.getX() + "\t" + firstActvityCoordinate.getY() + "\t" + cordonEntered + "\t" + modeUsedForEnteringCordonIsCar + "\t" +"0");
			else {
				if (a.size() != 0)
					for (Activity ac:a) {
						System.out.println(p.getId() + "\t" + selectedPlan.getScore() + "\t" + firstActvityCoordinate.getX() + "\t" + firstActvityCoordinate.getY() + "\t" + cordonEntered + "\t" + modeUsedForEnteringCordonIsCar + "\t" + ac.getType());

					}
				else 
					System.out.println(p.getId() + "\t" + selectedPlan.getScore() + "\t" + firstActvityCoordinate.getX() + "\t" + firstActvityCoordinate.getY() + "\t" + cordonEntered + "\t" + modeUsedForEnteringCordonIsCar + "\t" + "0");

			}
		}
		
	}
	
	
	private static class DidCarEnterCordon implements  LinkEnterEventHandler{

		
		private Map<Id<Link>, Link> links;
		private HashSet<Id> agentWhoEnteredCordon=new HashSet<Id>();


		public DidCarEnterCordon(Map<Id<Link>, Link> links){
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
