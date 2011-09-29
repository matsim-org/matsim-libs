package playground.wdoering.debugvisualization.controller;
import java.sql.Timestamp;
import java.util.HashMap;


public class ConsoleImpl implements Console {

	HashMap<Integer, String> log;
	
	public void Console()
	{
		log = new HashMap<Integer,String>();

		
	}
	
	@Override
	public void println(String string)
	{
		System.out.println(string);
		
	}

	@Override
	public void print(String string)
	{
		System.out.print(string);
	}

	
	
}
