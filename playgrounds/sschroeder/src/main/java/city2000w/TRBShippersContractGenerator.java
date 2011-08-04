package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import freight.CommodityFlow;
import freight.ShipperImpl;
import freight.ShipperPlanWriter;
import freight.ShipperUtils;
import freight.Shippers;

public class TRBShippersContractGenerator {
	
	public static class TimeProfile {
		public double pickupStart;
		public double pickupEnd;
		public double deliveryStart;
		public double deliveryEnd;
		public TimeProfile(double pickupStart, double pickupEnd,
				double deliveryStart, double deliveryEnd) {
			super();
			this.pickupStart = pickupStart;
			this.pickupEnd = pickupEnd;
			this.deliveryStart = deliveryStart;
			this.deliveryEnd = deliveryEnd;
		}
		
	}
	
	private Shippers shippers;
	
	private static Logger logger = Logger.getLogger(TRBShippersContractGenerator.class);
	
	public TRBShippersContractGenerator(Shippers shippers) {
		super();
		this.shippers = shippers;
	}
	
	public static void main(String[] args) {
		String shipperPlan = "input/trbCase/trbShippers_randVal.xml";
		Shippers shippers = new Shippers();
		TRBShippersContractGenerator contractGenerator = new TRBShippersContractGenerator(shippers);
		contractGenerator.run();
		new ShipperPlanWriter(shippers.getShippers()).write(shipperPlan);
	}
	
	public void run(){
		logger.info("start generating");
		int nOfShippers = 1;
		for(int i=0;i<nOfShippers;i++){
			Id sourceLinkId = makeId("industry");
			ShipperImpl shipper = ShipperUtils.createShipper("shipper_stefan", sourceLinkId.toString());
			shippers.getShippers().add(shipper);
			for (int destinationColumn = 0; destinationColumn <= 8; destinationColumn++) {
				if(destinationColumn == 4){
					continue;
				}
				Id destinationLinkId = makeLinkId(1, destinationColumn);
				CommodityFlow comFlow = ShipperUtils.createCommodityFlow(sourceLinkId, destinationLinkId, 10, getValue());
				shipper.getContracts().add(ShipperUtils.createShipperContract(comFlow));
			}
			shipper.getShipperKnowledge().addTimeProfile(1, getCollection(getRandomProfile()));
			shipper.getShipperKnowledge().addTimeProfile(2, getCollection(morning(), afternoon()));
		}
		logger.info("done");
	}
	
	private Collection<TimeProfile> getCollection(TimeProfile morning, TimeProfile afternoon) {
		List<TimeProfile> l = new ArrayList<TRBShippersContractGenerator.TimeProfile>();
		l.add(morning);
		l.add(afternoon);
		return l;
	}

	private Collection<TimeProfile> getCollection(TimeProfile randomProfile) {
		List<TimeProfile> l = new ArrayList<TimeProfile>();
		l.add(randomProfile);
		return l;
	}

	private TimeProfile getRandomProfile() {
		if(MatsimRandom.getRandom().nextDouble() < 0.5){
			return morning();
		}
		else{
			return afternoon();
		}
	}
	
	private double getValue() {
		double rand = MatsimRandom.getRandom().nextDouble();
		return Math.round(100.0 + (500.0-100.0)*rand);
	}

	private Id makeLinkId(int i, int j) {
		return makeId("i("+i+","+j+")");
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}
	
	private TimeProfile afternoon() {
		//<shipment size="5" startPickup="10200.0" endPickup="12000" startDelivery="0.0" endDelivery="14000"/>
		return new TimeProfile(12*3600,18*3600,0.0,24*3600);
	}

	private TimeProfile morning() {
		//<shipment size="5" startPickup="0.0" endPickup="7200.0" startDelivery="0.0" endDelivery="7200"/>
		return new TimeProfile(0.0,6*3600,0.0,12*3600);
	}

}
