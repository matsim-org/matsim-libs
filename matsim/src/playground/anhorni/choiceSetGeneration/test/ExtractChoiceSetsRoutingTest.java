package playground.anhorni.choiceSetGeneration.test;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.geometry.CoordImpl;

public class ExtractChoiceSetsRoutingTest implements AfterMobsimListener {
	
	private final static Logger log = Logger.getLogger(ExtractChoiceSetsRoutingTest.class);

	private Controler controler = null;
	public ExtractChoiceSetsRoutingTest(Controler controler) {
		
		this.controler = controler;
		
	}
	public void notifyAfterMobsim(final AfterMobsimEvent event) {	
		if (event.getIteration() < Gbl.getConfig().controler().getLastIteration()) {
			return;
		}
		computeChoiceSet(this.controler);
	}
			
	protected void computeChoiceSet(Controler controler) {
			
		NetworkImpl network = controler.getNetwork();
		
		LinkImpl link0 = network.getNearestLink(new CoordImpl(681753.6875, 251900.64844999998));
		ActivityImpl fromAct = new ActivityImpl("home", link0);
		
		LinkImpl link1 = network.getNearestLink(new CoordImpl(695278.8125, 257607.125));
		ActivityImpl toAct = new ActivityImpl("shop", link1);
		fromAct.setEndTime(0.0);
		
		LegImpl leg = computeLeg(fromAct, toAct, controler);	
		log.info(leg.getTravelTime());					
	}
	
	
	private LegImpl computeLeg(ActivityImpl fromAct, ActivityImpl toAct, Controler controler) {	
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);		
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());	
		return leg;
	}	
}
