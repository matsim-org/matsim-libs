package tutorial.programming.example16customscoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;

public class RunCustomScoringExample {

	static class RainOnPersonEvent extends Event implements HasPersonId {

		private Id<Person> personId;

		public RainOnPersonEvent(double time, Id<Person> personId) {
			super(time);
			this.personId = personId;
		}

		@Override
		public Id<Person> getPersonId() {
			return personId;
		}

		@Override
		public String getEventType() {
			return "rain";
		}

		@Override
		public Map<String, String> getAttributes() {
			final Map<String, String> attributes = super.getAttributes();
			attributes.put("person", getPersonId().toString());
			return attributes;
		}

	}

	public static void main(String[] args) {
		String configFile = "examples/tutorial/config/example5-config.xml" ;
		final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		// Every second person gets a special property which influences their score.
		// ObjectAttributes can be written to and read from files, so in reality,
		// this would come from a census, a pre-processing step, or from anywhere else, 
		// but for this example I just make it up.
		final String DISLIKES_LEAVING_EARLY_AND_COMING_HOME_LATE = "DISLIKES_LEAVING_EARLY_AND_COMING_HOME_LATE";
		final ObjectAttributes personAttributes = scenario.getPopulation().getPersonAttributes();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (Integer.parseInt(person.getId().toString()) % 2 == 0) {
				personAttributes.putAttribute(person.getId().toString(), DISLIKES_LEAVING_EARLY_AND_COMING_HOME_LATE, true);
			} else {
				personAttributes.putAttribute(person.getId().toString(), DISLIKES_LEAVING_EARLY_AND_COMING_HOME_LATE, false);
			}
		}
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// We add a class which reacts on people who enter a link and lets it rain on them
				// if we are within a certain time window.
				// The class registers itself as an EventHandler and also produces events by itself.
				bind(RainEngine.class).asEagerSingleton();
			}
		});
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final CharyparNagelScoringParameters params =
						new CharyparNagelScoringParameters.Builder(scenario, person.getId()).build();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				// Some people (not all) really do not like leaving home early or coming home late. These people
				// get extra penalties if that should happen.
				// This property is not directly attached to the Person object. I have to look it up through my 
				// custom attribute table which I defined above.
				if ((Boolean) personAttributes.getAttribute(person.getId().toString(), DISLIKES_LEAVING_EARLY_AND_COMING_HOME_LATE)) {
					sumScoringFunction.addScoringFunction(new SumScoringFunction.ActivityScoring() {
						private double score;
						@Override public void handleFirstActivity(Activity act) {
							if (act.getEndTime() < (6 * 60 * 60)) {
								score -= 300.0;
							} 
						}
						@Override public void handleActivity(Activity act) {} // Not doing anything on mid-day activities.
						@Override public void handleLastActivity(Activity act) {	
							if (act.getStartTime() > (20.0 * 60 * 60)) {
								score -= 100.0;
							}
						}
						@Override public void finish() {}
						@Override
						public double getScore() {
							return score;
						}
					});
				}

				sumScoringFunction.addScoringFunction(new SumScoringFunction.ArbitraryEventScoring() {
					private double score;
					@Override
					public void handleEvent(Event event) {
						if (event instanceof RainOnPersonEvent) {
							score -= 1000.0;
						}
					}
					@Override public void finish() {}
					@Override
					public double getScore() {
						return score;
					}
				});

				return sumScoringFunction;
			}

		});
		controler.run();
	}
	
	static class RainEngine implements PersonEntersVehicleEventHandler, LinkEnterEventHandler {
		
		private EventsManager eventsManager;
		
		private Map<Id<Vehicle>, Id<Person>> vehicle2driver = new HashMap<>();
		 
		@Inject
		RainEngine(EventsManager eventsManager) {
			this.eventsManager = eventsManager;
			this.eventsManager.addHandler(this);
		}
		
		@Override 
		public void reset(int iteration) {}
		
		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			vehicle2driver.put(event.getVehicleId(), event.getPersonId());
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (rainingAt(event.getTime(), event.getLinkId())) {
				eventsManager.processEvent(new RainOnPersonEvent(event.getTime(), vehicle2driver.get(event.getVehicleId())));
			}
		}

		// It starts raining on link 1 at 7:30.
		private boolean rainingAt(double time, Id<Link> linkId) {
			if (time > (7.5 * 60.0 * 60.0) && linkId.toString().equals("1")) {
				return true;
			} else {
				return false;
			}
		}
		
	}

}
