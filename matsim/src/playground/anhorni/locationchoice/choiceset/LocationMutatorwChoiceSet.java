package playground.anhorni.locationchoice.choiceset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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


public class LocationMutatorwChoiceSet extends LocationMutator {
	
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

	private void handleSubChains(final Plan plan, List<SubChain> subChains) {
		
		//initially using 60 km/h, 16.7 m/s
		double speed = 60.0/3.6;
		
		Iterator<SubChain> sc_it = subChains.iterator();
		while (sc_it.hasNext()) {
			SubChain sc = sc_it.next();
			this.handleSubChain(sc, speed, 0);			
		}
	}
	
	private void handleSubChain(SubChain subChain, double speed, int trialNr) {
		
		if (trialNr > 100) {
			log.info("Could not do location choice");
			
			Iterator<Act> act_it = subChain.getSlActs().iterator();
			while (act_it.hasNext()) {
				Act act = act_it.next();
				this.modifyLocation(act, subChain.getStartCoord(), subChain.getEndCoord(), Double.MAX_VALUE);
			}
			return;
		}
		
		CoordI startCoord = subChain.getStartCoord();
		CoordI endCoord = subChain.getEndCoord();
		double ttBudget = subChain.getTtBudget();		
		
		Act prevAct = subChain.getFirstPrimAct();
		
		Iterator<Act> act_it = subChain.getSlActs().iterator();
		while (act_it.hasNext()) {
			Act act = act_it.next();
			
			double radius = ttBudget * speed;									
			this.modifyLocation(act, startCoord, endCoord, radius);
			startCoord = act.getCoord();
					
			ttBudget -= this.computeTravelTime(prevAct, act);
			
			if (ttBudget <= 0.0) {
				this.handleSubChain(subChain, speed * 0.9, trialNr++);
				return;
			}
			prevAct = act;
		}
	}
	
	private void modifyLocation(Act act, CoordI startCoord, CoordI endCoord, double radius) {
		ArrayList<Facility> choiceSet = this.computeChoiceSet
		(startCoord, endCoord, radius);
		
		final Facility facility=(Facility)choiceSet.toArray()[
			           Gbl.random.nextInt(choiceSet.size()-1)];
			// plans: link, coords
			// facilities: coords
			// => use coords
		act.setLink(this.network.getNearestLink(facility.getCenter()));
		act.setCoord(facility.getCenter());
		
	}
	
	private double computeTravelTime(Act fromAct, Act toAct) {	
		Leg leg = new Leg(0 ,"car" , 0.0 , 0.0 , 0.0);	
		PlansCalcRoute router = (PlansCalcRoute)this.controler.getRoutingAlgorithm();
		router.handleLeg(leg, fromAct, toAct, fromAct.getEndTime());
		return leg.getTravTime();
	}
	
	
	private List<SubChain> calcActChains(final Plan plan) {
		
		List<SubChain> subChains = new Vector<SubChain>();
					
		boolean chainStarted = false;	
		double actDur = 0.0;
		double slStartTime = 0.0;
		double slEndTime = 0.0;
		
		int subChainIndex = 0;
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);	
			if (act.getType().startsWith("s") || act.getType().startsWith("l")) {
				subChains.get(subChainIndex).addAct(act);
				actDur += act.getDur();	
			}
			else if (act.getType().startsWith("h") || act.getType().startsWith("w")|| 
				act.getType().startsWith("e")) {
			
				if (!chainStarted) {
					subChains.add(new SubChain());
					subChains.get(subChainIndex).setFirstPrimAct(act);
					subChains.get(subChainIndex).setStartCoord(act.getCoord());
					slStartTime = act.getEndTime();
					chainStarted = true;
				}
				else {
					slEndTime = act.getStartTime();
					subChainIndex++;
					subChains.get(subChainIndex).setTtBudget(slEndTime-slStartTime - actDur);
					subChains.get(subChainIndex).setEndCoord(act.getCoord());
					chainStarted = false;
					actDur = 0.0;
				}
			}
		}
		return subChains;
	}
	
	
	private ArrayList<Facility>  computeChoiceSet(CoordI coordStart, CoordI coordEnd, double radius) {
		double midPointX = (coordStart.getX()+coordEnd.getX())/2.0;
		double midPointY = (coordStart.getY()+coordEnd.getY())/2.0;
		return (ArrayList<Facility>) super.zhShopFacQuadTree.get(midPointX, midPointY, radius);
	}
}
