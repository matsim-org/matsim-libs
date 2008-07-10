package playground.anhorni.locationchoice.choiceset;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.plans.Act;

public class ManageSubchains {
	
	private static final Logger log = Logger.getLogger(ManageSubchains.class);
	
	private List<SubChain> subChains = new Vector<SubChain>();
	
	private int subChainIndex = -1;
	boolean chainStarted = false;
	boolean sl_found = false;

	private double actDur = 0.0;
	private double slStartTime = 0.0;
	private double slEndTime = 0.0;
	
	public void slActivityFound(Act act) {
		// no plan starts with s or l !
		this.subChains.get(subChainIndex).addAct(act);
		this.sl_found = true;	
		this.actDur += act.getDur();

		log.info("found s/l act");	
	}
	
	public void hweActivityFound(Act act, boolean lastAct) {

		log.info("found hwe act");
		
		// close chain
		if (chainStarted) {
			if (sl_found) {
				this.slEndTime = act.getStartTime();
				log.info(slEndTime-slStartTime - actDur);
				this.subChains.get(subChainIndex).setTtBudget(slEndTime-slStartTime - actDur);
				this.subChains.get(subChainIndex).setEndCoord(act.getCoord());
			}
			else {
				this.subChains.remove(subChainIndex);
				this.subChainIndex--;
				log.info("chain removed");
			}
		}
		
		if (!lastAct) {
			//open chain
			this.subChains.add(new SubChain());
			this.subChainIndex++;
			this.subChains.get(subChainIndex).setFirstPrimAct(act);
			this.subChains.get(subChainIndex).setStartCoord(act.getCoord());
			this.chainStarted = true;
			this.sl_found = false;
			this.actDur = 0.0;
			log.info("chain startet");
		}			
	}

	public List<SubChain> getSubChains() {
		return subChains;
	}

	public void setSubChains(List<SubChain> subChains) {
		this.subChains = subChains;
	}		
}
