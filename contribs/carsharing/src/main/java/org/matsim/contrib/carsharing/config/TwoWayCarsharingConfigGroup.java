package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class TwoWayCarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "TwoWayCarsharing";

	private Double searchDistance = null;
	
	private boolean useTwoWayCarsharing = false;
	
	public TwoWayCarsharingConfigGroup() {
		super(GROUP_NAME);
	}	
	
	@StringGetter( "searchDistanceTwoWayCarsharing" )
	public Double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( "searchDistanceTwoWayCarsharing" )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}	
	
	@StringGetter( "useTwoWayCarsharing" )
	public boolean useTwoWayCarsharing() {
		return this.useTwoWayCarsharing;
	}

	@StringSetter( "useTwoWayCarsharing" )
	public void setUseTwoWayCarsharing(final boolean useTwoWayCarsharing) {
		this.useTwoWayCarsharing = useTwoWayCarsharing;
	}
	
	
}

