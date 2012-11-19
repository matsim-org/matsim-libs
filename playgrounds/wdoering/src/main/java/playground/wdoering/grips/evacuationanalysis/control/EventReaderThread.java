package playground.wdoering.grips.evacuationanalysis.control;

import playground.gregor.sim2d_v3.events.XYVxVyEventsFileReader;


public class EventReaderThread implements Runnable
{
	private final XYVxVyEventsFileReader reader; 
	private final String eventFile;

	public EventReaderThread(XYVxVyEventsFileReader reader, String eventFile)
	{
		this.reader = reader;
		this.eventFile = eventFile;
	}

	@Override
	public void run()
	{
		//System.out.println("event thread running!");
		this.reader.parse(this.eventFile);
		// TODO Auto-generated method stub
		
	}

	
}
