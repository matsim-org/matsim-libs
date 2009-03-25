package playground.anhorni.locationchoice.cs.test;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
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
			
		NetworkLayer network = controler.getNetwork();
		
		Link link0 = network.getNearestLink(new CoordImpl(681753.6875, 251900.64844999998));
		Activity fromAct = new org.matsim.core.population.ActivityImpl("home", link0);
		
		Link link1 = network.getNearestLink(new CoordImpl(695278.8125, 257607.125));
		Activity toAct = new org.matsim.core.population.ActivityImpl("shop", link1);
		fromAct.setEndTime(0.0);
		
		Leg leg = computeLeg(fromAct, toAct, controler);	
		log.info(leg.getTravelTime());					
	}
	
	
	private Leg computeLeg(Activity fromAct, Activity toAct, Controler controler) {	
		Leg leg = new org.matsim.core.population.LegImpl(BasicLeg.Mode.car);		
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());	
		return leg;
	}	
}
