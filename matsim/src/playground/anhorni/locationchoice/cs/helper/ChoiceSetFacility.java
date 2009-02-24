package playground.anhorni.locationchoice.cs.helper;

public class ChoiceSetFacility {
	
	private ZHFacility facility;
	private double	travelTimeStartShopEnd;
	private double travelDistanceStartShopEnd;
	

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
}
