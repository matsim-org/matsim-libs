package playground.mmoyo.PTRouter;

import playground.mmoyo.input.PTNetworkFactory2;
import org.matsim.core.network.NetworkLayer;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;

/**
 * Contains the common objects for the route search: network, logicNet, PTTimetable, PtRouter
 */
public class PTOb {
	private NetworkLayer networkLayer; 
	public  PTRouter2 ptRouter2;  //-->: make private
	private PTTimeTable2 ptTimeTable;

	private PTNetworkFactory2 ptNetworkFactory =new PTNetworkFactory2();
	private String outPutPlanFile; 
	private String config;
	private String plansFile;
	private String ptNetFile; 

	@Deprecated
	public PTOb(String configFile, String ptNetFileName, String timeTableFile, String plansFile, String outPutPlansFile){
		this.outPutPlanFile= outPutPlansFile;
		this.config = configFile;
	    this.plansFile= plansFile;
	    this.ptNetFile= ptNetFileName;
	    this.ptTimeTable = new PTTimeTable2(timeTableFile);
	}

	public PTOb(String configFile, String ptNetFileName, String plansFile, String outPutPlansFile){
		this.outPutPlanFile= outPutPlansFile;
		this.config = configFile;
	    this.plansFile= plansFile;
	    this.ptNetFile= ptNetFileName;
	}
	
	public void readPTNet(String netFile){
		this.networkLayer = this.ptNetworkFactory.readNetwork(netFile,ptTimeTable);
		createRouter();
	}
	
	public void createRouter(){
		ptRouter2 = new PTRouter2(networkLayer, ptTimeTable);
	}
	
	public void createPTNetWithTLinks(String inNetFile){
		this.networkLayer = ptNetworkFactory.createNetwork(inNetFile, this.ptTimeTable, ptNetFile);
	
	}

	public void writeNet(String outNetFile){
		this.ptNetworkFactory.writeNet(networkLayer, outNetFile);
	}
	
	public NetworkLayer getPtNetworkLayer() {
		return networkLayer;
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

	public void setPtNetworkLayer(final NetworkLayer ptNetworkLayer) {
		this.networkLayer = ptNetworkLayer;
	}

	public void setPtRouter2(final PTRouter2 ptRouter2) {
		this.ptRouter2 = ptRouter2;
	}	
		
	public void setPTTimeTable(final PTTimeTable2 ptTimeTable) {
		this.ptTimeTable= ptTimeTable;
	}


}
