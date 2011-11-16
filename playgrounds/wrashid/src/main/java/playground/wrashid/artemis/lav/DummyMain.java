package playground.wrashid.artemis.lav;

import playground.wrashid.lib.tools.txtConfig.TxtConfig;

public class DummyMain {

	public static void main(String[] args) {
		TxtConfig config=new TxtConfig(args[1]);
		System.out.println(config.getParameterValue("test"));
	}
	
}
