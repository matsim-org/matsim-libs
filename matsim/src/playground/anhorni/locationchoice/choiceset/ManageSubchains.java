package playground.anhorni.locationchoice.choiceset;

import java.util.List;
import java.util.Vector;

//import org.apache.log4j.Logger;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;

public class ManageSubchains {
	
	//private static final Logger log = Logger.getLogger(ManageSubchains.class);
	
	private List<SubChain> subChains = new Vector<SubChain>();
	
	private int subChainIndex = -1;
	boolean chainStarted = false;
	boolean sl_found = false;

	private double ttBudget = 0.0;
	private double totalTravelDistance = 0.0;
	
	public void slActivityFound(Act act, Leg leg) {
		// no plan starts with s or l !
		this.subChains.get(subChainIndex).addAct(act);
		this.sl_found = true;	
		this.ttBudget += leg.getTravTime();
		this.totalTravelDistance += leg.getRoute().getDist();

		//log.info("found s/l act");	
	}
	
	public void hweActivityFound(Act act, Leg leg) {
		//log.info("found hwe act");	
		// close chain
		if (chainStarted) {
			if (sl_found) {
				this.subChains.get(subChainIndex).setTotalTravelDistance(this.totalTravelDistance);
				this.subChains.get(subChainIndex).setTtBudget(this.ttBudget);
				this.subChains.get(subChainIndex).setEndCoord(act.getCoord());
				this.subChains.get(subChainIndex).setLastPrimAct(act);
			}
			else {
				this.subChains.remove(subChainIndex);
				this.subChainIndex--;
				//log.info("chain removed");
			}
		}
		
		// it is not the second home act
		if (!(leg == null)) {
			//open chain
			this.subChains.add(new SubChain());
			this.subChainIndex++;
			this.subChains.get(subChainIndex).setFirstPrimAct(act);
			this.subChains.get(subChainIndex).setStartCoord(act.getCoord());
			this.chainStarted = true;
			this.sl_found = false;
			this.ttBudget = leg.getTravTime();
			this.totalTravelDistance = leg.getRoute().getDist();
			//log.info("chain startet");
		}			
	}

	public List<SubChain> getSubChains() {
		return subChains;
	}

	public void setSubChains(List<SubChain> subChains) {
		this.subChains = subChains;
	}		
}
