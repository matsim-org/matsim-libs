package playground.wdoering.debugvisualization.controller;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;

public class XYVxVyEventThread implements Runnable
{
	private XYVxVyEventsFileReader reader; 
	private String eventFile;

	public XYVxVyEventThread(XYVxVyEventsFileReader reader, String eventFile)
	{
		this.reader = reader;
		this.eventFile = eventFile;
	}

	@Override
	public void run()
	{
		//System.out.println("event thread running!");
		this.reader.parse(eventFile);
		// TODO Auto-generated method stub
		
	}

	
}
