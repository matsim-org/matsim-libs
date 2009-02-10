package org.matsim.mobsim.deqsim;

import org.matsim.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class ConfigParameterTest extends MatsimTestCase {

	public void testParametersSetCorrectly(){
		String args[]=new String[]{"test/input/org/matsim/mobsim/deqsim/config.xml"};
		Controler controler=new Controler(args);
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	
}
