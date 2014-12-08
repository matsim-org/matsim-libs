package playground.toronto.sotr.config;

import org.matsim.core.config.ConfigGroup;

public class SOTRConfig extends ConfigGroup {
	
	public static final String GROUP_NAME = "secondOrderTransitRouter";

	static final String SEARCH_RADIUS = "searchRadius";
	static final String EXTENSION_RADIUS = "extensionRadius";
	static final String ADDITIONAL_TRANSFER_TIME = "additionalTransferTime";
	
	public double searchRadius = 1000.0;
	public double extensionRadius = 200.0;
	public double additionalTransferTime = 0.0;
	public double beelineWalkSpeed_m_s = 1.0;
	
	public SOTRConfig(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

}
