package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.List;


public abstract class NetworksStep {

	//Attributes

	protected MatchingComposedNetwork networkA;

	protected MatchingComposedNetwork networkB;

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

	public MatchingComposedNetwork getNetworkA() {
		return networkA;
	}

	public MatchingComposedNetwork getNetworkB() {
		return networkB;
	}

	public MatchingComposedNetwork[] execute(MatchingComposedNetwork networkA, MatchingComposedNetwork networkB) {
		MatchingComposedNetwork[] networks = new MatchingComposedNetwork[] {networkA, networkB};
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

	protected abstract MatchingComposedNetwork[] execute();
	
	
}
