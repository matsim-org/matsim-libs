package playground.toronto;


/**
 * A controller for doing a Toronto MATSim run. Hopefully, THE definitive controller for MATSim in the GTHA!
 * 
 * Currently blank!
 * 
 * @author pkucirek
 *
 */
public class RunToronto {
	
	/*
	 * General structure as follows:
	 * 
	 * 1. Load Config file
	 * 2. Load base network
	 * 3. Load transit network
	 * 4. Load ancillary tables (ie, for transit fares)
	 * 5. Load population (ie, plans)
	 * ...
	 * ...Disable plan mutator (since we are not modifying agents' plans)
	 * ...Set the transit fare router (optional)
	 * ...Set up specific listeners/event handlers (optional) for analysis
	 * [Last] Run Matsim
	 */

}
