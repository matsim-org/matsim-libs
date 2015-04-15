package playground.dhosse.prt;

import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.events.CostContainerHandler;
import playground.dhosse.prt.events.CostContainers2PersonMoneyEvent;
import playground.dhosse.prt.events.PrtRankAndPassengerStatsHandler;
import playground.dhosse.prt.events.PrtStatsWriter;
import playground.dhosse.prt.events.PrtVehicleFactory;
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

	public PrtControllerListener(PrtConfigGroup config, Controler controler, MatsimVrpContextImpl context,
			PrtData data) {
		
		this.cch = new CostContainerHandler(controler.getScenario().getNetwork(), context.getVrpData().getVehicles(),
				config.getFixedCost(), config.getVariableCostsD());
		this.cc = new CostContainers2PersonMoneyEvent(controler, cch);
		this.rsh = new PrtRankAndPassengerStatsHandler(context, data);
		this.statsWriter = new PrtStatsWriter(config, cch, rsh);
		this.vfl = new PrtVehicleFactory(config, context, rsh);
		
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
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
