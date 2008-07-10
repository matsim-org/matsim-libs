package playground.wrashid.deqsim;

import org.matsim.controler.Controler;

import playground.wrashid.DES.utils.Timer;

class DESControler extends Controler {
	public DESControler(final String[] args) {
	    super(args);
	  }

	protected void runMobSim() {
		
		new JavaDEQSim(this.network, this.population, this.events).run();
	}

	public static void main(final String[] args) {
		Timer t=new Timer();
		t.startTimer();
		new DESControler(args).run();
		t.endTimer();
		t.printMeasuredTime("Time needed for simulation: ");
	}
}