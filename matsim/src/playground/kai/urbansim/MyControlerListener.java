package playground.kai.urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 * 
 * @author nagel
 *
 */
public class MyControlerListener implements ShutdownListener {

	public void notifyShutdown(ShutdownEvent event) {

	}

}
