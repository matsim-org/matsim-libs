package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.QSim;

import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.michalm.taxi.TaxiActionCreator;
import playground.michalm.taxi.optimizer.TaxiOptimizer;

public class PrtQSimFactory implements MobsimFactory {
	
	private final PrtConfigGroup prtConfig;
	private final MatsimVrpContextImpl context;
	private final TaxiOptimizer optimizer;
	
	public PrtQSimFactory(PrtConfigGroup prtConfig, MatsimVrpContextImpl context, TaxiOptimizer optimizer){
		this.prtConfig = prtConfig;
		this.context = context;
		this.optimizer = optimizer;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
		
		QSim qSim = DynAgentLauncherUtils.initQSim(sc);
		this.context.setMobsimTimer(qSim.getSimTimer());
		qSim.addQueueSimulationListeners(this.optimizer);
		
		PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE,
				new PrtRequestCreator(), this.optimizer, this.context);
		qSim.addMobsimEngine(passengerEngine);
		qSim.addDepartureHandler(passengerEngine);
		
		if(this.prtConfig.getVehicleCapacity() > 1){
			VrpLauncherUtils.initAgentSources(qSim, this.context, this.optimizer, new NPersonsActionCreator(
        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, this.prtConfig.getPickupDuration()));
		} else{
			VrpLauncherUtils.initAgentSources(qSim, this.context, this.optimizer, new TaxiActionCreator(
        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, this.prtConfig.getPickupDuration()));
		}
		
		return qSim;
	}

}
