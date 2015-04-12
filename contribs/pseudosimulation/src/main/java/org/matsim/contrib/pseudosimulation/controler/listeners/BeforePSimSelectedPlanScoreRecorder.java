package org.matsim.contrib.pseudosimulation.controler.listeners;

import java.util.LinkedHashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.controler.PSimControler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * @author fouriep
 *         <P>
 *         Because psim is set to only execute newly formed plans from plan
 *         mutators, all other selected plans in the population get a score of
 *         zero when scoring is done, because these plans don't generate any
 *         events.
 * 
 *         <P>
 *         The current solution to this is to record the plan scores of agents
 *         not selected for psim in a map in the {@link PSimControler} when
 *         a psim iteration starts (this class's job), then restore the scores
 *         after scoring is run with
 *         {@link AfterScoringSelectedPlanScoreRestoreListener} (to get an idea
 *         of the impact of the psim iteration on the score).
 *         <P>
 *         The class only needs to record scores before a psim iteration is run.
 * 
 */
public class BeforePSimSelectedPlanScoreRecorder implements
		BeforeMobsimListener {
	private final PSimControler c;

	public BeforePSimSelectedPlanScoreRecorder(PSimControler c) {
		super();
		this.c = c;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			return;
		LinkedHashSet<Id> agentsForMentalSimulation = c
				.getAgentsForPseudoSimulation();
        for (Person p : c.getMATSimControler().getScenario().getPopulation().getPersons().values()) {
			if (!agentsForMentalSimulation.contains((Id) p.getId())) {
				c.getNonSimulatedAgentSelectedPlanScores().put(
						(Id) p.getId(), p.getSelectedPlan().getScore());
			}
		}

	}

}
