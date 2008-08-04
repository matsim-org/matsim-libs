package playground.wrashid.deqsim;

import org.matsim.controler.Controler;

import playground.wrashid.DES.utils.Timer;

public class PDESController1 extends Controler {
	public PDESController1(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		
		new JavaPDEQSim1(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new PDESController1(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for PDESController run: ");
	}
}
