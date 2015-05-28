package playground.balac.freefloating.controler.listener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.freefloating.controler.listener.FFParkingEventsHandler.RentalInfoFF;


public class FFListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	FFParkingEventsHandler ffhandler;
	Controler controler;	
	
	public FFListener( Controler controler) {
		
		this.controler = controler;
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		ArrayList<RentalInfoFF> infoff = ffhandler.rentals();
		
		final BufferedWriter outLinkff = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "FF_CS"));
		try {
			outLinkff.write("personID   startTime   endTIme   startLink   endLink   distance   accessTime");
			outLinkff.newLine();
		for(RentalInfoFF i: infoff) {
			
			
			outLinkff.write(i.toString());
			outLinkff.newLine();
			
		}
		outLinkff.flush();
		outLinkff.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
				
		event.getControler().getEvents().removeHandler(ffhandler);

	}

	@Override
	public void notifyStartup(StartupEvent event) {
        
		ffhandler = new FFParkingEventsHandler(event.getControler().getScenario().getNetwork());
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
			
		event.getControler().getEvents().addHandler(ffhandler);
			
	}
	
		

}