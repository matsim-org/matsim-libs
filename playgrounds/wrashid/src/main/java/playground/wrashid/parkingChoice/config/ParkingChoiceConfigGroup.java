package playground.wrashid.parkingChoice.config;

import org.matsim.core.config.ReflectiveConfigGroup;


public class ParkingChoiceConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "parkingChoice.ZH";

	private String parkingDataDirectory = null;
	
	private String cityZonesFile = null;
	
	private String networkLinkSlopes = null;
	
	private boolean restrictFFParkingToStreetParking = false;
	
	private String parkingWalkBeta = null;
	
	private String parkingCostBeta = null;
	
	private String parkingScoreScalingFactor = null;

	private String randomErrorTermScalingFactor = null;
	
	private double parkingGroupCapacityScalingFactor_streetParking = 1.0;
	private double parkingGroupCapacityScalingFactor_garageParking = 1.0;

	private double parkingGroupCapacityScalingFactor_privateParking = 1.0;
	
	private double parkingGroupCapacityScalingFactor_publicPOutsideCity = 1.0;
	
	private double populationScalingFactor = 1.0;

	public ParkingChoiceConfigGroup() {
		super(GROUP_NAME);
	}
	@StringGetter( "parkingDataDirectory" )
	public String getParkingDataDirectory() {
		return parkingDataDirectory;
	}
	@StringSetter( "parkingDataDirectory" )
	public void setParkingDataDirectory(String parkingDataDirectory) {
		this.parkingDataDirectory = parkingDataDirectory;
	}
	@StringGetter( "cityZonesFile" )
	public String getCityZonesFile() {
		return cityZonesFile;
	}
	@StringSetter( "cityZonesFile" )
	public void setCityZonesFile(String cityZonesFile) {
		this.cityZonesFile = cityZonesFile;
	}
	@StringGetter( "networkLinkSlopes" )
	public String getNetworkLinkSlopes() {
		return networkLinkSlopes;
	}
	@StringSetter( "networkLinkSlopes" )
	public void setNetworkLinkSlopes(String networkLinkSlopes) {
		this.networkLinkSlopes = networkLinkSlopes;
	}
	@StringGetter( "restrictFFParkingToStreetParking" )
	public boolean isRestrictFFParkingToStreetParking() {
		return restrictFFParkingToStreetParking;
	}
	@StringSetter( "restrictFFParkingToStreetParking" )
	public void setRestrictFFParkingToStreetParking(boolean restrictFFParkingToStreetParking) {
		this.restrictFFParkingToStreetParking = restrictFFParkingToStreetParking;
	}
	@StringGetter( "parkingWalkBeta" )
	public String getParkingWalkBeta() {
		return parkingWalkBeta;
	}
	@StringSetter( "parkingWalkBeta" )
	public void setParkingWalkBeta(String parkingWalkBeta) {
		this.parkingWalkBeta = parkingWalkBeta;
	}
	@StringGetter( "parkingCostBeta" )
	public String getParkingCostBeta() {
		return parkingCostBeta;
	}
	@StringSetter( "parkingCostBeta" )
	public void setParkingCostBeta(String parkingCostBeta) {
		this.parkingCostBeta = parkingCostBeta;
	}
	@StringGetter( "parkingScoreScalingFactor" )
	public String getParkingScoreScalingFactor() {
		return parkingScoreScalingFactor;
	}
	@StringSetter( "parkingScoreScalingFactor" )
	public void setParkingScoreScalingFactor(String parkingScoreScalingFactor) {
		this.parkingScoreScalingFactor = parkingScoreScalingFactor;
	}
	@StringGetter( "randomErrorTermScalingFactor" )
	public String getRandomErrorTermScalingFactor() {
		return randomErrorTermScalingFactor;
	}
	@StringSetter( "randomErrorTermScalingFactor" )
	public void setRandomErrorTermScalingFactor(String randomErrorTermScalingFactor) {
		this.randomErrorTermScalingFactor = randomErrorTermScalingFactor;
	}
	@StringGetter( "parkingGroupCapacityScalingFactor_streetParking" )
	public double getParkingGroupCapacityScalingFactor_streetParking() {
		return parkingGroupCapacityScalingFactor_streetParking;
	}
	@StringSetter( "parkingGroupCapacityScalingFactor_streetParking" )
	public void setParkingGroupCapacityScalingFactor_streetParking(double parkingGroupCapacityScalingFactor_streetParking) {
		this.parkingGroupCapacityScalingFactor_streetParking = parkingGroupCapacityScalingFactor_streetParking;
	}
	@StringGetter( "parkingGroupCapacityScalingFactor_garageParking" )
	public double getParkingGroupCapacityScalingFactor_garageParking() {
		return parkingGroupCapacityScalingFactor_garageParking;
	}
	@StringSetter( "parkingGroupCapacityScalingFactor_garageParking" )
	public void setParkingGroupCapacityScalingFactor_garageParking(double parkingGroupCapacityScalingFactor_garageParking) {
		this.parkingGroupCapacityScalingFactor_garageParking = parkingGroupCapacityScalingFactor_garageParking;
	}
	@StringGetter( "parkingGroupCapacityScalingFactor_privateParking" )
	public double getParkingGroupCapacityScalingFactor_privateParking() {
		return parkingGroupCapacityScalingFactor_privateParking;
	}
	@StringSetter( "parkingGroupCapacityScalingFactor_privateParking" )
	public void setParkingGroupCapacityScalingFactor_privateParking(
			double parkingGroupCapacityScalingFactor_privateParking) {
		this.parkingGroupCapacityScalingFactor_privateParking = parkingGroupCapacityScalingFactor_privateParking;
	}
	@StringGetter( "parkingGroupCapacityScalingFactor_publicPOutsideCity" )
	public double getParkingGroupCapacityScalingFactor_publicPOutsideCity() {
		return parkingGroupCapacityScalingFactor_publicPOutsideCity;
	}
	@StringSetter( "parkingGroupCapacityScalingFactor_publicPOutsideCity" )
	public void setParkingGroupCapacityScalingFactor_publicPOutsideCity(
			double parkingGroupCapacityScalingFactor_publicPOutsideCity) {
		this.parkingGroupCapacityScalingFactor_publicPOutsideCity = parkingGroupCapacityScalingFactor_publicPOutsideCity;
	}
	@StringGetter( "populationScalingFactor" )
	public double getPopulationScalingFactor() {
		return populationScalingFactor;
	}
	@StringSetter( "populationScalingFactor" )
	public void setPopulationScalingFactor(double populationScalingFactor) {
		this.populationScalingFactor = populationScalingFactor;
	}

}
