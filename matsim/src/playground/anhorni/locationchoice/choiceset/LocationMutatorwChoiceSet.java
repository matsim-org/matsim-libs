package playground.anhorni.locationchoice.choiceset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.utils.geometry.CoordI;

import playground.anhorni.locationchoice.LocationMutator;


public abstract class LocationMutatorwChoiceSet extends LocationMutator {
	
	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSet.class);
	
	public LocationMutatorwChoiceSet(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	public void handlePlan(final Plan plan){
		List<SubChain> subChains = this.calcActChains(plan);
		this.handleSubChains(plan, subChains);
		
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}	
	}

	protected void handleSubChains(final Plan plan, List<SubChain> subChains) {
		
		//initially using 25 km/h + 10 %
		// mikrozensus 2005
		double speed = 27.5/3.6;
		
		Iterator<SubChain> sc_it = subChains.iterator();
		while (sc_it.hasNext()) {
			SubChain sc = sc_it.next();
			
			// a ttFactor (1.0) should be calculated for faster convergence
			this.handleSubChain(sc, speed, 0, 1.0);
		}
	}
	
	
	protected void handleSubChain(SubChain subChain, double speed, int trialNr, double ttFactor) {
	}
	
	protected void modifyLocation(Act act, CoordI startCoord, CoordI endCoord, double radius, int trialNr) {
		
		if (trialNr > 10) {
			radius = Double.MAX_VALUE;
		}
		ArrayList<Facility> choiceSet = this.computeChoiceSet
		(startCoord, endCoord, radius, act.getType());
		
		if (choiceSet.size()>1) {
			final Facility facility=(Facility)choiceSet.toArray()[
           			           Gbl.random.nextInt(choiceSet.size()-1)];
       		act.setLink(this.network.getNearestLink(facility.getCenter()));
       		act.setCoord(facility.getCenter());
       		return;
		}
		else {
			this.modifyLocation(act, startCoord, endCoord, radius*1.1, trialNr++);
			return; 			
		}	
	}
	
	protected double computeTravelTime(Act fromAct, Act toAct) {	
		Leg leg = new Leg(0 ,"car" , 0.0 , 0.0 , 0.0);	
		PlansCalcRoute router = (PlansCalcRoute)this.controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravTime();
	}
		
	protected List<SubChain> calcActChains(final Plan plan) {
		
		ManageSubchains manager = new ManageSubchains();
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);	
			
			// found shopping or leisure activity
			if (act.getType().startsWith("s") || act.getType().startsWith("l")) {
				manager.slActivityFound(act, (Leg)actslegs.get(j+1));
			}
			
			// found home, work or education activity
			else if (act.getType().startsWith("h") || act.getType().startsWith("w") || 
					act.getType().startsWith("e")) {
				
				if (j == (actslegs.size()-1)) {
					manager.hweActivityFound(act, null);
				}
				else {
					manager.hweActivityFound(act, (Leg)actslegs.get(j+1));
				}
			}
		}
		return manager.getSubChains();
	}
	
	
	private ArrayList<Facility>  computeChoiceSet(CoordI coordStart, CoordI coordEnd, 
			double radius, String type) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		if (type.startsWith("s")) {
			return (ArrayList<Facility>) this.zhShopFacQuadTree.get(midPointX, midPointY, radius);
		}
		else {
			return (ArrayList<Facility>) this.zhLeisureFacQuadTree.get(midPointX, midPointY, radius);
		}
	}
}
