package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;

import playground.jhackney.replanning.SocialStrategyManagerConfigLoader;

public class SNController2 extends Controler {

	private final Logger log = Logger.getLogger(SNController2.class);
	
	public SNController2(String args[]){
		super(args);
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController2(args);
		controler.addControlerListener(new SNControllerListener2());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
//	@override
//	protected void setup(){
//		super.setUp();
//	}
	// SocialStrategyManagerConfigLoader

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
//	@Override
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		SocialStrategyManagerConfigLoader.load(this, this.getConfig(), manager);
		return manager;
	}
}
