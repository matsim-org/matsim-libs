package commercialtraffic.demandAssigment;

public class ServiceDefinition {
	String deleteActivity;
	String deliveryAmount;
	String deliveryType;
	String deliveryTimeStart;
	String deliveryTimeEnd;

	ServiceDefinition(String deleteActivity, String deliveryAmount, String deliveryType, String deliveryTimeStart,
			String deliveryTimeEnd) {

		this.deleteActivity = deleteActivity ;
		this.deliveryAmount = deliveryAmount;
		this.deliveryType = deliveryType;
		this.deliveryTimeStart =deliveryTimeStart;
		this.deliveryTimeEnd = deliveryTimeEnd ;
	}

}
