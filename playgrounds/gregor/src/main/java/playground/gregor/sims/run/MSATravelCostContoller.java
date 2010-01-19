package playground.gregor.sims.run;

import org.matsim.core.controler.Controler;
import org.matsim.evacuation.run.EvacuationQSimControllerII;

import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

public class MSATravelCostContoller extends EvacuationQSimControllerII {

	public MSATravelCostContoller(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void setUp(){
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		super.setUp();
		
	}
	

	public static void main(final String[] args) {
		final Controler controler = new MSATravelCostContoller(args);
		controler.run();
		System.exit(0);
	}	
}
