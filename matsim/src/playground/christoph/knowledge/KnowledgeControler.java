package playground.christoph.knowledge;

import org.matsim.controler.Controler;
import org.matsim.replanning.StrategyManager;

//============================================================================
// Controler class with implemented knowledge router.
// Christoph Dobler, August 2008
//============================================================================
public class KnowledgeControler extends Controler {

	public KnowledgeControler(String args[])
	{
		super(args);
	}
	
	public static void main(String args[])
	{
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} 
		else {
			final KnowledgeControler knowledgeControler = new KnowledgeControler(args);
			knowledgeControler.run();
		}
		System.exit(0);
	}

	// KnowledgeStrategyManagerConfigLoader
	
	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		KnowledgeStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}
	
}