package playground.mmoyo.PTCase2;

import org.matsim.core.network.NetworkLayer;

import playground.mmoyo.input.PTNetworkFactory2;

/**
 * Contains the common objects for the route search: network, PTTimetable, PtRouter
 */
public class PTOb {
	private NetworkLayer ptNetworkLayer; 
	public  PTRouter2 ptRouter2;  //-->: make private
	private PTTimeTable2 ptTimeTable;
	private String outPutPlanFile; 
	private String config;
	private String plansFile;
	private String ptNetFile; 
	private PTNetworkFactory2 ptNetworkFactory =new PTNetworkFactory2();
	
	public PTOb(String configFile, String ptNetFileName, String timeTableFile, String plansFile, String outPutPlansFile){
		this.outPutPlanFile= outPutPlansFile;
		this.config = configFile;
	    this.plansFile= plansFile;
	    this.ptNetFile= ptNetFileName;
	    this.ptTimeTable = new PTTimeTable2(timeTableFile);
	}

	public void readPTNet(String netFile){
		this.ptNetworkLayer= this.ptNetworkFactory.readNetwork(netFile,ptTimeTable);
		createRouter();
	}
	
	public void createRouter(){
		ptRouter2 = new PTRouter2(ptNetworkLayer, ptTimeTable);
	}
	
	public void createPTNetWithTLinks(String inNetFile){
		this.ptNetworkLayer = ptNetworkFactory.createNetwork(inNetFile, this.ptTimeTable, ptNetFile);
	}

	public void writeNet(String outNetFile){
		this.ptNetworkFactory.writeNet(this.ptNetworkLayer, outNetFile);
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
		return outPutPlanFile;
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

	public void setPtNetworkLayer(NetworkLayer ptNetworkLayer) {
		this.ptNetworkLayer = ptNetworkLayer;
	}

	public void setPtRouter2(PTRouter2 ptRouter2) {
		this.ptRouter2 = ptRouter2;
	}	
		
	public void setPTTimeTable(PTTimeTable2 ptTimeTable) {
		this.ptTimeTable= ptTimeTable;
	}	
}
