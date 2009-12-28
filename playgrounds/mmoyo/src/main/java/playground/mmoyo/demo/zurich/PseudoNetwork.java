package playground.mmoyo.demo.zurich;

import playground.mrieser.pt.demo.PseudoNetworkDemo;

public class PseudoNetwork {

	public static void main(final String[] args) {
		
		String scheduleFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/TransitSchedule.xml";
		PseudoNetworkDemo.main(new String[]{scheduleFile});
	}
	
}
