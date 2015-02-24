package playground.balac.allcsmodestest.controler.listeneronlymembers;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.allcsmodestest.controler.listener.NoParkingEventHandler;
import playground.balac.allcsmodestest.controler.listener.NoParkingEventHandler.NoParkingInfo;
import playground.balac.allcsmodestest.controler.listener.NoVehicleEventHandler;
import playground.balac.allcsmodestest.controler.listener.NoVehicleEventHandler.NoVehicleInfo;
import playground.balac.allcsmodestest.controler.listeneronlymembers.FFEventsHandler.RentalInfoFF;
import playground.balac.allcsmodestest.controler.listeneronlymembers.OWEventsHandler.RentalInfoOW;
import playground.balac.allcsmodestest.controler.listeneronlymembers.RTEventsHandler.RentalInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CarsharingListener implements StartupListener, IterationEndsListener, IterationStartsListener{
	RTEventsHandler cshandler;
	FFEventsHandler ffhandler;
	OWEventsHandler owhandler;
	NoVehicleEventHandler noVehicleHandler;
	NoParkingEventHandler noParkingHandler;
	Controler controler;
	int frequency = 0;
	
	public CarsharingListener(Controler controler, int frequency) {
				
		this.controler = controler;
		this.frequency = frequency;
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getIteration() % this.frequency == 0) {
		
		ArrayList<RentalInfo> info = cshandler.rentals();
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "RT_CS"));
		try {
			outLink.write("personID   startTime   endTIme   startLink   distance   accessTime   egressTime	vehicleID");
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
		
		final BufferedWriter outLinkff = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "FF_CS"));
		try {
			outLinkff.write("personID   startTime   endTIme   startLink   endLink   distance   accessTime	vehicleID");
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
		
		ArrayList<RentalInfoOW> infoow = owhandler.rentals();
		
		final BufferedWriter outLinkow = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "OW_CS"));
		try {
			outLinkow.write("personID   startTime   endTIme   startLink   endLink   distance   accessTime   egressTime	vehicleID");
			outLinkow.newLine();
		for(RentalInfoOW i: infoow) {
			
			
			outLinkow.write(i.toString());
			outLinkow.newLine();
			
		}
		outLinkow.flush();
		outLinkow.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<NoVehicleInfo> infoNoVehicles = noVehicleHandler.info();
		
		final BufferedWriter outNoVehicle = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "No_Vehicle_Stats.txt"));
		try {
			outNoVehicle.write("linkID	CSType");
			outNoVehicle.newLine();
		for(NoVehicleInfo i: infoNoVehicles) {
			
			
			outNoVehicle.write(i.toString());
			outNoVehicle.newLine();
			
		}
		outNoVehicle.flush();
		outNoVehicle.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<NoParkingInfo> infoNoParking = noParkingHandler.info();
		
		final BufferedWriter outNoParking = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(event.getIteration(), "No_Parking_Stats.txt"));
		try {
			outNoParking.write("linkID	CSType");
			outNoParking.newLine();
		for(NoParkingInfo i: infoNoParking) {
			
			
			outNoParking.write(i.toString());
			outNoParking.newLine();
			
		}
		outNoParking.flush();
		outNoParking.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		event.getControler().getEvents().removeHandler(this.cshandler);
		event.getControler().getEvents().removeHandler(this.ffhandler);
		event.getControler().getEvents().removeHandler(this.owhandler);
		event.getControler().getEvents().removeHandler(this.noVehicleHandler);
		event.getControler().getEvents().removeHandler(this.noParkingHandler);
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub

        this.cshandler = new RTEventsHandler();

        this.ffhandler = new FFEventsHandler();

        this.owhandler = new OWEventsHandler();
		
		this.noVehicleHandler = new NoVehicleEventHandler();	
		
		this.noParkingHandler = new NoParkingEventHandler();
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		if (event.getIteration() % this.frequency == 0) {
			event.getControler().getEvents().addHandler(this.cshandler);
			event.getControler().getEvents().addHandler(this.ffhandler);
			event.getControler().getEvents().addHandler(this.owhandler);
			event.getControler().getEvents().addHandler(this.noVehicleHandler);
			event.getControler().getEvents().addHandler(this.noParkingHandler);
		}
		
	}
	
		

}
