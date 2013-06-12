/**
 * 
 */
package playground.pieter.pseudosimulation.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;

import playground.pieter.pseudosimulation.controler.PSimControler;

/**
 * @author fouriep
 *
 */
public class PSimFactory implements MobsimFactory {
	PSimControler controler;
	public PSimFactory( PSimControler controler) {
		this.controler = controler;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		return new PSim(sc, eventsManager, controler);
	}
	


}
