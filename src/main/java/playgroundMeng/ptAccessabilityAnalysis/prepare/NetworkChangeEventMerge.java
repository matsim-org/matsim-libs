package playgroundMeng.ptAccessabilityAnalysis.prepare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;

public class NetworkChangeEventMerge {
	List<NetworkChangeEvent> changeEvents = new ArrayList<>();
	String outputFile = "mergedNetworkChangeEvent.xml";
	String districtString = "Ricklingen,Bornum,Oststadt,Linden-Nord,Isernhagen-Süd,Vinnhorst,Linden-Mitte,Heideviertel,Badenstedt,Döhren,Mitte,Linden-Süd,Seelhorst,Sahlkamp,Bothfeld,Mittelfeld,Wettbergen,Groß-Buchholz,Vahrenwald,Wülfel,Bemerode,Brink-Hafen,Wülferode,Misburg-Nord,Nordhafen,List,"
			+ "Kirchrode,Marienwerder,Misburg-Süd,Burg,Waldheim,Ledeburg,Calenberger Neustadt,Herrenhausen,Südstadt,VW Werk,Oberricklingen,Hainholz,Zoo,Mühlenberg,Leinhausen,Bult,Vahrenheide,Lahe,Limmer,Kleefeld,Waldhausen,Ahlem,Nordstadt,Anderten,Davenstedt";
	
	
	@Inject
	Network network;
	@Inject
	PtAccessabilityConfig ptAccessabilityConfig;
	
	public NetworkChangeEventMerge() {
	}
	
	public void merge() {
		
		String[] array = districtString.split(",");
		List<String> list = Arrays.asList(array);
		for(String string : list) {
			 new NetworkChangeEventsParser(network, changeEvents).readFile(ptAccessabilityConfig.getOutputDirectory()+"networkChangeEvent_"+string+"_area.xml");
		}
		new NetworkChangeEventsWriter().write(ptAccessabilityConfig.getOutputDirectory()+outputFile, this.changeEvents);
		System.out.println("finish");
	}

}
