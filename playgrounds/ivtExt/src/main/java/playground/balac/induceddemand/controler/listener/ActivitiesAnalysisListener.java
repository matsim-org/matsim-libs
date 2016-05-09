package playground.balac.induceddemand.controler.listener;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;

import playground.balac.induceddemand.strategies.activitychainmodifier.NeighboursCreator;

import java.util.*;

public class ActivitiesAnalysisListener implements IterationEndsListener, IterationStartsListener, BeforeMobsimListener {
	
	private Scenario scenario;
	private static final Logger logger = Logger.getLogger(ActivitiesAnalysisListener.class);

	
	private Map<String, Double> scoreChange;
	
	public ActivitiesAnalysisListener(Scenario scenario, HashMap<String, Double> scoreChange2) {
		
		this.scenario = scenario;
		this.scoreChange = scoreChange2;
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if ( ((ControlerConfigGroup) scenario.getConfig().getModule("controler")).getLastIteration() == event.getIteration()) {
			final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();
	
			Set<String> activityChains = new HashSet<String>();
			Map<String, Integer> actPerType = new HashMap<String, Integer>();
			
			int[] sizeChain = new int[30];
			
			stageTypes.addActivityTypes( new StageActivityTypesImpl( "pt interaction" ) );
	
			int size = 0;
			int count = 0;
			
			for (Person person : scenario.getPopulation().getPersons().values()) {
				
				List<Activity> t = TripStructureUtils.getActivities(person.getSelectedPlan(), stageTypes);
				boolean ind = false;
				for (Activity a : t) {
					
					if (a.getType().equals("cb-home") || a.getType().equals("cb-tta") || a.getType().equals("freight") )
						ind = true;
				}
				if (!ind) {
				sizeChain[t.size()]++;
				size += t.size();
				count++;
	
				String chain = "";
				
				for(Activity a : t) {
					
					if (actPerType.containsKey(a.getType())) {
						
						int current = actPerType.get(a.getType());
						actPerType.put(a.getType(), current + 1);
						
					}
					else {
						
						actPerType.put(a.getType(), 1);
					}
					
					if (!chain.equals(""))
						chain = chain.concat("-");
					chain = chain.concat(a.getType());
				}
				
				activityChains.add(chain);
				}
				
			}
			
			System.out.println("Average number of activities per person is: " + (double)size/(double)count);
			System.out.println("Number of different chains is: " + activityChains.size());
			System.out.println("Chains are: ");
			System.out.println("");
	
			for (String c : activityChains) {
				
				System.out.println(c);
			}
			
			System.out.println("");
	
			for (String s : actPerType.keySet()) {
				
				System.out.println(s + ": " + actPerType.get(s).toString());
			}
			System.out.println("");
	
			
			for(int x : sizeChain) {
				
				System.out.println(x);
			}
		}
		
		for (String s : this.scoreChange.keySet()) {
			Person person = scenario.getPopulation().getPersons().get(Id.create(s, Person.class));
			Plan plan = person.getSelectedPlan();
			
			double score = plan.getScore();
			double change = score - this.scoreChange.get(s);
			logger.info(change + " " + person.getId());
			}
		
		
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
			this.scoreChange.clear();
		
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		
	}

}
