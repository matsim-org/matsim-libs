package playground.pieter.pseudosim.controler.listeners;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.population.PersonImpl;

public class MyIterationEndsListener implements ControlerListener,
		IterationEndsListener {



	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		Logger.getLogger(getClass()).error("Calling Handler");
		Collection<Plan> plans = new LinkedHashSet<Plan>();
		for (Person p : event.getControler().getScenario().getPopulation().getPersons().values()) {
			PersonImpl pax = (PersonImpl) p;
//			System.out.println(pax.getSelectedPlan().getScore());
			if (pax.getSelectedPlan().getScore()==0.0)
				System.out.println(pax.removePlan(pax.getSelectedPlan()));
		}
	}
}
