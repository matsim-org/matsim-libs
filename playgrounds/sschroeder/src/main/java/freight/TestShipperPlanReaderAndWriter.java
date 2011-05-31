package freight;

import java.util.ArrayList;
import java.util.Collection;

public class TestShipperPlanReaderAndWriter {
	
	public static void main(String[] args) {
		Collection<ShipperImpl> shippers = new ArrayList<ShipperImpl>();
		ShipperPlanReader reader = new ShipperPlanReader(shippers);
		reader.read("output/shipperPlans.xml");
		printShippers(shippers);
		
		ShipperPlanWriter writer = new ShipperPlanWriter(shippers);
		writer.write("output/newShipperPlans.xml");
		
		shippers.clear();
		ShipperPlanReader anotherReader = new ShipperPlanReader(shippers);
		anotherReader.read("output/newShipperPlans.xml");
		printShippers(shippers);
		
	}

	private static void printShippers(Collection<ShipperImpl> shippers) {
		for(ShipperImpl s : shippers){
			System.out.println(s);
			printShipperPlan(s.getSelectedPlan());
			System.out.println();
		}
		System.out.println();
		
	}

	private static void printShipperPlan(ShipperPlan selectedPlan) {
		for(ScheduledCommodityFlow sFlow : selectedPlan.getScheduledFlows()){
			System.out.println(sFlow.getCommodityFlow());
			System.out.println(sFlow.getTspOffer());
			printShipments(sFlow.getShipments());
		}
		
	}

	private static void printShipments(Collection<ShipperShipment> shipments) {
		for(ShipperShipment s : shipments){
			System.out.println(s);
		}
		
	}

}
