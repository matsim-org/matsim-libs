package lsp.shipment;

import java.util.*;

import org.matsim.api.core.v01.Id;

/*package-private*/ class ShipmentPlanImpl implements ShipmentPlan {

	static class LogElementComparator implements Comparator<ShipmentPlanElement>{

		@Override
		public int compare(ShipmentPlanElement o1, ShipmentPlanElement o2) {
			if(o1.getStartTime() > o2.getStartTime()){
				return 1;
			}
			if(o1.getStartTime() < o2.getStartTime()){
				return -1;
			}
			if(o1.getStartTime() == o2.getStartTime()) {
				if(o1.getEndTime() > o2.getEndTime()) {
					return 1;
				}
				if(o1.getEndTime() < o2.getEndTime()) {
					return -1;
				}
			}
			return 0;
		}
	}

	
	private final LSPShipment shipment;
	private final HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement> logElements;
	
	
	ShipmentPlanImpl( LSPShipment shipment ){
		this.shipment = shipment;
		this.logElements = new HashMap<>();
	}
	
	@Override
	public LSPShipment getShipment() {
		return shipment;
	}

	@Override public void addPlanElement( Id<ShipmentPlanElement> id, ShipmentPlanElement element ) {
		logElements.put(id, element);
	}

	@Override
	public Map<Id<ShipmentPlanElement>, ShipmentPlanElement> getPlanElements() {
		return Collections.unmodifiableMap( logElements );
	}

	@Override
	public ShipmentPlanElement getMostRecentEntry() {

		// there is no method to remove entries.  in consequence, the only way to change the result of this method is to "add" additional material into the plan.  Possibly,
		// the method here is indeed there to find the plan element that was added most recently, to figure out how the next one can be added.  However, this then
		// should be sorted by sequence of addition, not by timing.  ???   kai/kai, apr'21

		ArrayList<ShipmentPlanElement> logList = new ArrayList<>( logElements.values() );
		logList.sort(new LogElementComparator());
		Collections.reverse(logList);
		return logList.get(0);
	}

	@Override
	public void clear() {
		logElements.clear();
	}
}
