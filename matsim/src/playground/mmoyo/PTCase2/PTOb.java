package playground.mmoyo.PTCase2;

import org.matsim.network.NetworkLayer;

public class PTOb {
	private NetworkLayer ptNetworkLayer; 
	private PTRouter2 ptRouter2;
	private PTTimeTable2 ptTimeTable;
	private String outPutFile; 
	private String config;
	private String plansFile;
	private PTNetworkFactory2 ptNetworkFactory =new PTNetworkFactory2();
	
	public PTOb(String configFile, String netFile, String timeTableFile, String plansFile, String outPutPlans){
		this.ptTimeTable = new PTTimeTable2(timeTableFile);
		ptNetworkLayer = ptNetworkFactory.createNetwork(netFile, ptTimeTable);
		ptRouter2 = new PTRouter2(ptNetworkLayer, ptTimeTable);
		this.outPutFile= outPutPlans;
		this.config = configFile;
	    this.plansFile= plansFile;
	}

	public NetworkLayer getPtNetworkLayer() {
		return ptNetworkLayer;
	}

	public PTRouter2 getPtRouter2() {
		return ptRouter2;
	}

	public PTTimeTable2 getPtTimeTable() {
		return ptTimeTable;
	}

	public String getOutPutFile() {
		return outPutFile;
	}

	public String getConfig() {
		return config;
	}

	public String getPlansFile() {
		return plansFile;
	}

	public PTNetworkFactory2 getPtNetworkFactory() {
		return ptNetworkFactory;
	}	
}
