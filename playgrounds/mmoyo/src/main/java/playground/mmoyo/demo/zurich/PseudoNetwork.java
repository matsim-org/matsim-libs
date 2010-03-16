package playground.mmoyo.demo.zurich;

import playground.mrieser.pt.demo.PseudoNetworkDemo;

public class PseudoNetwork {

	public static void main(final String[] args) {
		
		String scheduleFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/transfer/blue_green_TransitSchedule.xml";
		PseudoNetworkDemo.main(new String[]{scheduleFile});
	}
	
}
