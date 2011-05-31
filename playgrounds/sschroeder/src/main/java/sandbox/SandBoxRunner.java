package sandbox;

import java.util.ArrayList;
import java.util.List;

import playground.mzilske.freight.CarrierImpl;
import freight.CarrierPlanWriter;

public class SandBoxRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<CarrierImpl> carriers = new ArrayList<CarrierImpl>();
		SandBoxTrafficGenerator generator = new SandBoxTrafficGenerator(carriers);
		generator.run();
		CarrierPlanWriter planWriter = new CarrierPlanWriter(carriers);
		planWriter.write("./output/sandBoxPlans.xml");
	}

}
