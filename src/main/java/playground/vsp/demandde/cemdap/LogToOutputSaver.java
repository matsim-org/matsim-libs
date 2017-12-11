package playground.vsp.demandde.cemdap;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;

public class LogToOutputSaver {
	final private static Logger log = Logger.getLogger(LogToOutputSaver.class);
	
	public static void setOutputDirectory(String outputBase) {
		try	{
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputBase);
			log.info("Logging will be stored at " + outputBase);
		} catch (IOException e)	{
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}
}