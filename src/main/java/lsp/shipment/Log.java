package lsp.shipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.matsim.api.core.v01.Id;

public class Log implements AbstractShipmentPlan {

	class LogElementComparator implements Comparator<AbstractShipmentPlanElement>{

		@Override
		public int compare(AbstractShipmentPlanElement o1, AbstractShipmentPlanElement o2) {
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
	private HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement> logElements;
	
	
	public Log(LSPShipment shipment){
		this.shipment = shipment;
		this.logElements = new HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement>();
	}
	
	@Override
	public LSPShipment getShipment() {
		return shipment;
	}

	public void addPlanElement(Id<AbstractShipmentPlanElement> id, AbstractShipmentPlanElement element) {
		logElements.put(id, element);
	}

	@Override
	public  HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement> getPlanElements() {
		return logElements;
	}

	@Override
	public AbstractShipmentPlanElement getMostRecentEntry() {
		ArrayList<AbstractShipmentPlanElement> logList = new ArrayList<AbstractShipmentPlanElement>(logElements.values());
		Collections.sort(logList, new LogElementComparator());
		Collections.reverse(logList);
		return logList.get(0);
	}

	@Override
	public void clear() {
		logElements.clear();
	}
}
