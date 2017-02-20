package saleem.stockholmmodel.resultanalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

//This class calculates different relevant stats about stockholm scenario
public class StockholmScenarioAnalysis {
	public static void main(String[] args){
		String path = "./ihop2/matsim-input/configoptimisationcarpt.xml";
		final Config config = ConfigUtils.loadConfig(path);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final EventsManager eventsNoP01 = EventsUtils.createEventsManager(config);
		StockholmScenarioStatisticsCalculator handler = new StockholmScenarioStatisticsCalculator(scenario.getPopulation().getPersons());
		eventsNoP01.addHandler(handler);
		final MatsimEventsReader readerNoP01 = new MatsimEventsReader(eventsNoP01);
		readerNoP01.readFile("./ihop2/matsim-input/500.events.xml.gz");
		handler.getTT();
		handler.getTotalrips();
	}
}
