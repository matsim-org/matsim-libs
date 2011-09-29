package freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TimeWindow;
import freight.utils.GreedyShipmentAggregator;

public class GreedyShipmentAggregatorImpl implements GreedyShipmentAggregator{
	
	static class ClusterKey {
		public Id from;
		
		public Id to;
		
		public double pickupStart;
		
		public double pickupEnd;
		
		public double deliveryStart;
		
		public double deliveryEnd;

		public ClusterKey(Id from, Id to, double pickupStart, double pickupEnd,
				double deliveryStart, double deliveryEnd) {
			super();
			this.from = from;
			this.to = to;
			this.pickupStart = pickupStart;
			this.pickupEnd = pickupEnd;
			this.deliveryStart = deliveryStart;
			this.deliveryEnd = deliveryEnd;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(deliveryEnd);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(deliveryStart);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			temp = Double.doubleToLongBits(pickupEnd);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(pickupStart);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClusterKey other = (ClusterKey) obj;
			if (Double.doubleToLongBits(deliveryEnd) != Double
					.doubleToLongBits(other.deliveryEnd))
				return false;
			if (Double.doubleToLongBits(deliveryStart) != Double
					.doubleToLongBits(other.deliveryStart))
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (Double.doubleToLongBits(pickupEnd) != Double
					.doubleToLongBits(other.pickupEnd))
				return false;
			if (Double.doubleToLongBits(pickupStart) != Double
					.doubleToLongBits(other.pickupStart))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
	}
	
	private static Logger logger = Logger.getLogger(GreedyShipmentAggregatorImpl.class);
	
	private Map<CarrierShipment,Collection<CarrierShipment>> shipmentMap = new HashMap<CarrierShipment, Collection<CarrierShipment>>();
	
	private Map<ClusterKey,Collection<CarrierShipment>> clusteredShipments = new HashMap<ClusterKey, Collection<CarrierShipment>>();
	
	private int maxSize;

	public GreedyShipmentAggregatorImpl(int maxSize) {
		super();
		this.maxSize = maxSize;
	}

	public Map<CarrierShipment,Collection<CarrierShipment>> aggregateShipments(Collection<CarrierShipment> shipments){
		logger.info("aggregate shipments to bundles");
		logger.info(shipments.size() + " shipments to aggregate. this might take a bit");
		reset();
		clusterShipments(shipments);
		createBundles();
		assertNoShipmentLost();
		logger.info("number of resulting bundles: " + shipmentMap.keySet().size());
		return shipmentMap;
	}

	private void assertNoShipmentLost() {
		int totalShipmentSizeOfBundles = 0;
		for(Shipment s : shipmentMap.keySet()){
			totalShipmentSizeOfBundles += s.getSize();
		}
		int totalShipmentSizeOfOriginalShipments = 0;
		for(Collection<CarrierShipment> sColl : clusteredShipments.values()){
			for(Shipment s : sColl){
				totalShipmentSizeOfOriginalShipments += s.getSize();
			}
		}
		if(totalShipmentSizeOfBundles != totalShipmentSizeOfOriginalShipments){
			throw new IllegalStateException("shipments lost! this cannot be. orgSize=" + totalShipmentSizeOfOriginalShipments + ", bundleSize=" + totalShipmentSizeOfBundles);
		}
	}

	private void createBundles() {
		for(ClusterKey key : clusteredShipments.keySet()){
			Collection<CarrierShipment> shipmentColl = clusteredShipments.get(key);
			LinkedList<CarrierShipment> sortedList = new LinkedList<CarrierShipment>(shipmentColl);
			Collections.sort(sortedList, getComparator());
			logger.debug(sortedList);
			while(!sortedList.isEmpty()){
				int load = 0;
				Collection<CarrierShipment> firstPartOfShipmentBundle = new ArrayList<CarrierShipment>();
				Iterator<CarrierShipment> descendingSizeIter = sortedList.iterator();
				boolean stillSpace = true;
				while(descendingSizeIter.hasNext() && stillSpace){
					CarrierShipment s = descendingSizeIter.next();
					if(load+s.getSize() <= maxSize){
						load += s.getSize();
						firstPartOfShipmentBundle.add(s);
					}
					else{
						stillSpace = false;
					}
				}
				for(Shipment s : firstPartOfShipmentBundle){
					sortedList.remove(s);
				}
				Collection<CarrierShipment> secondPartOfShipmentBundle = new ArrayList<CarrierShipment>();
				boolean bundleNotFound = true;
				Iterator<CarrierShipment> ascendingSizeIter = sortedList.descendingIterator(); 
				while(ascendingSizeIter.hasNext() && bundleNotFound){
					CarrierShipment s = ascendingSizeIter.next();
					if(load+s.getSize() <= maxSize){
						load+=s.getSize();
						secondPartOfShipmentBundle.add(s);
					}
					else{
						bundleNotFound = false;
					}
				}
				for(Shipment s : secondPartOfShipmentBundle){
					sortedList.remove(s);
				}
				Collection<CarrierShipment> wholeBundel = new ArrayList<CarrierShipment>();
				wholeBundel.addAll(firstPartOfShipmentBundle);
				wholeBundel.addAll(secondPartOfShipmentBundle);
				CarrierShipment shipment = CarrierUtils.createShipment(key.from, key.to, load, key.pickupStart, key.pickupEnd, 
						key.deliveryStart, key.deliveryEnd);
				shipmentMap.put(shipment, wholeBundel);
			}
		}
	}

	private Comparator<CarrierShipment> getComparator() {
		return new Comparator<CarrierShipment>() {

			public int compare(CarrierShipment o1, CarrierShipment o2) {
				if(o1.getSize() > o2.getSize()){
					return -1;
				}
				else{
					return 1;
				}
			}
		};
	}

	private void clusterShipments(Collection<CarrierShipment> shipments) {
		for(CarrierShipment s : shipments){
			ClusterKey key = makeKey(s.getFrom(),s.getTo(),s.getPickupTimeWindow(),s.getDeliveryTimeWindow());
			if(clusteredShipments.containsKey(key)){
				clusteredShipments.get(key).add(s);
			}
			else{
				Collection<CarrierShipment> coll = new ArrayList<CarrierShipment>();
				coll.add(s);
				clusteredShipments.put(key, coll);
			}
		}
		logger.debug("nOfClusters: " + clusteredShipments.size());
	}

	private ClusterKey makeKey(Id from, Id to, TimeWindow pickupTimeWindow, TimeWindow deliveryTimeWindow) {
		return new ClusterKey(from,to,pickupTimeWindow.getStart(), pickupTimeWindow.getEnd(),deliveryTimeWindow.getStart(),deliveryTimeWindow.getEnd());
	}

	public void reset() {
		shipmentMap.clear();
		clusteredShipments.clear();
	}
	
	
}