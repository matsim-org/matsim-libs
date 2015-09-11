package playground.dhosse.cl.population;

public class FreightTrip {
	
	private String id;
	private String originZone;
	private String destinationZone;
	private String pCon; //Point of survey
	private int numberOfAxis;
	private double timeOfSurvey;
	private String typeOfGoods; 
	

	public FreightTrip(String id, String originZone, String destinationZone,
			String pCon, int numberOfAxis, double timeOfSurvey,
			String typeOfGoods) {
		super();
		this.id = id;
		this.originZone = originZone;
		this.destinationZone = destinationZone;
		this.pCon = pCon;
		this.numberOfAxis = numberOfAxis;
		this.timeOfSurvey = timeOfSurvey;
		this.typeOfGoods = typeOfGoods;
	}


	public String getId() {
		return id;
	}

	public String getOriginZone() {
		return originZone;
	}

	public String getDestinationZone() {
		return destinationZone;
	}

	public String getpCon() {
		return pCon;
	}

	public int getNumberOfAxis() {
		return numberOfAxis;
	}

	public double getTimeOfSurvey() {
		return timeOfSurvey;
	}
	
	public String getTypeOfGoods() {
		return typeOfGoods;
	}

//	public void setId(String id) {
//		this.id = id;
//	}
//
//	public void setOriginZone(String originZone) {
//		this.originZone = originZone;
//	}
//
//	public void setDestinationZone(String destinationZone) {
//		this.destinationZone = destinationZone;
//	}
//
//	public void setpCon(String pCon) {
//		this.pCon = pCon;
//	}
//
//	public void setNumberOfAxis(int numberOfAxis) {
//		this.numberOfAxis = numberOfAxis;
//	}
//
//	public void setTimeOfSurvey(double timeOfSurvey) {
//		this.timeOfSurvey = timeOfSurvey;
//	}
//	
//	public void setTypeOfGoods(String typeOfGoods) {
//		this.typeOfGoods = typeOfGoods;
//	}


}
