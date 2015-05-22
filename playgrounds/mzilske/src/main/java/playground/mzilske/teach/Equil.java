package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.vis.otfvis.OTFClientFile;

public class Equil {
	
	public static Logger logger = Logger.getLogger(Equil.class);
	
	public static void main(String[] args) {
		String configFile = "/Users/zilske/matsim-without-history/matsim/trunk/examples/equil/config.xml";
		final Map<Id, Double> lastEntered = new HashMap<Id, Double>();
		final Controler controller = new Controler(configFile);
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		final ArrayList<Double> link2 = new ArrayList<Double>();
		final ArrayList<Double> link15 = new ArrayList<Double>();
		final ArrayList<Double> link2time = new ArrayList<Double>();
		final ArrayList<Double> link15time = new ArrayList<Double>();
		final ArrayList<Double> iterations = new ArrayList<Double>();
		
		final LinkLeaveEventHandler linkLeaveEventHandler = new LinkLeaveEventHandler() {

			int it = 0;
			
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				if (event.getLinkId().toString().equals("2")) {
					lastEntered.put(event.getLinkId(), event.getTime());
					link2.set(it, link2.get(it) + 1);
					link2time.set(it, event.getTime());
				} else if (event.getLinkId().toString().equals("15")) {
					lastEntered.put(event.getLinkId(), event.getTime());
					link15.set(it, link15.get(it) + 1);
					link15time.set(it, event.getTime());
				}
			}

			@Override
			public void reset(int iteration) {
				if (iteration != 0) {
					for (Map.Entry<Id, Double> entry : lastEntered.entrySet()) {
						System.out.println(entry.getKey() + " " + entry.getValue());
					}
				}	
				System.out.println("Iteration: " + iteration);
				lastEntered.clear();
				it = iteration;
				iterations.add((double) iteration);
				link2.add(0.0);
				link15.add(0.0);
				link2time.add(86400.0);
				link15time.add(86400.0);
			}
						
		};
		
		controller.getEvents().addHandler(linkLeaveEventHandler);
		
		controller.run() ;
		for (Map.Entry<Id, Double> entry : lastEntered.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		
		Scenario sc = controller.getScenario() ;
		Config cf = sc.getConfig() ;
		String dir = cf.controler().getOutputDirectory();
		int iterNumber = cf.controler().getLastIteration();
		
		logger.warn("Output is in " + dir + ".  Use otfvis (preferably hardware-accelerated) to play movies." ) ; 
		
		XYLineChart chart = new XYLineChart("Traffic link 2", "iteration", "last time");
		chart.addSeries("link2", convert(iterations), convert(link2));
		chart.addSeries("link15", convert(iterations), convert(link15));
		chart.saveAsPng(dir + "wurst.png", 800, 600);
		
		XYLineChart chart2 = new XYLineChart("Traffic link 2", "iteration", "last time");
		chart2.addSeries("link2", convert(iterations), convert(link2time));
		chart2.addSeries("link15", convert(iterations), convert(link15time));
		chart2.saveAsPng(dir + "wurst2.png", 800, 600);
		
		new OTFClientFile(dir + "/ITERS/it."+iterNumber+"/"+iterNumber+".otfvis.mvi").run();
	}

	private static double[] convert(ArrayList<Double> iterations) {
		double[] result = new double[iterations.size()];
		int i=0;
		for(Double entry : iterations) {
			result[i] = entry;
			i++;
		}
		return result;
	}
	
}
