package playground.wrashid.deqsim;

import org.matsim.controler.Controler;
import org.matsim.mobsim.jdeqsim.util.Timer;


public class PDESController3 extends Controler {
	public PDESController3(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		
		new JavaPDEQSim3(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new PDESController3(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for PDESController run: ");
	}
}
