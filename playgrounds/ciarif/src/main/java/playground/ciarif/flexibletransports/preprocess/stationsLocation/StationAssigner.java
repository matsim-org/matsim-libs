package playground.ciarif.flexibletransports.preprocess.stationsLocation;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ciarif.flexibletransports.preprocess.membership.MembershipAssigner;

public class StationAssigner {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(StationAssigner.class);
	private MembershipAssigner membershipAssigner;
	
	public StationAssigner(MembershipAssigner membershipAssigner) {
		this.membershipAssigner = membershipAssigner;
	}

	private void init() {
		
	}

	public void run() {
		this.init();
		this.modifyStations();
	}

	private void modifyStations() {
		// TODO Auto-generated method stub
		
	}
	
	
}
