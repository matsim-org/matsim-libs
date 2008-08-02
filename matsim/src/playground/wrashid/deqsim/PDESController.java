package playground.wrashid.deqsim;

import org.matsim.controler.Controler;

import playground.wrashid.DES.utils.Timer;

public class PDESController extends Controler {
	public PDESController(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		
		new JavaPDEQSim(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new PDESController(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for PDESController run: ");
	}
}
