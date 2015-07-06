package playground.balac.allcsmodestest.config;

import org.matsim.core.config.ReflectiveConfigGroup;


public class AllCSModesConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "AllCSModes";
	
	private String statsWriterFrequency = null;
	
	

	
	public AllCSModesConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "statsWriterFrequency" )
	public String getStatsWriterFrequency() {
		return this.statsWriterFrequency;
	}

	@StringSetter( "statsWriterFrequency" )
	public void setStatsWriterFrequency(final String statsWriterFrequency) {
		this.statsWriterFrequency = statsWriterFrequency;
	}
	
}
