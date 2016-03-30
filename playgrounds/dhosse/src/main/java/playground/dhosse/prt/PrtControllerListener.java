package playground.dhosse.prt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.events.*;
import playground.michalm.util.MovingAgentsRegister;

/**
 * 
 * Class that contains all event handlers needed for PRT.
 * 
 * @author dhosse
 *
 */

public class PrtControllerListener implements StartupListener, IterationStartsListener,
		BeforeMobsimListener, AfterMobsimListener, IterationEndsListener {

	private PrtVehicleFactory vfl;
	private CostContainerHandler cch;
	private CostContainers2PersonMoneyEvent cc;
	private PrtRankAndPassengerStatsHandler rsh;
	private PrtStatsWriter statsWriter;


	public PrtControllerListener(PrtConfig config, MatsimServices controler,
			PrtData data, VrpData vrpData, Scenario scenario) {
		
		this.cch = new CostContainerHandler(controler.getScenario().getNetwork(),
				config.getFixedCost(), config.getVariableCostsD());
		this.cc = new CostContainers2PersonMoneyEvent(controler, cch);
		this.rsh = new PrtRankAndPassengerStatsHandler(vrpData, scenario, data);
		this.statsWriter = new PrtStatsWriter(config, cch, rsh);
		this.vfl = new PrtVehicleFactory(config, vrpData, scenario, rsh);
		
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		MatsimServices controler = event.getServices();
		controler.getEvents().addHandler(this.cch);
		controler.getEvents().addHandler(this.rsh);
		MovingAgentsRegister movingAgents = new MovingAgentsRegister();
		controler.getEvents().addHandler(movingAgents);
		
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		this.vfl.notifyIterationStarts(event);
		this.cch.reset(event.getIteration());
		this.rsh.reset(event.getIteration());
		
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		this.statsWriter.notifyBeforeMobsim(event);
		
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		this.cc.notifyAfterMobsim(event);
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
	
		this.statsWriter.notifyIterationEnds(event);
		
	}

}
