package gunnar.ihop2.integration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import floetteroed.utilities.math.Histogram;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TollLinkAnalyzer implements LinkEnterEventHandler {

	private static final String[] tollLinkIdStr = new String[] {

	"101350_AB", "112157_AB", "9216_AB", "13354_AB", "18170_AB", "35424_AB",
			"44566_AB", "51930_AB", "53064_AB", "68760_AB", "74188_AB",
			"79555_AB", "90466_AB", "122017_AB", "122017_BA", "22626_AB",
			"77626_AB", "90317_AB", "92866_AB", "110210_AB", "2453_AB",
			"6114_AB", "6114_BA", "25292_AB", "28480_AB", "34743_AB",
			"124791_AB", "71617_AB", "71617_BA", "80449_AB", "96508_AB",
			"96508_BA", "108353_AB", "113763_AB", "121908_AB", "121908_BA",
			"52416_BA", "125425_AB"

	};

	private final Map<String, Histogram> linkIdStr2entryHist;

	public TollLinkAnalyzer() {
		this.linkIdStr2entryHist = new LinkedHashMap<>();
		for (String linkIdStr : tollLinkIdStr) {
			this.linkIdStr2entryHist.put(linkIdStr,
					Histogram.newHistogramWithUniformBins(0.0, 3600.0, 24));
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<String, Histogram> entry : linkIdStr2entryHist
				.entrySet()) {
			result.append(entry.getKey());
			for (int i = 1; i <= 24; i++) {
				result.append("\t");
				result.append(entry.getValue().cnt(i)); // bin 0 is before t=0.0
			}
			result.append("\n");
		}
		return result.toString();
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// System.out.println(event);
		final Histogram hist = this.linkIdStr2entryHist.get(event.getLinkId()
				.toString());
		if (hist != null) {
			hist.add(event.getTime());
		}
	}

	public static void main(String[] args) {

		final String path = "/Users/gunnarfl/Desktop/TEST_MATSIM_DUMMY/";

		final Config config = ConfigUtils.loadConfig(path
				+ "archive/without-toll/input/matsim-config.xml");

		final TollLinkAnalyzer withoutToll = new TollLinkAnalyzer();
		EventsManager events = EventsUtils.createEventsManager(config);
		events.addHandler(withoutToll);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(path
				+ "archive/without-toll/matsim-output.1/ITERS/it.200/200.events.xml.gz");

		final TollLinkAnalyzer withToll = new TollLinkAnalyzer();
		events = EventsUtils.createEventsManager(config);
		events.addHandler(withToll);
		reader = new MatsimEventsReader(events);
		reader.readFile(path
				+ "archive/with-toll/matsim-output.1/ITERS/it.200/200.events.xml.gz");

		System.out.println();
		for (String linkIdStr : tollLinkIdStr) {

			System.out.print(linkIdStr + "(no-toll)");
			for (int i = 1; i <= 24; i++) {
				System.out.print("\t");
				// bin 0 is before t=0.0
				System.out.print(withoutToll.linkIdStr2entryHist.get(linkIdStr)
						.cnt(i));
			}
			System.out.println();
			System.out.print(linkIdStr + "(toll)");
			for (int i = 1; i <= 24; i++) {
				System.out.print("\t");
				// bin 0 is before t=0.0
				System.out.print(withToll.linkIdStr2entryHist.get(linkIdStr)
						.cnt(i));
			}
			System.out.println();
			System.out.println();
		}
		System.out.println();

	}

}
