package playground.sebhoerl.av.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

import playground.sebhoerl.av.framework.AVModule.DispatcherOption;
import playground.sebhoerl.av.framework.AVModule.DistributionStrategyOption;

public class AVConfigGroup extends ReflectiveConfigGroup {
    enum RouterOption {
        Freespeed, Congestion, Randomized, FlowDensity, Speed, FlowDensityRandomized
    }
    
	private int numberOfVehicles = 0;
	private DistributionStrategyOption distributionStrategy = DistributionStrategyOption.Random;
	private DispatcherOption dispatcher = DispatcherOption.FIFO;
	private InteractionConfig interaction = new InteractionConfig();
	private int dumpServicesInterval = 0;
	private double marginalUtilityOfWaiting = 0.0;
	private RouterOption router = RouterOption.Freespeed;
	
	private String mutatorPath = null;
	
	public AVConfigGroup() {
		super("av");
	}
	
    @StringSetter( "marginalUtilityOfWaiting" )
    public void setMarginalUtilityOfWaiting(double marginalUtilityOfWaiting) {
        this.marginalUtilityOfWaiting = marginalUtilityOfWaiting;
    }
    
    @StringGetter( "marginalUtilityOfWaiting" )
    public double getMarginalUtilityOfWaiting() {
        return this.marginalUtilityOfWaiting;
    }
    
    public double getMarginalUtilityOfWaiting_s() {
        return getMarginalUtilityOfWaiting() / 3600.0;
    }
	
	@StringSetter( "numberOfVehicles" )
	public void setNumberOfVehicles(int numberOfVehicles) {
		this.numberOfVehicles = numberOfVehicles;
	}
	
	@StringGetter( "numberOfVehicles" )
	public int getNumberOfVehicles() {
		return this.numberOfVehicles;
	}
	
	@StringGetter( "distributionStrategy" )
	DistributionStrategyOption getDistributionStrategy() {
		return this.distributionStrategy;
	}
	
	@StringSetter( "distributionStrategy" )
	void setDistributionStrategy(DistributionStrategyOption distributionStrategy) {
		this.distributionStrategy = distributionStrategy;
	}
	
	@StringGetter( "dispatcher" )
	DispatcherOption getDispatcher() {
		return this.dispatcher;
	}
	
	@StringSetter( "dispatcher" )
	void setDispatcher(DispatcherOption dispatcher) {
		this.dispatcher = dispatcher;
	}
	
    @StringGetter( "router" )
    RouterOption getRouter() {
        return this.router;
    }
    
    @StringSetter( "router" )
    void setRouter(RouterOption router) {
        this.router = router;
    }
	
	@StringGetter( "pickupDuration" )
	double getPickupDuration() {
		return interaction.getPickupDuration();
	}
	
	@StringSetter( "pickupDuration" )
	void setPickupDuration(double pickupDuration) {
		interaction.setPickupDuration(pickupDuration);
	}
	
	@StringGetter( "dropoffDuration" )
	double getDropoffDuration() {
		return interaction.getDropoffDuration();
	}
	
	@StringSetter( "dropoffDuration" )
	void setDropoffDuration(double dropoffDuration) {
		interaction.setDropoffDuration(dropoffDuration);
	}
	
	InteractionConfig getInteractionConfig() {
		return interaction;
	}

	@StringGetter( "dumpServicesInterval" )
	public int getDumpServicesInterval() {
		return dumpServicesInterval;
	}
	
	@StringSetter( "dumpServicesInterval" )
	public void setDumpServicesInterval(int dumpServicesInterval) {
		this.dumpServicesInterval = dumpServicesInterval;
	}
	
    @StringGetter( "mutatorPath" )
    public String getMutatorPath() {
        return mutatorPath;
    }
    
    @StringSetter( "mutatorPath" )
    public void setMutatorPath(String mutatorPath) {
        this.mutatorPath = mutatorPath;
    }
}
