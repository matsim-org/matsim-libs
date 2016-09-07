package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class OneWayCarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "OneWayCarsharing";
	
	private String travelingOneWayCarsharing = null;
	
	private String constantOneWayCarsharing = null;
	
	private String vehiclelocationsInputFile = null;
	
	private Double searchDistance = null;
	
	private String rentalPriceTimeOneWayCarsharing = null;
	
	private String timeFeeOneWayCarsharing = null;
	
	private String timeParkingFeeOneWayCarsharing = null;

	private String distanceFeeOneWayCarsharing = null;
	
	private boolean useOneWayCarsharing = false;
	
	public OneWayCarsharingConfigGroup() {
		super(GROUP_NAME);
	}
		
	@StringGetter( "travelingOneWayCarsharing" )
	public String getUtilityOfTravelling() {
		return this.travelingOneWayCarsharing;
	}

	@StringSetter( "travelingOneWayCarsharing" )
	public void setUtilityOfTravelling(final String travelingOneWayCarsharing) {
		this.travelingOneWayCarsharing = travelingOneWayCarsharing;
	}

	@StringGetter( "constantOneWayCarsharing" )
	public String constantOneWayCarsharing() {
		return this.constantOneWayCarsharing;
	}

	@StringSetter( "constantOneWayCarsharing" )
	public void setConstantOneWayCarsharing(final String constantOneWayCarsharing) {
		this.constantOneWayCarsharing = constantOneWayCarsharing;
	}
	
	@StringGetter( "rentalPriceTimeOneWayCarsharing" )
	public String getRentalPriceTimeOneWayCarsharing() {
		return this.rentalPriceTimeOneWayCarsharing;
	}

	@StringSetter( "rentalPriceTimeOneWayCarsharing" )
	public void setRentalPriceTimeOneWayCarsharing(final String rentalPriceTimeOneWayCarsharing) {
		this.rentalPriceTimeOneWayCarsharing = rentalPriceTimeOneWayCarsharing;
	}
	
	@StringGetter( "vehiclelocationsOneWayCarsharing" )
	public String getvehiclelocations() {
		return this.vehiclelocationsInputFile;
	}

	@StringSetter( "vehiclelocationsOneWayCarsharing" )
	public void setvehiclelocations(final String vehiclelocationsInputFile) {
		this.vehiclelocationsInputFile = vehiclelocationsInputFile;
	}
	
	@StringGetter( "searchDistanceOneWayCarsharing" )
	public Double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( "searchDistanceOneWayCarsharing" )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}
	
	@StringGetter( "timeFeeOneWayCarsharing" )
	public String timeFeeOneWayCarsharing() {
		return this.timeFeeOneWayCarsharing;
	}

	@StringSetter( "timeFeeOneWayCarsharing" )
	public void setTimeFeeOneWayCarsharing(final String timeFeeOneWayCarsharing) {
		this.timeFeeOneWayCarsharing = timeFeeOneWayCarsharing;
	}
	
	@StringGetter( "timeParkingFeeOneWayCarsharing" )
	public String timeParkingFeeOneWayCarsharing() {
		return this.timeParkingFeeOneWayCarsharing;
	}

	@StringSetter( "timeParkingFeeOneWayCarsharing" )
	public void setTimeParkingFeeOneWayCarsharing(final String timeParkingFeeOneWayCarsharing) {
		this.timeParkingFeeOneWayCarsharing = timeParkingFeeOneWayCarsharing;
	}
	
	@StringGetter( "distanceFeeOneWayCarsharing" )
	public String distanceFeeOneWayCarsharing() {
		return this.distanceFeeOneWayCarsharing;
	}

	@StringSetter( "distanceFeeOneWayCarsharing" )
	public void setDistanceFeeOneWayCarsharing(final String distanceFeeOneWayCarsharing) {
		this.distanceFeeOneWayCarsharing = distanceFeeOneWayCarsharing;
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
