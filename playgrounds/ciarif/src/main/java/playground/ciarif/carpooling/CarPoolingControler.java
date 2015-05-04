package playground.ciarif.carpooling;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

import playground.meisterk.kti.controler.KTIControler;

public class CarPoolingControler extends KTIControler {
//public class CarPoolingControler extends Controler {
	
	public CarPoolingControler (String[] args){
		super(args);
		this.loadMyControlerListeners();
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}
	private void loadMyControlerListeners() {
		
//		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		this.addControlerListener(new CarPoolingListener());
		
	}
	
	 
	public static void main (final String[] args) { 
		Controler controler = new CarPoolingControler(args);
		controler.run();
	}
}







