package sandbox;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;

import java.util.ArrayList;
import java.util.List;

public class SandBoxRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Carrier> carriers = new ArrayList<Carrier>();
		SandBoxTrafficGenerator generator = new SandBoxTrafficGenerator(carriers);
		generator.run();
		CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers);
		planWriter.write("./output/sandBoxPlans.xml");
	}

}
