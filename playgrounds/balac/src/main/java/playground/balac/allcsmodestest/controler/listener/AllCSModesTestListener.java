package playground.balac.allcsmodestest.controler.listener;

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

import playground.balac.allcsmodestest.controler.listener.CSEventsHandler.RentalInfo;
import playground.balac.allcsmodestest.controler.listener.FFEventsHandler.RentalInfoFF;

public class AllCSModesTestListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	CSEventsHandler cshandler;
	FFEventsHandler ffhandler;
	OWEventsHandler owhandler;
	String inputFilerb;
	String inputFileow;
	String inputFileff;
	
	public AllCSModesTestListener(String inputFilerb, String inputFileff, String inputFileow) {
		
		this.inputFilerb = inputFilerb;
		this.inputFileff = inputFileff;
		this.inputFileow = inputFileow;
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getIteration() % 10 == 0) {
		
		ArrayList<RentalInfo> info = cshandler.rentals();
		
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
		
		ArrayList<RentalInfoFF> infoff = ffhandler.rentals();
		
		final BufferedWriter outLinkff = IOUtils.getBufferedWriter(inputFileff);
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
		
		ArrayList<playground.balac.allcsmodestest.controler.listener.OWEventsHandler.RentalInfoFF> infoow = owhandler.rentals();
		
		final BufferedWriter outLinkow = IOUtils.getBufferedWriter(inputFileow);
		try {
			outLinkow.write("personID   startTime   endTIme   startLink   endLink   distance   accessTime   egressTime");
			outLinkow.newLine();
		for(playground.balac.allcsmodestest.controler.listener.OWEventsHandler.RentalInfoFF i: infoow) {
			
			
			outLinkow.write(i.toString());
			outLinkow.newLine();
			
		}
		outLinkow.flush();
		outLinkow.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		event.getControler().getEvents().removeHandler(cshandler);
		event.getControler().getEvents().removeHandler(ffhandler);
		event.getControler().getEvents().removeHandler(owhandler);

		
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
		cshandler = new CSEventsHandler(event.getControler().getNetwork());
		
		ffhandler = new FFEventsHandler(event.getControler().getNetwork());
		
		owhandler = new OWEventsHandler(event.getControler().getNetwork());

		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		if (event.getIteration() % 10 == 0) {
			event.getControler().getEvents().addHandler(cshandler);
			event.getControler().getEvents().addHandler(ffhandler);
			event.getControler().getEvents().addHandler(owhandler);
			
		}
		
	}
	
		

}
