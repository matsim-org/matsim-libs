package playground.vsp.pt.fare;

import java.util.Map;

public class FareZoneBasedPtFareParams extends PtFareParams {
	public static final String SET_NAME = "ptFareCalculationFareZoneBased";

	public static final String COST = "cost";

	private Double cost;

	public FareZoneBasedPtFareParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(COST, "Cost of a trip within the fare zone.");
		return map;
	}

	@StringGetter(COST)
	public Double getCost() {
		return cost;
	}

	@StringSetter(COST)
	public void setCost(Double cost) {
		this.cost = cost;
	}
	//TODO check consistency
}
