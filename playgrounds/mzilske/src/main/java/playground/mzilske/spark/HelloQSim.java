package playground.mzilske.spark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;

import static org.matsim.core.config.ConfigUtils.loadConfig;
import static spark.Spark.get;

public class HelloQSim {

	public static void main(String[] args) {
		get("/hello", (req, res) -> {
			ServletOutputStream outputStream = res.raw().getOutputStream();
			Config config = loadConfig(new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/matsim/examples/equil/config.xml"));
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			Controler controler = new Controler(config);
			controler.addControlerListener((IterationStartsListener) event -> {
				EventWriterXML handler = new EventWriterXML(new PrintStream(outputStream));
				event.getServices().getEvents().addHandler(handler);
				event.getServices().addControlerListener((IterationEndsListener) event1 -> handler.closeFile());
			});
			try {
				controler.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		});
	}

}
