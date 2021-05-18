package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class OneWayCarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "OneWayCarsharing";
	
	private Double searchDistance = null;
	
	private boolean useOneWayCarsharing = false;
	
	public OneWayCarsharingConfigGroup() {
		super(GROUP_NAME);
	}			
	
	@StringGetter( "searchDistanceOneWayCarsharing" )
	public Double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( "searchDistanceOneWayCarsharing" )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}	
	
	@StringGetter( "useOneWayCarsharing" )
	public boolean useOneWayCarsharing() {
		return this.useOneWayCarsharing;
	}

	@StringSetter( "useOneWayCarsharing" )
	public void setUseOneWayCarsharing(final boolean useOneWayCarsharing) {
		this.useOneWayCarsharing = useOneWayCarsharing;
	}
	
}
