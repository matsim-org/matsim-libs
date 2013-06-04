package playground.pieter.pseudosim.controler.listeners;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.PersonImpl;

import playground.pieter.pseudosim.controler.PseudoSimControler;

public class BeforePseudoSimSelectedPlanNullifier implements
		BeforeMobsimListener {

	private PseudoSimControler controler;

	public BeforePseudoSimSelectedPlanNullifier(PseudoSimControler c) {
		controler = c;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		//if we don't set the selected plan for persons to null before a mentalsim iter
		//then the selected plans of unsimulated persons get a score of zero
		if(MobSimSwitcher.isMobSimIteration){
			for(Person p :controler.getPopulation().getPersons().values()){
				if(!controler.getAgentsForMentalSimulation().contains(p.getId())){
					((PersonImpl)p).setSelectedPlan(null);
					
				}
			}
			
		}

	}

}
