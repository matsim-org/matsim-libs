package playground.balac.onewaycarsharingredisgned.controler;

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

import playground.balac.onewaycarsharingredisgned.controler.OWEventsHandler.RentalInfoFF;


public class OWListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	OWEventsHandler owhandler;
	String inputFileow;
	
	public OWListener(String inputFileow) {
		
		this.inputFileow = inputFileow;
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getIteration() % 2 == 0) {
		
	
		ArrayList<RentalInfoFF> infoow = owhandler.rentals();
		
		final BufferedWriter outLinkow = IOUtils.getBufferedWriter(inputFileow);
		try {
			outLinkow.write("personID   startTime   endTIme   startLink   endLink   distance   accessTime   egressTime");
			outLinkow.newLine();
		for(RentalInfoFF i: infoow) {
			
			
			outLinkow.write(i.toString());
			outLinkow.newLine();
			
		}
		outLinkow.flush();
		outLinkow.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		event.getControler().getEvents().removeHandler(owhandler);

		
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub

        owhandler = new OWEventsHandler(event.getControler().getScenario().getNetwork());

		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		if (event.getIteration() % 2 == 0) {
			event.getControler().getEvents().addHandler(owhandler);
			
		}
		
	}
	
		

}
