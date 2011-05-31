package freight;

public class ShipperSchema {
	public static class Shipper{
		public static String ID = "id";
		
		public static String LOCATION = "linkId";
	}
	
	public static class CommodityFlow {
		public static String ID = "id";
		
		public static String FROM = "from";
		
		public static String TO = "to";
		
		public static String SIZE = "size";
		
		public static String VALUE = "value";
	}
	
	public static class ScheduledCommodityFlow {
		public static String ID = "comFlowId";
		
		public static String TSPID = "tspId";
		
		public static String PRICE = "price";
	}
	
	public static class Shipment {
		public static String SIZE = "size";
		
		public static String STARTPICKUP = "startPickup";
		
		public static String ENDPICKUP = "endPickup";
		
		public static String STARTDELIVERY = "startDelivery";
		
		public static String ENDDELIVERY = "endDelivery";
	}
	
	public static String SHIPPERS = "shippers";
	
	public static String SHIPPER = "shipper";
	
	public static String COMMODITYFLOWS = "commodityFlows";
	
	public static String COMMODITYFLOW = "commodityFlow";
	
	public static String SCHEDULEDFLOWS = "scheduledFlows";
	
	public static String SCHEDULEDFLOW = "scheduledFlow";
	
	public static String SHIPMENT = "shipment";
	
	public static String FROM = "from";
	
	

}
