package playground.anhorni.locationchoice.cs.helper;

import org.matsim.interfaces.core.v01.Activity;

public class Trip {
	
	int tripNr;
	Activity beforeShoppingAct = null;
	// TODO: handle multiple shopping acts
	Activity shoppingAct = null;
	Activity afterShoppingAct = null;
	
	public Trip(int tripNr, Activity beforeShoppingAct, Activity shoppingAct,
			Activity afterShoppingAct) {
		super();
		this.tripNr = tripNr;
		this.beforeShoppingAct = beforeShoppingAct;
		this.shoppingAct = shoppingAct;
		this.afterShoppingAct = afterShoppingAct;
	}
		
	// --------------------------------------------------------
	public Activity getBeforeShoppingAct() {
		return beforeShoppingAct;
	}
	public void setBeforeShoppingAct(Activity beforeShoppingAct) {
		this.beforeShoppingAct = beforeShoppingAct;
	}
	public Activity getShoppingAct() {
		return shoppingAct;
	}
	public void setShoppingAct(Activity shoppingAct) {
		this.shoppingAct = shoppingAct;
	}
	public Activity getAfterShoppingAct() {
		return afterShoppingAct;
	}
	public void setAfterShoppingAct(Activity afterShoppingAct) {
		this.afterShoppingAct = afterShoppingAct;
	}
	public int getTripNr() {
		return tripNr;
	}
	public void setTripNr(int tripNr) {
		this.tripNr = tripNr;
	}

}
