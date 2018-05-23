package lsp.shipment;

import java.util.Comparator;

import lsp.ShipmentTuple;

public class ShipmentComparator implements Comparator<ShipmentTuple>{

	@Override
	public int compare(ShipmentTuple o1, ShipmentTuple o2) {
		if(o1.getTime() > o2.getTime()){
			return 1;	
		}
		if(o1.getTime() < o2.getTime()){
			return -1;
		}
		else{
			return 0;
		}	
	}	
}
