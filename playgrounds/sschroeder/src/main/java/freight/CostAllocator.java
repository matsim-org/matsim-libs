package freight;

import java.util.Collection;

import freight.ShipperSchema.Shipment;

public interface CostAllocator {
	
	public void allocate(Collection<Shipment> shipments, double costs);

}
