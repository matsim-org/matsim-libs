package playground.dhosse.prt;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.*;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.michalm.taxi.data.TaxiRankDataImpl;

public class PrtModule {
	
	private PrtConfig prtConfig;
	private TravelTime ttime;
	private TravelDisutility tdis;
	
	public void configureControler(final Controler controler, Fleet vrpData){
		ModeRoutingParams pars = new ModeRoutingParams();
		pars.setMode(PrtRequestCreator.MODE);
		pars.setTeleportedModeSpeed(1.);
		controler.getConfig().plansCalcRoute().getModeRoutingParams().put(PrtRequestCreator.MODE, pars);
		
		PrtData prtData = new PrtData(controler.getScenario().getNetwork(), (TaxiRankDataImpl)vrpData);
		
		PrtTripRouterFactoryImpl factory = new PrtTripRouterFactoryImpl(vrpData, controler.getScenario(), ttime, tdis, prtData);
		controler.setTripRouterFactory(factory);

        controler.addOverridingModule(new TaxiModule());

		controler.addControlerListener(new PrtControllerListener(prtConfig, controler, prtData, vrpData, controler.getScenario()));
		
	}
	
}
