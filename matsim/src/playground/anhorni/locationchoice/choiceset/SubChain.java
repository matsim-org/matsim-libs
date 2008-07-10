package playground.anhorni.locationchoice.choiceset;

import java.util.List;
import java.util.Vector;

import org.matsim.plans.Act;
import org.matsim.utils.geometry.CoordI;

public class SubChain {
	
	private Act firstPrimAct = null;
	private Act lastPrimAct = null;
	
	private List<Act> slActs = null;
	private double ttBudget = 0.0;
	
	private CoordI startCoord = null;
	private CoordI endCoord = null;

	
	public SubChain() {
		slActs = new Vector<Act>();		
	}
	
	public void addAct(Act act) {
		this.slActs.add(act);
	}


	public double getTtBudget() {
		return ttBudget;
	}


	public void setTtBudget(double ttBudget) {
		this.ttBudget = ttBudget;
	}

	public CoordI getStartCoord() {
		return startCoord;
	}

	public void setStartCoord(CoordI startCoord) {
		this.startCoord = startCoord;
	}

	public CoordI getEndCoord() {
		return endCoord;
	}

	public void setEndCoord(CoordI endCoord) {
		this.endCoord = endCoord;
	}

	public List<Act> getSlActs() {
		return slActs;
	}

	public void setSlActs(List<Act> slActs) {
		this.slActs = slActs;
	}

	public Act getFirstPrimAct() {
		return firstPrimAct;
	}

	public void setFirstPrimAct(Act firstPrimAct) {
		this.firstPrimAct = firstPrimAct;
	}

	public Act getLastPrimAct() {
		return lastPrimAct;
	}

	public void setLastPrimAct(Act lastPrimAct) {
		this.lastPrimAct = lastPrimAct;
	}
		
}
