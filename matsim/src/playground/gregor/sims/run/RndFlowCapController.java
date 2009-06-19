package playground.gregor.sims.run;

import org.matsim.core.controler.Controler;

import playground.gregor.sims.socialcostII.LinkFlowCapRandomizer;


public class RndFlowCapController extends Controler {

	private double c;

	public RndFlowCapController(String[] args, double c) {
		super(args);
		this.c = c;
	}
	
	

	@Override
	protected void setUp() {
		super.setUp();
		LinkFlowCapRandomizer lr = new LinkFlowCapRandomizer(this.network,c,0.1);
		this.addControlerListener(lr);
	}



	public static void main(String [] args ) {
		double c = Double.parseDouble(args[1]);
		Controler controller = new RndFlowCapController(args, c);
		controller.setOverwriteFiles(true);
		controller.run();
		System.exit(0);
	}
}
