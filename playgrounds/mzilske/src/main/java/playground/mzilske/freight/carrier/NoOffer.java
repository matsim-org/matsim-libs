package playground.mzilske.freight.carrier;


public class NoOffer extends CarrierOffer {
	
	public NoOffer(){
		super.setPrice(Double.MAX_VALUE);
	}
}
