/**
 * 
 */
package playground.mfeil;

import org.matsim.controler.Controler;

/**
 * @author Matthias Feil
 * To call ControlerTest
 */
public class ControlerRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controler controler = new ControlerTest(args);
		controler.run();

	}

}
