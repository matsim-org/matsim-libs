package org.matsim.contrib.pseudosimulation;


import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
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
	private int psimIterationCount = 0;
	private int iterationsPerCycle = 0;
	private Map<Id<Person>, Double> selectedPlanScoreMemory;
	@Inject private PlanCatcher plancatcher;
	@Inject private PSimProvider pSimProvider;

	@Inject
	public
	MobSimSwitcher(PSimConfigGroup pSimConfigGroup, Scenario scenario) {
		iterationsPerCycle = pSimConfigGroup.getIterationsPerCycle();
		this.scenario = scenario;
	}

	public PSimProvider getpSimProvider() {
		return pSimProvider;
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
			for (Person person : scenario.getPopulation().getPersons().values()) {
					plancatcher.addPlansForPsim(person.getSelectedPlan());
			}

		}
	}

	private boolean determineIfQSimIter(int iteration) {

		if (iteration == scenario.getConfig().controler().getLastIteration() || iteration == scenario.getConfig().controler().getFirstIteration() ) {
			isQSimIteration = true;
			return isQSimIteration;
		}
		if (isQSimIteration && psimIterationCount == 0) {
			isQSimIteration = false;
			psimIterationCount++;
			return isQSimIteration;
		}
		if (psimIterationCount >= iterationsPerCycle - 1) {
			isQSimIteration = true;
			qsimIters.add(iteration);
			psimIterationCount = 0;
			return isQSimIteration;
		}
		if (isQSimIteration) {
			qsimIters.add(iteration);
		} else {
			psimIterationCount++;

		}
		return isQSimIteration;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		//only for psim iterations
		if (this.isQSimIteration())
			return;

		for (Person person : scenario.getPopulation().getPersons().values()) {
				plancatcher.removeExistingPlanOrAddNewPlan(person.getSelectedPlan());
		}

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


