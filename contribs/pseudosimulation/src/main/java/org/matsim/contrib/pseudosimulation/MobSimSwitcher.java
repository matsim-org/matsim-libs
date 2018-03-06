package org.matsim.contrib.pseudosimulation;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.PSimFactory;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MobSimSwitcher implements IterationEndsListener,
		IterationStartsListener, BeforeMobsimListener {
	private final ArrayList<Integer> qsimIters = new ArrayList<>();
	private final Scenario scenario;
	private boolean isQSimIteration = true;
	private int cheapIterCount = 0;
	private int currentRate = 0;
	private Map<Id<Person>, Double> selectedPlanScoreMemory;
	private PlanCatcher plancatcher;
	private PSimFactory pSimFactory;

	public MobSimSwitcher(int overridingRate, Scenario scenario) {
		currentRate = overridingRate;
		this.scenario = scenario;
	}

	public PSimFactory getpSimFactory() {
		return pSimFactory;
	}

	public boolean isQSimIteration() {
		return isQSimIteration;
	}

	public ArrayList<Integer> getQSimIters() {
		return qsimIters;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (determineIfQSimIter(event.getIteration())) {
			Logger.getLogger(this.getClass()).warn("Running full queue simulation");

		} else {
			Logger.getLogger(this.getClass()).info("Running PSim");
			plancatcher.init();
		}
	}

	private boolean determineIfQSimIter(int iteration) {

		if (iteration == scenario.getConfig().controler().getLastIteration()) {
			isQSimIteration = true;
			return isQSimIteration;
		}
		if (isQSimIteration && cheapIterCount == 0) {
			isQSimIteration = false;
			cheapIterCount++;
			return isQSimIteration;
		}
		if (cheapIterCount >= currentRate - 1) {
			isQSimIteration = true;
			qsimIters.add(iteration);
			cheapIterCount = 0;
			return isQSimIteration;
		}
		if (isQSimIteration) {
			qsimIters.add(iteration);
		} else {
			cheapIterCount++;

		}
		return isQSimIteration;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		//only for psim iterations
		if (this.isQSimIteration())
			return;

		selectedPlanScoreMemory = new HashMap<>(scenario.getPopulation().getPersons().size());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			selectedPlanScoreMemory.put(person.getId(), person.getSelectedPlan().getScore());
		}
		for (Plan plan : plancatcher.getPlansForPSim()) {
			selectedPlanScoreMemory.remove(plan.getPerson().getId());
		}

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.isQSimIteration())
			return;
		Iterator<Map.Entry<Id<Person>, Double>> iterator = selectedPlanScoreMemory.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Id<Person>, Double> entry = iterator.next();
			scenario.getPopulation().getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
		}
	}

}


