package playground.wdoering.grips.evacuationanalysis.control;

import org.matsim.core.events.EventsReaderXMLv1;



public class EventReaderThread implements Runnable
{
	private final EventsReaderXMLv1 reader; 
	private final String eventFile;

	public EventReaderThread(EventsReaderXMLv1 reader, String eventFile)
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
