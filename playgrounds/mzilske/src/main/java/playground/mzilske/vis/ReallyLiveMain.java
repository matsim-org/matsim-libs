package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;

import org.matsim.run.OTFVis;


public class ReallyLiveMain {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {

		String configFileName = "./examples/tutorial/config/example5-config.xml";
		OTFVis.playConfig(configFileName);
		
	}

}
