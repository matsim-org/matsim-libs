package playground.balac.twowaycarsharingredisigned.controler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.twowaycarsharingredisigned.controler.TWEventsHandler.RentalInfo;


public class TWListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	TWEventsHandler twhandler;
	String inputFilerb;
	
	public TWListener(String inputFilerb) {
		
		this.inputFilerb = inputFilerb;
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getIteration() % 2 == 0) {
		
		ArrayList<RentalInfo> info = twhandler.rentals();
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter(inputFilerb);
		try {
			outLink.write("personID   startTime   endTIme   startLink   distance   accessTime   egressTime");
			outLink.newLine();
		for(RentalInfo i: info) {
			
			
				outLink.write(i.toString());
				outLink.newLine();
			
		}
		outLink.flush();
		outLink.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		event.getControler().getEvents().removeHandler(twhandler);

		
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub

        twhandler = new TWEventsHandler(event.getControler().getScenario().getNetwork());
		
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		if (event.getIteration() % 2 == 0) {
			event.getControler().getEvents().addHandler(twhandler);
			
		}
		
	}
	
		

}
