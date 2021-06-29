package org.matsim.contrib.carsharing.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class OneWayCarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "OneWayCarsharing";

	private static final String PARAM_SEARCH_DISTANCE = "searchDistanceOneWayCarsharing";
	private static final String PARAM_USE_ONEWAY = "useOneWayCarsharing";

	private double searchDistance = 500.0;
	
	private boolean useOneWayCarsharing = false;
	
	public OneWayCarsharingConfigGroup() {
		super(GROUP_NAME);
	}			
	
	@StringGetter( PARAM_SEARCH_DISTANCE )
	public double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( PARAM_SEARCH_DISTANCE )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}	
	
	@StringGetter( PARAM_USE_ONEWAY )
	public boolean useOneWayCarsharing() {
		return this.useOneWayCarsharing;
	}

	@StringSetter( PARAM_USE_ONEWAY )
	public void setUseOneWayCarsharing(final boolean useOneWayCarsharing) {
		this.useOneWayCarsharing = useOneWayCarsharing;
	}
	
	@Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PARAM_SEARCH_DISTANCE, "Defines the search radius for one-way vehicles. Default is 500 meters.");
        map.put(PARAM_USE_ONEWAY, "Defines if the one-way carsharing should be a mode alternative. Default is false.");
         
        return map;
    }
	
}
