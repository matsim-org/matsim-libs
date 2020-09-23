package lsp.shipment;

import java.util.Comparator;

import lsp.ShipmentWithTime;

public final class ShipmentComparator implements Comparator<ShipmentWithTime>{

	@Override
	public int compare( ShipmentWithTime o1, ShipmentWithTime o2 ) {
		return Double.compare(o1.getTime(), o2.getTime());
	}
}
