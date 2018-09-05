package lsp.shipment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;



public class Schedule implements AbstractShipmentPlan{

	class ScheduleElementComparator implements Comparator<AbstractShipmentPlanElement>{

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
	private HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement> scheduleElements;
	
	
	public Schedule(LSPShipment shipment){
		this.shipment = shipment;
		this.scheduleElements = new HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement>();
	}
	

	public LSPShipment getShipment() {
		return shipment;
	}

	public HashMap<Id<AbstractShipmentPlanElement> , AbstractShipmentPlanElement> getPlanElements() {
		return scheduleElements;
	}

	public void addPlanElement(Id<AbstractShipmentPlanElement> id, AbstractShipmentPlanElement element) {
		scheduleElements.put(id, element);
	}


	@Override
	public AbstractShipmentPlanElement getMostRecentEntry() {
		ArrayList<AbstractShipmentPlanElement> scheduleList =  new ArrayList<AbstractShipmentPlanElement>(scheduleElements.values());
		Collections.sort(scheduleList, new ScheduleElementComparator());
		Collections.reverse(scheduleList);
		return scheduleList.get(0);
	}
	
	@Override
	public void clear() {
		scheduleElements.clear();
	}

}

	

	