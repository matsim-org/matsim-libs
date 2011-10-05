package freight.vrp;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CompleteDigraphNetworkBuilder {
	
	private int top;
	
	private int left;
	
	private int bottom;
	
	private int right;
	
	private Scenario scenario;
	
	private int nextLinkId = 0;

	private int scale;
	
	public CompleteDigraphNetworkBuilder(Scenario scenario, int scale, int top, int left, int bottom, int right) {
		super();
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.scenario = scenario;
		this.scale = scale;
	}
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scen = ScenarioUtils.createScenario(config);
		CompleteDigraphNetworkBuilder netCreator = new CompleteDigraphNetworkBuilder(scen, 1000, 100, 0, 0, 100);
		new NetworkWriter(netCreator.createNetwork()).write("vrp/completeDigraphNetwork.xml");
	}
	
	public Network createNetwork(){
		Network network = scenario.getNetwork();
		
		for(int i=left*scale;i<=right*scale;i=i+scale){
			for(int j=bottom*scale;j<=top*scale;j=j+scale){
				Node node =  createAndAddNode(network, i, j);
				if(i>left && j>bottom){
					Node otherNode = createAndAddNode(network, i-scale, j-scale);
					createAndAddLinks(network, node, otherNode);
				}
				if(j>0){
					Node otherNode = createAndAddNode(network, i, j-scale);
					createAndAddLinks(network, node, otherNode);
				}
				if(i>0){
					Node otherNode = createAndAddNode(network, i-scale, j);
					createAndAddLinks(network, node, otherNode);
				}
				if(j<top*scale && i>bottom){
					Node otherNode = createAndAddNode(network, i-scale, j+scale);
					createAndAddLinks(network, node, otherNode);
				}
				if(i<right*scale && j>left){
					Node otherNode = createAndAddNode(network, i+scale, j-scale);
					createAndAddLinks(network, node, otherNode);
				}
			}
		}
		return network;	
	}

	private void createAndAddLinks(Network network, Node node, Node otherNode) {
		Link link = network.getFactory().createLink(getLinkId(), otherNode, node);
		setAttributes(link,otherNode.getCoord(),node.getCoord());
		Link linkR = network.getFactory().createLink(getLinkId(), node, otherNode);
		setAttributes(linkR,node.getCoord(),otherNode.getCoord());
		network.addLink(link);
		network.addLink(linkR);
	}

	private Node createAndAddNode(Network network, int i, int j) {
		Node otherNode = null;
		if(network.getNodes().containsKey(makeId(i-1,j-1))){
			otherNode = network.getNodes().get(makeId(i-1,j-1));
		}
		else{
			otherNode = network.getFactory().createNode(makeId(i-1,j-1), makeCoord(i-1, j-1));
			network.addNode(otherNode);
		}
		return otherNode;
	}

	private Id makeId(int i, int j) {
		return new IdImpl("n("+i+","+j+")");
	}

	private void setAttributes(Link link, Coord fromNode, Coord toNode) {
		double distance = CoordUtils.calcDistance(fromNode, toNode);
		link.setLength(distance*1000);
		link.setFreespeed(1);
		link.setCapacity(1000);
		
	}

	private Id getLinkId(){
		Id linkId = new IdImpl(nextLinkId);
		nextLinkId++;
		return linkId;
	}

	private Coord makeCoord(int i, int j) {
		return new CoordImpl(i,j);
	}

}
