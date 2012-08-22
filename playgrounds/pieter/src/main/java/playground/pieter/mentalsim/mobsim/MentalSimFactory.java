/**
 * 
 */
package playground.pieter.mentalsim.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import playground.pieter.mentalsim.controler.MentalSimControler;

/**
 * @author fouriep
 *
 */
public class MentalSimFactory implements MobsimFactory {
	TravelTime ttcalc;
	MentalSimControler controler;
	public MentalSimFactory(TravelTime ttcalc, MentalSimControler controler) {
		this.ttcalc = ttcalc;
		this.controler = controler;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		return new MentalSim(sc, eventsManager, ttcalc,controler);
	}


}
