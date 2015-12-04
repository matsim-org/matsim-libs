package playground.balac.induceddemand.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class ActivityStrategiesConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ActivityStrategies";
	
	private boolean useInsertActivityStrategy = false;
	private boolean useRemoveActivityStrategy = false;
	private boolean useSwapActivitiesStrategy = false;

	public ActivityStrategiesConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "useInsertActivityStrategy" )
	public boolean getUseInsertActivityStrategy() {
		return useInsertActivityStrategy;
	}
	
	@StringSetter( "useInsertActivityStrategy" )
	public void setUseInsertActivityStrategy(boolean useInsertActivityStrategy) {
		this.useInsertActivityStrategy = useInsertActivityStrategy;
	}
	
	@StringGetter( "useRemoveActivityStrategy" )
	public boolean getUseRemoveActivityStrategy() {
		return useRemoveActivityStrategy;
	}
	
	@StringSetter( "useRemoveActivityStrategy" )
	public void setUseRemoveActivityStrategy(boolean useRemoveActivityStrategy) {
		this.useRemoveActivityStrategy = useRemoveActivityStrategy;
	}
	
	@StringGetter( "useSwapActivitiesStrategy" )
	public boolean getUseSwapActivitiesStrategy() {
		return useSwapActivitiesStrategy;
	}
	
	@StringSetter( "useSwapActivitiesStrategy" )
	public void setUseSwapActivitiesStrategy(boolean useSwapActivitiesStrategy) {
		this.useSwapActivitiesStrategy = useSwapActivitiesStrategy;
	}

}
