package playground.anhorni.locationchoice.cs.helper;

import org.matsim.interfaces.core.v01.Act;

public class Trip {
	
	int tripNr;
	Act beforeShoppingAct = null;
	// TODO: handle multiple shopping acts
	Act shoppingAct = null;
	Act afterShoppingAct = null;
	
	public Trip(int tripNr, Act beforeShoppingAct, Act shoppingAct,
			Act afterShoppingAct) {
		super();
		this.tripNr = tripNr;
		this.beforeShoppingAct = beforeShoppingAct;
		this.shoppingAct = shoppingAct;
		this.afterShoppingAct = afterShoppingAct;
	}
		
	// --------------------------------------------------------
	public Act getBeforeShoppingAct() {
		return beforeShoppingAct;
	}
	public void setBeforeShoppingAct(Act beforeShoppingAct) {
		this.beforeShoppingAct = beforeShoppingAct;
	}
	public Act getShoppingAct() {
		return shoppingAct;
	}
	public void setShoppingAct(Act shoppingAct) {
		this.shoppingAct = shoppingAct;
	}
	public Act getAfterShoppingAct() {
		return afterShoppingAct;
	}
	public void setAfterShoppingAct(Act afterShoppingAct) {
		this.afterShoppingAct = afterShoppingAct;
	}
	public int getTripNr() {
		return tripNr;
	}
	public void setTripNr(int tripNr) {
		this.tripNr = tripNr;
	}

}
