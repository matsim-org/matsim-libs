package playground.mmoyo.input;

import java.util.ArrayList;
import java.util.List;
import org.matsim.core.network.NetworkLayer;
import org.matsim.api.basic.v01.network.BasicNode;

import playground.mmoyo.PTCase2.PTTimeTable2;

public class PTLineAggregator {
	private NetworkLayer net; 
	private PTTimeTable2 timeTable;
	private String filePath;
	
	public PTLineAggregator(String filePath, NetworkLayer net, PTTimeTable2 timeTable) {
		this.net=net;
		this.timeTable= timeTable;
		this.filePath=filePath;
	}
		
	public void AddLine(){
		PTNodeReader ptNodeReader = new PTNodeReader();
		ptNodeReader.readFile (filePath);
		
		List<List<BasicNode>> nodeListList = ptNodeReader.getNodeLists();
		PTNodeFactory ptNodeFactory = new PTNodeFactory(this.net, this.timeTable);
		PTLinkFactory ptLinkFactory= new PTLinkFactory (this.net);
	
		//System.out.println(nodeListList.size());
		
		int iniNodes= net.getNodes().size();
		int iniLinks= net.getLinks().size();
		System.out.println("creating new nodes and links...");
		
		BasicNode[] basicNodeArr = new BasicNode[2];
		for(List<BasicNode> basicNodeList: nodeListList){
			List<BasicNode> basicNodeList1 = new ArrayList<BasicNode>();
			List<BasicNode> basicNodeList2 = new ArrayList<BasicNode>();
			for (BasicNode basicNode : basicNodeList){
				basicNodeArr = ptNodeFactory.CreatePTNodes(basicNode);
				basicNodeList1.add(basicNodeArr[0]);
				basicNodeList2.add(basicNodeArr[1]);
			}
			ptLinkFactory.AddNewLinks(basicNodeList1);
			ptLinkFactory.AddNewLinks(basicNodeList2);
		}
		System.out.println("Done.");
		
		int finNodes= net.getNodes().size() - iniNodes;
		int finLinks= net.getLinks().size() - iniLinks;
	
		System.out.println("created Nodes:" + finNodes);
		System.out.println("created Links:" + finLinks);
		
		//-->and after creating the new nodes and links, we must create the transfers and detached again
	}
}
