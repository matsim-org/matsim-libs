package playground.pieter.pseudosim.controler.listeners;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PersonImpl;

import playground.pieter.pseudosim.controler.PseudoSimControler;

public class IterationEndsSelectedPlanScoreRestoreListener implements IterationEndsListener{
	PseudoSimControler c;
	public IterationEndsSelectedPlanScoreRestoreListener(PseudoSimControler c) {
		super();
		this.c = c;
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		HashMap<IdImpl,Double> nSASS = c.getNonSimulatedAgentSelectedPlanScores();
		Map<Id, ? extends Person> persons = c.getPopulation().getPersons();
		for(IdImpl id : nSASS.keySet()){
			((PersonImpl)persons.get(id)).getSelectedPlan().setScore(nSASS.get(id));
		}
		
	}

}
