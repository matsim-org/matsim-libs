package playground.sergioo.hits2012;

import java.io.Serializable;

public abstract class Stage implements Serializable {

	//Enumerations
	public static enum Column implements Serializable {
	
		MODE(82, "CE"),
		MODE_OTHERS(83, "CF"),
		WALK_TIME(84, "CG"),
		START(85, "CH"),
		END(86, "CI"),
		NUM_PASSENGERS(87, "CJ"),
		WAIT_TIME(88, "CK"),
		IN_VEHICLE_TIME(89, "CL"),
		TAXI_FARE(90, "CM"),
		TAXI_REIMBURSMENT(91, "CN"),
		ERP_COST(92, "CO"),
		ERP_REIMBURSMENT(93, "CP"),
		PARK_COST(94, "CQ"),
		PARK_TYPE(95, "CR"),
		PARK_REIMBURSMENT(96, "CS"),
		FIRST_TRANSFER(98, "CU"),
		SECOND_TRANSFER(99, "CV"),
		THIRD_TRANSFER(100, "CW"),
		CYCLE(101, "CX"),
		LAST_WALK_TIME(102, "CY");
	
		//Attributes
		public int column;
		public String columnName;
	
		//Constructors
		private Column(int column, String columnName) {
			this.column = column;
			this.columnName = columnName;
		}
	
	}
	
	public static enum Mode implements Serializable {
	
		CAR_DRIVER("Car driver"),
		CAR_PASSENGER("Car passenger"),
		VAN_DRIVER("Van / Lorry driver"),
		VAN_PASSENGER("Van / Lorry passenger"),
		BIKE_RIDER("Motorcycle rider"),
		BIKE_PASSENGER("Motorcycle passenger"),
		LRT("LRT"),
		MRT("MRT"),
		PUBLIC_BUS("Public bus"),
		SCHOOL_BUS("School bus"),
		SHUTTLE_BUS("Shuttle bus"),
		COMPANY_BUS("Company bus"),
		TAXI("Taxi"),
		CYCLE("Cycle"),
		OTHER("Other (please describe)");
	
		//Attributes
		private String text;
	
		//Contructors
		private Mode(String text) {
			this.text = text;
		}
		public static Mode getMode(String text) {
			for(Mode mode:Mode.values())
				if(mode.text.equals(text))
					return mode;
			return null;
		}
	
	}
	
	//Attributes
	private final String id;
	private final String mode;
	private final double walkTime;
	private final double inVehicleTime;
	private final double lastWalkTime;
	
	//Constructors;
	public Stage(String id, String mode, double walkTime, double inVehicleTime, double lastWalkTime) {
		super();
		this.id = id;
		this.mode = mode;
		this.walkTime = walkTime;
		this.inVehicleTime = inVehicleTime;
		this.lastWalkTime = lastWalkTime;
	}

	//Methods
	public String getId() {
		return id;
	}
	public String getMode() {
		return mode;
	}
	public double getWalkTime() {
		return walkTime;
	}
	public double getInVehicleTime() {
		return inVehicleTime;
	}
	public double getLastWalkTime() {
		return lastWalkTime;
	}

}
