package lsp.shipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.matsim.api.core.v01.Id;

/*package-private*/ class Log implements ShipmentPlan {

	class LogElementComparator implements Comparator<ShipmentPlanElement>{

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

	
	private LSPShipment shipment;
	private HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement> logElements;
	
	
	public Log(LSPShipment shipment){
		this.shipment = shipment;
		this.logElements = new HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement>();
	}
	
	@Override
	public LSPShipment getShipment() {
		return shipment;
	}

	public void addPlanElement(Id<ShipmentPlanElement> id, ShipmentPlanElement element) {
		logElements.put(id, element);
	}

	@Override
	public  HashMap<Id<ShipmentPlanElement> , ShipmentPlanElement> getPlanElements() {
		return logElements;
	}

	@Override
	public ShipmentPlanElement getMostRecentEntry() {
		ArrayList<ShipmentPlanElement> logList = new ArrayList<ShipmentPlanElement>(logElements.values());
		Collections.sort(logList, new LogElementComparator());
		Collections.reverse(logList);
		return logList.get(0);
	}

	@Override
	public void clear() {
		logElements.clear();
	}
}
