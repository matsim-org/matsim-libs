package org.matsim.contrib.carsharing.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class TwoWayCarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "TwoWayCarsharing";
	
	private static final String PARAM_SEARCH_DISTANCE = "searchDistanceTwoWayCarsharing";
	private static final String PARAM_USE_TWOWAY = "useTwoWayCarsharing";
	
	private Double searchDistance = 500.0;
	
	private boolean useTwoWayCarsharing = false;
	
	public TwoWayCarsharingConfigGroup() {
		super(GROUP_NAME);
	}	
	
	@StringGetter( PARAM_SEARCH_DISTANCE )
	public Double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( PARAM_SEARCH_DISTANCE )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}	
	
	@StringGetter( PARAM_USE_TWOWAY )
	public boolean useTwoWayCarsharing() {
		return this.useTwoWayCarsharing;
	}

	@StringSetter( PARAM_USE_TWOWAY )
	public void setUseTwoWayCarsharing(final boolean useTwoWayCarsharing) {
		this.useTwoWayCarsharing = useTwoWayCarsharing;
	}
	
	@Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PARAM_SEARCH_DISTANCE, "Defines the search radius for two-way vehicles. Default is 500 meters.");
        map.put(PARAM_USE_TWOWAY, "Defines if the two-way carsharing should be a mode alternative. Default is false.");
         
        return map;
    }
}

