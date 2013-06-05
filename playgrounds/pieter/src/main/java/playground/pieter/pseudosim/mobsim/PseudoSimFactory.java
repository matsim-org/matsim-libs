/**
 * 
 */
package playground.pieter.pseudosim.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import playground.pieter.pseudosim.controler.PseudoSimControler;

/**
 * @author fouriep
 *
 */
public class PseudoSimFactory implements MobsimFactory {
	TravelTime ttcalc;
	PseudoSimControler controler;
	public PseudoSimFactory(TravelTime ttcalc, PseudoSimControler controler) {
		this.ttcalc = ttcalc;
		this.controler = controler;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		return new PseudoSim(sc, eventsManager, controler);
	}
	


}
