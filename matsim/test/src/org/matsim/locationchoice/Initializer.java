package org.matsim.locationchoice;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class Initializer {
	
	private Controler controler;
	
	public Initializer() {	
	}
	
	public void init(MatsimTestCase testCase) {
		// TODO: Avoid copying of config 3 times 
		// lnk does not work. get path to locationchcoice
		String	path = testCase.getPackageInputDirectory() + "config.xml";		
		testCase.loadConfig(path);
		this.controler = new Controler(Gbl.getConfig());
		this.controler.setOverwriteFiles(true);		
		this.controler.run();
	}

	public Controler getControler() {
		return controler;
	}
	public void setControler(Controler controler) {
		this.controler = controler;
	}

}
