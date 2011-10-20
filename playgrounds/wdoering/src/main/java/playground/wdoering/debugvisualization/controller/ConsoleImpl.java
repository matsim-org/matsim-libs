package playground.wdoering.debugvisualization.controller;
import java.sql.Timestamp;
import java.util.HashMap;


public class ConsoleImpl implements Console {

	HashMap<Integer, String> log;
	private boolean debug;
	
	public ConsoleImpl(boolean debug)
	{
		this.debug = debug;
	}

	public void Console()
	{
		log = new HashMap<Integer,String>();

		
	}
	
	@Override
	public void println(String string)
	{
		if (debug)
			System.out.println(string);
		
	}

	@Override
	public void print(String string)
	{
		if (debug)
			System.out.print(string);
	}

	
	
}
