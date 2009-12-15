package playground.anhorni.choiceSetGeneration.helper;

public class ChoiceSetFacility {
	
	private ZHFacility facility;
	private double	travelTimeStartShopEnd;
	private double travelDistanceStartShopEnd;
	
	private double additionalTime;
	private double additionalDistance;
	
	public ChoiceSetFacility(ZHFacility facility,
			double travelTimeStartShopEnd, double travelDistanceStartShopEnd) {
		super();
		this.facility = facility;
		this.travelTimeStartShopEnd = travelTimeStartShopEnd;
		this.travelDistanceStartShopEnd = travelDistanceStartShopEnd;
	}
		
	public ZHFacility getFacility() {
		return facility;
	}
	public void setFacility(ZHFacility facility) {
		this.facility = facility;
	}
	public double getTravelTimeStartShopEnd() {
		return travelTimeStartShopEnd;
	}
	public void setTravelTimeStartShopEnd(double travelTimeStartShopEnd) {
		this.travelTimeStartShopEnd = travelTimeStartShopEnd;
	}
	public double getTravelDistanceStartShopEnd() {
		return travelDistanceStartShopEnd;
	}
	public void setTravelDistanceStartShopEnd(double travelDistanceStartShopEnd) {
		this.travelDistanceStartShopEnd = travelDistanceStartShopEnd;
	}

	public double getAdditionalTime() {
		return additionalTime;
	}

	public void setAdditionalTime(double additionalTime) {
		this.additionalTime = additionalTime;
	}

	public double getAdditionalDistance() {
		return additionalDistance;
	}

	public void setAdditionalDistance(double additionalDistance) {
		this.additionalDistance = additionalDistance;
	}
}
