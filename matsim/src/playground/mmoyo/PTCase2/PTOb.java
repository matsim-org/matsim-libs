package playground.mmoyo.PTCase2;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkFactory;
import org.matsim.network.Node;
import org.matsim.network.Link;
import java.util.Iterator;
import java.util.Map;
import playground.mmoyo.PTRouter.PTNode;

public class PTOb {
	private NetworkLayer ptNetworkLayer; 
	private PTRouter2 ptRouter2;
	private PTTimeTable2 ptTimeTable;
	private String outPutPlanFile; 
	private String config;
	private String plansFile;
	private String ptNetFile; 
	private PTNetworkFactory2 ptNetworkFactory =new PTNetworkFactory2();
	
	public PTOb(String configFile, String inNetFileName, String ptNetFileName, String timeTableFile, String plansFile, String outPutPlansFile){
		this.outPutPlanFile= outPutPlansFile;
		this.config = configFile;
	    this.plansFile= plansFile;
	    this.ptNetFile= ptNetFileName;
	    this.ptTimeTable = new PTTimeTable2(timeTableFile);
	    this.ptNetworkLayer= this.ptNetworkFactory.readNetwork(ptNetFileName,ptTimeTable);
		ptRouter2 = new PTRouter2(ptNetworkLayer, ptTimeTable);
	}

	public void createPTNet(String inNetFile){
		this.ptNetworkLayer = ptNetworkFactory.createNetwork(inNetFile, this.ptTimeTable, ptNetFile);
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
}
