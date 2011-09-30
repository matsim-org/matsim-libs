package sandbox;

import java.util.ArrayList;
import java.util.List;

import playground.mzilske.freight.carrier.Carrier;
import freight.CarrierPlanWriter;

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
