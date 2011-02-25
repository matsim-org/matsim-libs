package air;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;



public class SfFlightTimeControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

	private SfFlightTimeEventHandler flightTimeHandler;
	private static final String SEPARATOR = "\t";


	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.flightTimeHandler = new SfFlightTimeEventHandler();
		event.getControler().getEvents().addHandler(this.flightTimeHandler);		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.writeStats(event);
		
	}
	
	
	private void writeStats(IterationEndsEvent event){
		try {
			String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "statistic.csv");
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			String header = "FlightNumber" + SEPARATOR + "ArrivalTime";
			writer.write(header);
			writer.newLine();
				
			for (Entry<Id, Double> entry : this.flightTimeHandler.returnArrival().entrySet()) {
						StringBuilder line = new StringBuilder();
						String[] keyEntries = entry.getKey().toString().split("_");	//extracting flight number from personId
						line.append(keyEntries[1]);	//flight number
						line.append(SEPARATOR);
						line.append(entry.getValue());	//arrival time
						writer.append(line.toString());
						writer.newLine();
					}						
			writer.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	
//	DgCottbusSylviaAnalysisControlerListener
	

}
