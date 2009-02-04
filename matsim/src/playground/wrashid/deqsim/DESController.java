package playground.wrashid.deqsim;

import org.matsim.controler.Controler;
import org.matsim.mobsim.deqsim.util.Timer;


public class DESController extends Controler {
	public DESController(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		new JavaDEQSim(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new DESController(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for DESController run: ");
	}
}