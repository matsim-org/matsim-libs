package lsp.shipment;

import java.util.Comparator;

public final class ShipmentPlanElementComparator implements Comparator<ShipmentPlanElement>{

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
