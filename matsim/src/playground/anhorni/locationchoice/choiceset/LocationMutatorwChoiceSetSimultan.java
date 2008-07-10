package playground.anhorni.locationchoice.choiceset;


import java.util.Iterator;
import org.apache.log4j.Logger;
import org.matsim.controler.Controler;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.utils.geometry.CoordI;


public class LocationMutatorwChoiceSetSimultan extends LocationMutatorwChoiceSet {
	
	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSetSimultan.class);
	
	public LocationMutatorwChoiceSetSimultan(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	@Override
	protected void handleSubChain(SubChain subChain, double speed, int trialNr) {
		
		if (trialNr > 10) {
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
			log.info("radius " +radius);
			
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
}
