package playground.fzwick;


	import java.awt.List;
	import java.io.BufferedReader;
	import java.io.BufferedWriter;
	import java.io.FileNotFoundException;
	import java.io.FileReader;
	import java.io.FileWriter;
	import java.io.IOException;
	import java.util.ArrayList;
	import org.matsim.api.core.v01.network.Link;
	import org.matsim.api.core.v01.network.Network;
	import org.matsim.core.network.NetworkUtils;
	import org.matsim.core.network.io.MatsimNetworkReader;
	import org.matsim.counts.Count;
	import org.matsim.counts.Counts;
	import org.matsim.counts.MatsimCountsReader;

	public class CountsWriter {	

		public static String inputNetwork ="C:/Users/Felix/Documents/VSP/shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds.xml.gz";
		public static String inputCounts ="C:/Users/Felix/Documents/VSP/shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/vmz_di-do_shortIds.xml";
		public static String output ="C:/Users/Felix/Documents/VSP/shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/OSM-nodes.csv";
//		ArrayList<String> counts = new ArrayList<String>();
		
		
		
		public static void main(String[] args) {
			try {
				Network network = NetworkUtils.createNetwork();
				new MatsimNetworkReader(network).readFile(inputNetwork);
				
				Counts<Link> counts = new Counts<>();
		        MatsimCountsReader cReader = new MatsimCountsReader(counts);
		        cReader.readFile(inputCounts);
		        
				BufferedWriter writer = new BufferedWriter(new FileWriter(output));
				
				String header = "countID\tOSM-From-Node-ID\tOSM-To-Node-ID";
				writer.write(header);
				writer.newLine();
				for(Count<Link> count : counts.getCounts().values()) {
				writer.write(count.getCsLabel().toString()+"\t"+count.getId().toString());
				writer.newLine();
					}
				writer.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			

		}
		
		
		

	}