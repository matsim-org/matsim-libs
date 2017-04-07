package playground.santiago.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;


/**All the relevant information given an events-file.
  **/
public class TrafficAnalysis {
	
	static String netFile = "../../../runs-svn/santiago/baseCase10pct/outputOfStep0_24T0/output_network.xml.gz";
	static String countsFile = "../../../runs-svn/santiago/baseCase10pct/outputOfStep0_24T0/output_counts.xml.gz";
	
	

	

		public static void main(String[] args) {
		
		int it=0;
		
		while (it<=300){
			int itAux = 300 + it;
			String eventsFile = "../../../runs-svn/santiago/baseCase10pct/outputOfStep0_24T0/ITERS/it." + it + "/" + it + ".events.xml.gz";
			String txtOutFile = "../../../runs-svn/santiago/baseCase10pct/outputOfStep0_24T0/analysis/" + itAux + ".countscompare.txt";
			writeCountsCompare(eventsFile, txtOutFile);
			it = it + 50;
		}


		}

	private static void writeCountsCompare (String events, String txtFile){
	Network network = readNetwork( netFile );
	Counts counts = readCounts( countsFile );
	VolumesAnalyzer volumes = readVolumes( network , events );
	double scaleFactor = 10; //TODO: BE AWARE OF THIS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! WARNING

	
	final CountsComparisonAlgorithm cca =
			new CountsComparisonAlgorithm(
					volumes,
					counts,
					network,
					scaleFactor );

		cca.run();
		
		try {
			final CountSimComparisonTableWriter ctw=
				new CountSimComparisonTableWriter(
						cca.getComparison(),
						Locale.ENGLISH);
			ctw.writeFile( txtFile );
		}
		catch ( Exception e ) {

		}
	}
	
	private static VolumesAnalyzer readVolumes ( Network network, String eventsFile ) {
		final VolumesAnalyzer volumes = new VolumesAnalyzer( 3600 , 24 * 3600 - 1 , network );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( volumes );
		new MatsimEventsReader( events ).readFile( eventsFile );
		return volumes;
	}
	
	private static Counts readCounts(final String countsFile) {
		final Counts counts = new Counts();
		new MatsimCountsReader( counts ).readFile( countsFile );
		return counts;
	}

	private static Network readNetwork(final String netFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( netFile );
		return sc.getNetwork();
	}

}
