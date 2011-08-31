package playground.sergioo.NetworksMatcher.kernel.core;


public abstract class NetworksStep {

	//Attributes

	protected ComposedNetwork networkA;

	protected ComposedNetwork networkB;

	protected Region region;

	//Methods

	public NetworksStep(Region region) {
		this.region = region;
	}

	public ComposedNetwork getNetworkA() {
		return networkA;
	}

	public ComposedNetwork getNetworkB() {
		return networkB;
	}
	
	public void setNetworks() {
		
	}

	public ComposedNetwork[] execute(ComposedNetwork networkA, ComposedNetwork networkB) {
		this.networkA = networkA;
		this.networkB = networkB;
		return execute();
	}

	protected abstract ComposedNetwork[] execute();
	
	
}
