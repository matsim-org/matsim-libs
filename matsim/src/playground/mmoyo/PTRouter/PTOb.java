package playground.mmoyo.PTRouter;

import playground.mmoyo.input.PTNetworkFactory;
import org.matsim.core.network.NetworkLayer;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;

/**
 * Contains the common objects for the route search: network, logicNet, PTTimetable, PtRouter
 */
public class PTOb {
	private NetworkLayer networkLayer; 
	public  PTRouter ptRouter;  //-->: make private
	private PTTimeTable ptTimeTable;

	private PTNetworkFactory ptNetworkFactory =new PTNetworkFactory();
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
	    this.ptTimeTable = new PTTimeTable(timeTableFile);
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
		ptRouter = new PTRouter(networkLayer, ptTimeTable);
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

	public PTRouter getPtRouter() {
		return ptRouter;
	}

	public PTTimeTable getPtTimeTable() {
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

	public PTNetworkFactory getPtNetworkFactory() {
		return ptNetworkFactory;
	}

	public void setPtNetworkLayer(final NetworkLayer ptNetworkLayer) {
		this.networkLayer = ptNetworkLayer;
	}

	public void setPtRouter2(final PTRouter ptRouter) {
		this.ptRouter = ptRouter;
	}	
		
	public void setPTTimeTable(final PTTimeTable ptTimeTable) {
		this.ptTimeTable= ptTimeTable;
	}


}
