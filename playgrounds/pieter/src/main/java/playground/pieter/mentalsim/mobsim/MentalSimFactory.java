/**
 * 
 */
package playground.pieter.mentalsim.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * @author fouriep
 *
 */
public class MentalSimFactory implements MobsimFactory {
	PersonalizableTravelTime ttcalc;
	public MentalSimFactory(PersonalizableTravelTime ttcalc) {
		this.ttcalc = ttcalc;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		return new MentalSim(sc, eventsManager, ttcalc);
	}


}
