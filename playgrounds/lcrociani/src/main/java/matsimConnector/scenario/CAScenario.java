package matsimConnector.scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import matsimConnector.network.HybridNetworkBuilder;
import matsimConnector.utility.Constants;
import matsimConnector.utility.MathUtility;
import matsimConnector.utility.IdUtility;
import matsimConnector.utility.LinkUtility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import pedCA.context.Context;
import pedCA.output.Log;

public class CAScenario {
	
	private boolean connected;
	private Scenario matsimScenario;
	private final Map<Id<CAEnvironment>,CAEnvironment> environments;
	private final Map<Link, CAEnvironment> linkToEnvironment;
		
	public CAScenario(){
		this.environments = new HashMap<Id<CAEnvironment>,CAEnvironment>();
		this.linkToEnvironment = new HashMap<Link, CAEnvironment>();
	}
	
	public CAScenario(String path){
		this();
		loadConfiguration(path);
	}
	
	public void initNetworks(){
		for (CAEnvironment environmentCA : environments.values())
			HybridNetworkBuilder.buildNetwork(environmentCA,this);
	}
	
	public void connect(Scenario matsimScenario){
		if (this.connected) {
			Log.warning("CA Scenario already connected!");
			return;
		}
		Log.log("Connecting CA scenario.");
		matsimScenario.addScenarioElement(Constants.CASCENARIO_NAME, this);
		this.matsimScenario = matsimScenario;
		Network scNet = matsimScenario.getNetwork();
		for (CAEnvironment environmentCA : environments.values()) 
			connect(environmentCA,scNet);
		this.connected = true;
	}
	
	private void loadConfiguration(String path){
		Context context;
		try {
			context = new Context(path);
			addCAEnvironment(new CAEnvironment(""+environments.size(), context));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void connect(CAEnvironment environmentCA, Network scNet) {
		Network envNet = environmentCA.getNetwork();
		for (Node nodeCA : envNet.getNodes().values()) {
			Node nodeMatsim = scNet.getNodes().get(nodeCA.getId());
			if (nodeMatsim == null) {
				nodeCA.getInLinks().clear();
				nodeCA.getOutLinks().clear();
				scNet.addNode(nodeCA);
				plugNode(nodeCA,scNet, environmentCA);
			}else{
				Log.warning("Node already present in the network!");
			}
		}
		for (Link link : envNet.getLinks().values()) {
			if (scNet.getLinks().get(link.getId()) != null) { 
				//don't create links that already exist
				continue;
			}
			Node nFrom = scNet.getNodes().get(link.getFromNode().getId());
			Node nTo = scNet.getNodes().get(link.getToNode().getId());
			if (link.getFromNode() != nFrom) {
				link.setFromNode(nFrom);
			}
			if (link.getToNode() != nTo) {
				link.setToNode(nTo);
			}
			scNet.addLink(link); 
		}
	}

	private void plugNode(Node n, Network scNet, CAEnvironment environmentCA) {
		Node pivot = null;
		double radius = .4;
		Set<String> modesToCA = new HashSet<String>();
		modesToCA.add("car");
		modesToCA.add("walk");
		modesToCA.add(Constants.TO_CA_LINK_MODE);
		Set<String> modesToQ = new HashSet<String>();
		modesToQ.add("car");
		modesToQ.add("walk");
		modesToQ.add(Constants.TO_Q_LINK_MODE);
		for (Node node : scNet.getNodes().values()){
			Log.log(n.getCoord()+" "+node.getCoord());
			if (node != n && MathUtility.EuclideanDistance(n.getCoord(),node.getCoord()) <= radius){
				pivot = node;
				break;
			}
		}
		if (pivot == null)
			return;
				
		Id<Node> fromId = pivot.getId();
		Id<Node> toId = n.getId();
		
		for(Link link : pivot.getOutLinks().values()){
			scNet.removeLink(link.getId());
			link.setFromNode(n);
			link.setLength(link.getLength()+Constants.TRANSITION_LINK_LENGTH);
			link.setAllowedModes(modesToQ);
			scNet.addLink(link);
			mapLinkToEnvironment(link, environmentCA);
		}
		
		//Set<String> modesToQ = new HashSet<String>();
		//modesToQ.add("car");
		//modesToQ.add("walk");
		//modesToQ.add(Constants.TO_Q_LINK_MODE);
		//Id <Link> toQId = IdUtility.createLinkId(toId, fromId);
		//Link toQ = scNet.getFactory().createLink(toQId, n, pivot);
		//LinkUtility.initLink(toQ, Constants.TRANSITION_LINK_LENGTH, modesToQ);
		//scNet.addLink(toQ);
		
		
		Id <Link> toCAId = IdUtility.createLinkId(fromId, toId);
		Link toCA = scNet.getFactory().createLink(toCAId, pivot, n);
		LinkUtility.initLink(toCA, Constants.TRANSITION_LINK_LENGTH, 10, modesToCA);
		scNet.addLink(toCA);
		mapLinkToEnvironment(toCA, environmentCA);
	}

	public Map<Id<CAEnvironment>, CAEnvironment> getEnvironments(){
		return environments;
	}
	
	public void addCAEnvironment(CAEnvironment environment) {
		this.environments.put(environment.getId(), environment);
	}
	
	public void mapLinkToEnvironment(Link link, CAEnvironment environmentCA){
		this.linkToEnvironment.put(link, environmentCA);
	}

	public CAEnvironment getCAEnvironment(Id<CAEnvironment> id) {
		return this.environments.get(id);
	}
	
	public Scenario getMATSimScenario() {
		return matsimScenario;
	}

	public CAEnvironment getCAEnvironment(Link link) {
		return linkToEnvironment.get(link);
	}
}