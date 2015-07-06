package playground.balac.taxiservice.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class TaxiserviceConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "Taxiservice";
	
	private String travelingTaxiservice = null;
	
	private String constantTaxiservice = null;
	
	private String timeFeeTaxiservice = null;
	
	private String distanceFeeTaxiservice = null;
	
	public TaxiserviceConfigGroup() {
		super(GROUP_NAME);
	}
	@StringGetter( "travelingTaxiservice" )
	public String getUtilityOfTravelling() {
		return this.travelingTaxiservice;
	}

	@StringSetter( "travelingTaxiservice" )
	public void setUtilityOfTravelling(final String travelingTaxiservice) {
		this.travelingTaxiservice = travelingTaxiservice;
	}

	@StringGetter( "constantTaxiservice" )
	public String constantTaxiservice() {
		return this.constantTaxiservice;
	}

	@StringSetter( "constantTaxiservice" )
	public void setConstantTaxiservice(final String constantTaxiservice) {
		this.constantTaxiservice = constantTaxiservice;
	}
	
	@StringGetter( "timeFeeTaxiservice" )
	public String timeFeeTaxiservice() {
		return this.timeFeeTaxiservice;
	}

	@StringSetter( "timeFeeTaxiservice" )
	public void setTimeFeeTaxiservice(final String timeFeeTaxiservice) {
		this.timeFeeTaxiservice = timeFeeTaxiservice;
	}
	
	@StringGetter( "distanceFeeTaxiservice" )
	public String distanceFeeTaxiservice() {
		return this.distanceFeeTaxiservice;
	}

	@StringSetter( "distanceFeeTaxiservice" )
	public void setDistanceFeeTaxiservice(final String distanceFeeTaxiservice) {
		this.distanceFeeTaxiservice = distanceFeeTaxiservice;
	}

}
