package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.NetworkLayer;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicles;

public class ReconstructingUmlaufBuilder implements UmlaufBuilder {

	private static final Comparator<UmlaufStueck> departureTimeComparator = new Comparator<UmlaufStueck>() {

		public int compare(UmlaufStueck o1, UmlaufStueck o2) {
			return Double.compare(o1.getDeparture().getDepartureTime(), o2.getDeparture().getDepartureTime());
		}
		
	};
	
	private Collection<TransitLine> transitLines;
	private BasicVehicles basicVehicles;
	private Map<Id,Umlauf> umlaeufe = new HashMap<Id,Umlauf>();
	private ArrayList<UmlaufStueck> umlaufStuecke;
	
	private UmlaufInterpolator umlaufInterpolator;
	
	
	public ReconstructingUmlaufBuilder(NetworkLayer network, Collection<TransitLine> transitLines,
			BasicVehicles basicVehicles) {
		super();
		this.umlaufInterpolator = new UmlaufInterpolator(network);
		this.transitLines = transitLines;
		this.basicVehicles = basicVehicles;
	}

	public Collection<Umlauf> build() {
		createEmptyUmlaeufe();
		createUmlaufStuecke();
		for (UmlaufStueck umlaufStueck : umlaufStuecke) {
			Umlauf umlauf = umlaeufe.get(umlaufStueck.getDeparture().getVehicleId());
			umlaufInterpolator.addUmlaufStueckToUmlauf(umlaufStueck, umlauf);
		}
		return umlaeufe.values();
	}

	private void createEmptyUmlaeufe() {
		for (BasicVehicle basicVehicle : basicVehicles.getVehicles().values()) {
			UmlaufImpl umlauf = new UmlaufImpl(basicVehicle.getId());
			umlauf.setVehicleId(basicVehicle.getId());
			umlaeufe.put(umlauf.getId(), umlauf);
		}
	}

	private void createUmlaufStuecke() {
		this.umlaufStuecke = new ArrayList<UmlaufStueck>();
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route,
							departure);
					umlaufStuecke.add(umlaufStueck);
				}
			}
		}
		Collections.sort(this.umlaufStuecke, departureTimeComparator);
	}

}
