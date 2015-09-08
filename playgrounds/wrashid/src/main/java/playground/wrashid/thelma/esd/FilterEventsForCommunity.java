package playground.wrashid.thelma.esd;

import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.tools.events.FilterAgents;
import playground.wrashid.lib.tools.network.obj.ComplexRectangularSelectionArea;
import playground.wrashid.lib.tools.network.obj.RectangularArea;

public class FilterEventsForCommunity {

	public static void main(String[] args) {
		String eventsFile = "F:/data/THELMA/for esd/9-jan-2014/input/baseline2010/run.2010.150.events.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();

		String networkFilePath = "F:/data/THELMA/for esd/9-jan-2014/input/baseline2010/multimodalNetwork2010final.xml.gz";
		Network network = GeneralLib.readNetwork(networkFilePath);

		ComplexRectangularSelectionArea complexRectangularSelectionArea = defineAreaOfInterest();

		FilterAgentsWithHomeLocationInPredefinedAreas filterAgentsWithHomeLocationInPredefinedAreas = new FilterAgentsWithHomeLocationInPredefinedAreas(complexRectangularSelectionArea, network);		
		
		events.addHandler(filterAgentsWithHomeLocationInPredefinedAreas);

		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);

		//reader.readFile(eventsFile);
		reader.parse(eventsFile);

		FilterAgents.keepAgents(filterAgentsWithHomeLocationInPredefinedAreas.getIncludedAgents(), eventsFile, "C:/eTmp/filteredEventsForZernez-2010.txt.gz");
	
		Scenario scenario = GeneralLib.readScenario("F:/data/THELMA/for esd/9-jan-2014/input/baseline2010/run.2010.output_plans.xml.gz", networkFilePath,"F:/data/THELMA/for esd/9-jan-2014/input/baseline2010/facilities2012secondary.xml.gz");
	
		
		LinkedList<Person> persons=new LinkedList<Person>();
		
		System.out.println("personId selected for filtered plans file");
		for (Id<Person> personId:filterAgentsWithHomeLocationInPredefinedAreas.getIncludedAgents()){
			persons.add(scenario.getPopulation().getPersons().get(personId));
			System.out.println(personId);
		}
		
		GeneralLib.writePersons(persons, "C:/eTmp/plansForZernez2010.xml.gz", network,(ScenarioImpl) scenario);
	}

	private static ComplexRectangularSelectionArea defineAreaOfInterest() {
		LinkedList<RectangularArea> includeInSection = new LinkedList<RectangularArea>();
		LinkedList<RectangularArea> excludeFromSelection = new LinkedList<RectangularArea>();

		//RectangularArea rectangleWattwil = new RectangularArea(new CoordImpl(719500,234500 ), new CoordImpl(729700, 243600));
		//includeInSection.add(rectangleWattwil);
		//RectangularArea rectangleBuchs = new RectangularArea(new CoordImpl(748300,222700   ), new CoordImpl(756800, 230000));
		//includeInSection.add(rectangleBuchs);
		RectangularArea rectangleZernez = new RectangularArea(new Coord((double) 792900, (double) 165100), new Coord((double) 817400, (double) 179050));
		includeInSection.add(rectangleZernez);
		
		ComplexRectangularSelectionArea complexRectangularSelectionArea = new ComplexRectangularSelectionArea(includeInSection,
				excludeFromSelection);
		return complexRectangularSelectionArea;
	}

	private static class FilterAgentsWithHomeLocationInPredefinedAreas implements  ActivityStartEventHandler {

		private final ComplexRectangularSelectionArea complexRectangularSelectionArea;
		
		private HashSet<Id<Person>> includedAgents=new HashSet<>();

		private final Network network;

		public HashSet<Id<Person>> getIncludedAgents() {
			return includedAgents;
		}

		public FilterAgentsWithHomeLocationInPredefinedAreas(ComplexRectangularSelectionArea complexRectangularSelectionArea, Network network) {
			this.complexRectangularSelectionArea = complexRectangularSelectionArea;
			this.network = network;
		}

		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			Coord linkCoordinate = network.getLinks().get(event.getLinkId()).getCoord();
			if (isHomeActivity(event) && isActivityHappeningInSelectionArea(linkCoordinate)){
				includedAgents.add(event.getPersonId());
			}
		}

		private boolean isActivityHappeningInSelectionArea(Coord linkCoordinate) {
			return complexRectangularSelectionArea.isInArea(linkCoordinate);
		}

		private boolean isHomeActivity(ActivityStartEvent event) {
			return event.getActType().equalsIgnoreCase("home");
		}
	}
}
