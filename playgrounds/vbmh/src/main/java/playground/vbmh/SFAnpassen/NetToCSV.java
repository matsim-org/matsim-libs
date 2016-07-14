package playground.vbmh.SFAnpassen;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vbmh.util.CSVWriter;

public class NetToCSV {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF_PLUS/Scenario/140324_1/config.xml"));
		Network netz = scenario.getNetwork();
		CSVWriter writer = new CSVWriter("input/csvnetz");
		LinkedList<LinkedList<String>> liste = new LinkedList<LinkedList<String>>();
		for(Link link : netz.getLinks().values()){
			LinkedList <String> zeile = new LinkedList<String>();
			double x = link.getFromNode().getCoord().getX();
			double y = link.getFromNode().getCoord().getY();
			zeile.add(Double.toString(x));
			zeile.add(Double.toString(y));
			zeile.add(Double.toString(link.getCapacity()));
			zeile.add(link.getFromNode().getId().toString());
			liste.add(zeile);
		}
		writer.writeAll(liste);
		writer.close();
	}

}
