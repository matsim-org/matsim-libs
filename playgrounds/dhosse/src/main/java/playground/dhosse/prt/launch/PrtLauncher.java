package playground.dhosse.prt.launch;

public class PrtLauncher {
	
	private final static String path = "C:/Users/Daniel/Dropbox/dvrpTestRun/";
//	private static final String netFile = path + "testNetwork.xml";
	private static final String netFile = path + "network_mb.xml";
//	private static final String plansFile = path + "testPlans.xml";
	private static final String plansFile = path + "population_1000_per_hour_each_from_6_to_10.xml";
//	private static final String plansFile = path + "testPlans_mb.xml";
	private static final String eventsFileName = "events.xml";
	
	public static void main(String args[]){
		
		VrpLauncher launcher = new VrpLauncher(netFile, plansFile, eventsFileName);
		launcher.run();
        
	}
	
}
