package playground.anhorni.locationchoice.cs.test;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.router.PlansCalcRoute;
import org.matsim.utils.geometry.CoordImpl;

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
		
		Link link0 = network.getNearestLink(new CoordImpl(681740.0, 251920.0));
		Act fromAct = new Act("home", link0);
		
		Link link1 = network.getNearestLink(new CoordImpl(682040.0, 251720.0));
		Act toAct = new Act("shop", link1);
		fromAct.setStartTime(0.0);
		fromAct.setEndTime(5.0);
		toAct.setStartTime(100.0);
		toAct.setEndTime(150.0);
		
		Leg legBefore = computeLeg(fromAct, toAct, controler);	
		log.info(legBefore.getTravelTime());
	
		//--------------------------------------------------			
		fromAct = new Act("shop", link1);
		toAct = new Act("shop", link0);
		fromAct.setStartTime(200.0);
		fromAct.setEndTime(300.0);
		toAct.setStartTime(1000.0);
		toAct.setEndTime(1200.0);
		
		Leg legAfter = computeLeg(fromAct, toAct, controler);
		log.info(legAfter.getTravelTime());
		//--------------------------------------------------					
	}
	
	
	private Leg computeLeg(Act fromAct, Act toAct, Controler controler) {	
		Leg leg = new Leg(BasicLeg.Mode.car);		
		PlansCalcRoute router = (PlansCalcRoute)controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());	
		return leg;
	}	
}
