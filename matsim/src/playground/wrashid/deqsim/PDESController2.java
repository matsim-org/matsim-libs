package playground.wrashid.deqsim;

import org.matsim.controler.Controler;

import playground.wrashid.DES.util.Timer;

public class PDESController2 extends Controler {
	public PDESController2(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		
		new JavaPDEQSim2(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new PDESController2(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for PDESController run: ");
	}
}
