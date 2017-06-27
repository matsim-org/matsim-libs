package playground.vsptelematics.common;

import org.matsim.core.config.ReflectiveConfigGroup;

public class TelematicsConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUPNAME = "telematics";
	public static final String INCIDENTS_FILE = "incidentsFile";
	public static final String USE_HOMOGENEOUS_TRAVEL_TIMES = "useHomogeneousTravelTimes";
	public static final String INFOTYPE = "infotype";
	public static final String EQUIPMENT_RATE = "equipmentRate";
	public static final String USE_PREDICTED_TRAVEL_TIMES = "usePredictedTravelTimes";
	public static final String PREDICTED_TRAVEL_TIME_ROUTE1 = "predictedTravelTimeRoute1";
	public static final String PREDICTED_TRAVEL_TIME_ROUTE2 = "predictedTravelTimeRoute2";
	
	public enum Infotype{
		estimated, reactive
	}

	private String incidentsFile;
	private Infotype infotype;
	private double equipmentRate;
	private boolean usePredictedTravelTimes = false;
	private double predictedTravelTimeRoute1;
	private double predictedTravelTimeRoute2;
	private boolean useHomogeneousTravelTimes = false;
	
	public TelematicsConfigGroup() {
		super(GROUPNAME);
	}

//	@Override
//	protected void checkConsistency(Config config) {
//		if ((this.signalSystemFile == null) && (this.signalControlFile != null)) {
//			throw new IllegalStateException("For using a SignalSystemConfiguration a definition of the signal systems must exist!");
//		}
//	}

	@StringGetter( INCIDENTS_FILE )
	public String getIncidentsFile() {
		return this.incidentsFile;
	}

	@StringSetter( INCIDENTS_FILE )
	public void setIncidentsFile(final String incidentsFile) {
		this.incidentsFile = incidentsFile;
	}

	@StringGetter( USE_HOMOGENEOUS_TRAVEL_TIMES )
	public boolean isUseHomogeneousTravelTimes() {
		return this.useHomogeneousTravelTimes;
	}
	
	@StringSetter( USE_HOMOGENEOUS_TRAVEL_TIMES )
	public void setUseHomogeneousTravelTimes(boolean useHomogeneousTravelTimes){
		this.useHomogeneousTravelTimes = useHomogeneousTravelTimes;
	}
	
	@StringGetter( INFOTYPE )
	public Infotype getInfotype() {
		return this.infotype;
	}

	@StringSetter( INFOTYPE )
	public void setInfotype(Infotype infotype) {
		switch (infotype){
		// set the value for the supported actions
		case estimated:
		case reactive:
			this.infotype = infotype;
			break;
		// throw an exception if the value is not supported
		default:
			throw new IllegalArgumentException("The value " + infotype 
					+ " for key : " + INFOTYPE + " is not supported by this config group");
		}
		this.infotype = infotype;
	}
	
	@StringGetter( EQUIPMENT_RATE )
	public double getEquipmentRate() {
		return this.equipmentRate;
	}

	@StringSetter( EQUIPMENT_RATE )
	public void setEquipmentRate(final double equipmentRate) {
		this.equipmentRate = equipmentRate;
	}
	
	@StringGetter( USE_PREDICTED_TRAVEL_TIMES )
	public boolean getUsePredictedTravelTimes() {
		return this.usePredictedTravelTimes;
	}

	@StringSetter( USE_PREDICTED_TRAVEL_TIMES )
	public void setUsePredictedTravelTimes(final boolean usePredictedTravelTimes) {
		this.usePredictedTravelTimes = usePredictedTravelTimes;
	}
	
	@StringGetter( PREDICTED_TRAVEL_TIME_ROUTE1 )
	public double getPredictedTravelTimeRoute1() {
		return this.predictedTravelTimeRoute1;
	}

	@StringSetter( PREDICTED_TRAVEL_TIME_ROUTE1 )
	public void setPredictedTravelTimeRoute1(final double predictedTravelTimeRoute1) {
		this.predictedTravelTimeRoute1 = predictedTravelTimeRoute1;
	}
	
	@StringGetter( PREDICTED_TRAVEL_TIME_ROUTE2 )
	public double getPredictedTravelTimeRoute2() {
		return this.predictedTravelTimeRoute2;
	}

	@StringSetter( PREDICTED_TRAVEL_TIME_ROUTE2 )
	public void setPredictedTravelTimeRoute2(final double predictedTravelTimeRoute2) {
		this.predictedTravelTimeRoute2 = predictedTravelTimeRoute2;
	}
}
