package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;

class MyControlerListener implements StartupListener, AfterMobsimListener {
	
	playground.kai.analysis.MyCalcLegTimes calcLegTimes = null ;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.calcLegTimes = new playground.kai.analysis.MyCalcLegTimes( event.getControler().getScenario() ) ;
		event.getControler().getEvents().addHandler( this.calcLegTimes ) ;

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		int iteration = event.getIteration() ;

		this.calcLegTimes.writeStats(event.getControler().getControlerIO().getIterationFilename(iteration, "mytripdurations.txt"));

		Logger.getLogger(this.getClass()).info("[" + iteration + "] average trip (probably: leg) duration is: " 
				+ (int) this.calcLegTimes.getAverageTripDuration()
				+ " seconds = " + Time.writeTime(this.calcLegTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));

		// trips are from "true" activity to "true" activity.  legs may also go
		// from/to ptInteraction activity.  This, in my opinion "legs" is the correct (matsim) term
		// kai, jul'11

	}

}

class MyControler {
	
	public static void main ( String[] args ) {

		Controler controler = new Controler( args ) ;

		controler.setOverwriteFiles(true) ;
		
		ControlerListener myControlerListener = new MyControlerListener() ;
		controler.addControlerListener(myControlerListener) ;
		
		controler.run();
	
	}

}
