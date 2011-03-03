package playground.mzilske.freight;

public class NoOffer extends Offer {
	
	public NoOffer(){
		super.setPrice(Double.MAX_VALUE);
	}
}
