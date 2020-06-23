package lsp.shipment;

import java.util.Comparator;

import lsp.ShipmentTuple;

public final class ShipmentComparator implements Comparator<ShipmentTuple>{

	@Override
	public int compare(ShipmentTuple o1, ShipmentTuple o2) {
		return Double.compare(o1.getTime(), o2.getTime());
	}
}
