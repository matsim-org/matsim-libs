package lsp.shipment;

import java.util.Comparator;

public class AbstractShipmentPlanElementComparator implements Comparator<AbstractShipmentPlanElement>{

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
