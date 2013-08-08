package playground.wdoering.debugvisualization.controller;

import playground.wdoering.oldstufffromgregor.XYVxVyEventsFileReader;



public class XYVxVyEventThread implements Runnable
{
	private final XYVxVyEventsFileReader reader; 
	private final String eventFile;

	public XYVxVyEventThread(XYVxVyEventsFileReader reader, String eventFile)
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
