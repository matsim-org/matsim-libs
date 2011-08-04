package playground.mzilske.freight;

public class NoOffer extends CarrierOffer {
	
	public NoOffer(){
		super.setPrice(Double.MAX_VALUE);
	}
}
