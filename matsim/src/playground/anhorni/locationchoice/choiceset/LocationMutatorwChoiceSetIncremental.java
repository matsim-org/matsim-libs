package playground.anhorni.locationchoice.choiceset;



import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;


public class LocationMutatorwChoiceSetIncremental extends LocationMutatorwChoiceSet {
	
	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSetIncremental.class);
	
	public LocationMutatorwChoiceSetIncremental(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	@Override
	protected boolean handleSubChain(SubChain subChain, double speed, int trialNr) {
		log.info("handleSubChain");
		return true;
	}
}
