package playground.pieter.pseudosim.controler.listeners;

import java.util.LinkedHashSet;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import playground.pieter.pseudosim.controler.PseudoSimControler;

public class BeforePSimSelectedPlanScoreRecorder implements
		BeforeMobsimListener {
	private PseudoSimControler c;
	public BeforePSimSelectedPlanScoreRecorder(PseudoSimControler c) {
		super();
		this.c = c;
	}
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		LinkedHashSet<IdImpl> agentsForMentalSimulation = c.getAgentsForPseudoSimulation();
		for(Person p:c.getPopulation().getPersons().values()){
			if(!agentsForMentalSimulation.contains((IdImpl)p.getId())){
				c.getNonSimulatedAgentSelectedPlanScores().put((IdImpl) p.getId(), p.getSelectedPlan().getScore());
			}
		}

	}

}
