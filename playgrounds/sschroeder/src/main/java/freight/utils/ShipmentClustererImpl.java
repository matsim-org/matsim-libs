package freight.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import playground.mzilske.freight.carrier.Shipment;

public class ShipmentClustererImpl implements ShipmentClusterer{

	private TimePeriods timePeriods;
	
	public ShipmentClustererImpl(TimePeriods timePeriods) {
		super();
		this.timePeriods = timePeriods;
	}

	@Override
	public Map<TimePeriod, Collection<Shipment>> clusterShipments(Collection<Shipment> shipments) {
		Map<TimePeriod,Collection<Shipment>> clusters = new TreeMap<TimePeriod, Collection<Shipment>>(new Comparator<TimePeriod>() {

			@Override
			public int compare(TimePeriod o1, TimePeriod o2) {
				if(o1.start < o2.start){
					return -1;
				}
				return 1;
			}
		});
		for(Shipment shipment : shipments){
			TimePeriod accordingTimeBin = getTimePeriod(shipment);
			if(accordingTimeBin == null){
				throw new IllegalStateException("could not find timeBin for shipment " +  shipment);
			}
			if(clusters.containsKey(accordingTimeBin)){
				clusters.get(accordingTimeBin).add(shipment);
			}
			else{
				Collection<Shipment> newShipments = new ArrayList<Shipment>();
				newShipments.add(shipment);
				clusters.put(accordingTimeBin, newShipments);
 			}
		}
		return clusters;
	}

	private TimePeriod getTimePeriod(Shipment shipment) {
		for(TimePeriod period : this.timePeriods.getPeriods()){
			if(period.isWithin(shipment.getPickupTimeWindow().getStart()) && period.isWithin(shipment.getDeliveryTimeWindow().getEnd())){
				return period;
			}
		}
		return null;
	}
	
	

}
