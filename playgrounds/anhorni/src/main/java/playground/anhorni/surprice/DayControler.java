package playground.anhorni.surprice;

import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

public class DayControler extends Controler {
	
	public DayControler(final ScenarioImpl scenario) {
		super(scenario);	
		super.setOverwriteFiles(true);
	}
    
    @Override
    protected void setUp() {
    	super.setUp(); 
	}  
}
