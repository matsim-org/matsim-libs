package playground.wrashid.thelma.esd;

import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.events.FilterAgents;
import playground.wrashid.lib.tools.network.obj.ComplexRectangularSelectionArea;
import playground.wrashid.lib.tools.network.obj.RectangularArea;

public class FilterEventsForCommunity {

	public static void main(String[] args) {
		String eventsFile = "H:/data/cvs/ivt/studies/switzerland/results/census2000v2/runs/ivtch-10pctcv2-rev9473-run00/ITERS/it.200/200.events.txt.gz";
		EventsManagerImpl events = new EventsManagerImpl();

		NetworkImpl network = GeneralLib.readNetwork("H:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml");

		ComplexRectangularSelectionArea complexRectangularSelectionArea = defineAreaOfInterest();

		FilterAgentsWithHomeLocationInPredefinedAreas filterAgentsWithHomeLocationInPredefinedAreas = new FilterAgentsWithHomeLocationInPredefinedAreas(complexRectangularSelectionArea, network);		
		
		events.addHandler(filterAgentsWithHomeLocationInPredefinedAreas);

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);

		reader.readFile(eventsFile);

		FilterAgents.keepAgents(filterAgentsWithHomeLocationInPredefinedAreas.getIncludedAgents(), "H:/data/cvs/ivt/studies/switzerland/results/census2000v2/runs/ivtch-10pctcv2-rev9473-run00/ITERS/it.200/200.events.txt.gz", "C:/eTmp/filteredEventsForWattwilAndBuchs.txt.gz");
	
		Scenario scenario = GeneralLib.readScenario("H:/data/cvs/ivt/studies/switzerland/results/census2000v2/runs/ivtch-10pctcv2-rev9473-run00/ITERS/it.200/200.plans.xml.gz", "H:/data/cvs/ivt/studies/switzerland/networks/ivtch/network.xml","H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz");
	
		
		LinkedList<Person> persons=new LinkedList<Person>();
		
		for (Id personId:filterAgentsWithHomeLocationInPredefinedAreas.getIncludedAgents()){
			persons.add(scenario.getPopulation().getPersons().get(personId));
		}
		
		GeneralLib.writePersons(persons, "C:/eTmp/plansForWattwilAndBuchs.xml.gz", network);
	}

	private static ComplexRectangularSelectionArea defineAreaOfInterest() {
		LinkedList<RectangularArea> includeInSection = new LinkedList<RectangularArea>();
		LinkedList<RectangularArea> excludeFromSelection = new LinkedList<RectangularArea>();

		RectangularArea rectangleWattwil = new RectangularArea(new CoordImpl(719500,234500 ), new CoordImpl(729700, 243600));
		includeInSection.add(rectangleWattwil);
		RectangularArea rectangleBuchs = new RectangularArea(new CoordImpl(748300,222700   ), new CoordImpl(756800, 230000));
		includeInSection.add(rectangleBuchs);

		ComplexRectangularSelectionArea complexRectangularSelectionArea = new ComplexRectangularSelectionArea(includeInSection,
				excludeFromSelection);
		return complexRectangularSelectionArea;
	}

	private static class FilterAgentsWithHomeLocationInPredefinedAreas implements  ActivityStartEventHandler {

		private final ComplexRectangularSelectionArea complexRectangularSelectionArea;
		
		private HashSet<Id> includedAgents=new HashSet<Id>();

		private final NetworkImpl network;

		public HashSet<Id> getIncludedAgents() {
			return includedAgents;
		}

		public FilterAgentsWithHomeLocationInPredefinedAreas(ComplexRectangularSelectionArea complexRectangularSelectionArea, NetworkImpl network) {
			this.complexRectangularSelectionArea = complexRectangularSelectionArea;
			this.network = network;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
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
