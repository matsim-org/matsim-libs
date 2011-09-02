package playground.sergioo.NetworksMatcher.kernel.core;


public abstract class NetworksStep {

	//Attributes

	protected MatchingComposedNetwork networkA;

	protected MatchingComposedNetwork networkB;

	protected Region region;

	//Methods

	public NetworksStep(Region region) {
		this.region = region;
	}

	public MatchingComposedNetwork getNetworkA() {
		return networkA;
	}

	public MatchingComposedNetwork getNetworkB() {
		return networkB;
	}
	
	public void setNetworks() {
		
	}

	public MatchingComposedNetwork[] execute(MatchingComposedNetwork networkA, MatchingComposedNetwork networkB) {
		this.networkA = networkA;
		this.networkB = networkB;
		return execute();
	}

	protected abstract MatchingComposedNetwork[] execute();
	
	
}
