package saleem.p0.resultanalysis;

import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
/**
 * A class for collecting link data for visualizing congestioin benefits of P0
 * 
 * @author Mohammad Saleem
 */
public class P0Visualisation {
	public static void main(String[] args){
		P0Visualisation visualiser = new P0Visualisation();
		String path = "./ihop2/matsim-input/config - P0.xml";
//		String path = "H:\\Mike Work\\input\\config.xml";
		Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		final EventsManager manager = EventsUtils.createEventsManager(config);
		VisualisationHandler handler = new VisualisationHandler(scenario.getNetwork());//first order second order effects of P0
		manager.addHandler(handler);
		final MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile("C:\\P0 Paper Runs\\HINP06\\ITERS\\it.2000\\2000.events.xml.gz");
		visualiser.printLinksDataToXML("C:\\P0 Paper Runs\\NP0LinksData.xml", handler.getRelativeSpeeds());

	}
	public void printLinksDataToXML(String path, Map<Id<Link>, Map<Double, Double>> relspeeds) {
		
		Element root = new Element("dynamicdata");
		root.setAttribute("starttime", "0");
		root.setAttribute("binsize", "1");
		root.setAttribute("bincount", "60");
		root.setAttribute("subclass", "floetteroed.utilities.visualization.LinkDataIO");
		
		Document doc = new Document();
		doc.setRootElement(root);
		
		Iterator<Id<Link>> iterator = relspeeds.keySet().iterator();
		
		while(iterator.hasNext()){
			Id<Link> linkid = iterator.next();
			Element entry  = new Element("entry");
			entry.setAttribute("key", linkid.toString());
			String content = "";
			Map<Double, Double> hourlyspeeds = relspeeds.get(linkid);
			Iterator<Double> hourlyiterator = hourlyspeeds.keySet().iterator();
			
			while(hourlyiterator.hasNext()){
				content = content + hourlyspeeds.get(hourlyiterator.next()) + " ";
				
			}
			entry.setAttribute("value", content);
			root.addContent(entry);
		}
		
		try{
			XMLOutputter xmlOutput = new XMLOutputter();
			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(path));
			System.out.println("File Saved!");
		}catch(Exception ex){
			;
		}
	}
}
