package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;


public abstract class NetworksStep {

	//Attributes

	protected Network networkA;

	protected Network networkB;

	protected Region region;
	
	protected final List<NetworksStep> networkSteps;
	
	protected int internalStepPosition;
	
	private String name;

	//Methods

	public NetworksStep(String name, Region region) {
		this.name = name;
		this.region = region;
		networkSteps = new ArrayList<NetworksStep>();
		internalStepPosition = 0;
	}

	public Network getNetworkA() {
		return networkA;
	}

	public Network getNetworkB() {
		return networkB;
	}
	
	public List<NetworksStep> getNetworkSteps() {
		return networkSteps;
	}
	
	public int getInternalStepPosition() {
		return internalStepPosition;
	}

	public void setInternalStepPosition(int internalStepPosition) {
		this.internalStepPosition = internalStepPosition;
	}

	public Network[] execute(Network networkA, Network networkB) {
		Network[] networks = new Network[] {networkA, networkB};
		int i = 0;
		do{
			if(i==internalStepPosition) {
				System.out.println("Execute: "+name);
				this.networkA = networks[0];
				this.networkB = networks[1];
				networks = execute();
			}
			if(i<networkSteps.size())
				networks = networkSteps.get(i).execute(networks[0], networks[1]);
			i++;
		} while(i<networkSteps.size() || i==internalStepPosition);
		return networks;
	}

	protected Network[] execute() {
		Network oldNetworkA = NetworkImpl.createNetwork();
		Network oldNetworkB = NetworkImpl.createNetwork();
		saveOldNetworks(oldNetworkA, oldNetworkB);
		process(oldNetworkA, oldNetworkB);
		Network[] reduced = new Network[] {networkA, networkB};
		networkA = oldNetworkA;
		networkB = oldNetworkB;
		return reduced;
	}
	
	protected abstract void process(Network oldNetworkA, Network oldNetworkB);
	
	protected void saveOldNetworks(Network oldNetworkA, Network oldNetworkB) {
		for(Node node:networkA.getNodes().values()) {
			Node newNode = new ComposedNode(node);
			oldNetworkA.addNode(newNode);
		}
		for(Link link:networkA.getLinks().values()) {
			MatchingComposedLink composedLink = new MatchingComposedLink(link.getId(), oldNetworkA.getNodes().get(link.getFromNode().getId()), oldNetworkA.getNodes().get(link.getToNode().getId()), oldNetworkA);
			oldNetworkA.addLink(composedLink);
		}
		for(Node node:networkB.getNodes().values()) {
			Node newNode = new ComposedNode(node);
			oldNetworkB.addNode(newNode);
		}
		for(Link link:networkB.getLinks().values()) {
			MatchingComposedLink composedLink = new MatchingComposedLink(link.getId(), oldNetworkB.getNodes().get(link.getFromNode().getId()), oldNetworkB.getNodes().get(link.getToNode().getId()), oldNetworkA);
			oldNetworkB.addLink(composedLink);
		}
	}
	
	
}
