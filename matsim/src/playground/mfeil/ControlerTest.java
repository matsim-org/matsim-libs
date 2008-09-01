/**
 * 
 */
package playground.mfeil;


import org.matsim.replanning.StrategyManager;
import playground.mfeil.StrategyManagerConfigLoaderTest;


/**
 * @author Matthias Feil
 * Adjusting the Controler in order to refer to the StrategyManagerConfigLoaderTest
 */
public class ControlerTest extends org.matsim.controler.Controler {
	public ControlerTest (){
		super("");
	}
		/**
		 * @return A fully initialized StrategyManager for the plans replanning.
		 */
	
// Although illustrating that this method is overriding the superclass method it does not. To be clarified...	
	@Override
		protected StrategyManager loadStrategyManager() {
			StrategyManager manager = new StrategyManager();
			CopyOfStrategyManagerConfigLoaderTest cf = new CopyOfStrategyManagerConfigLoaderTest();
			System.out.println ("Das ist der ControlerTest.");
			cf.load(this, this.config, manager);
			return manager;
		}
	
}
