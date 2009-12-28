package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class SingletonUmlaufBuilderImpl implements UmlaufBuilder {
	
	private Collection<TransitLine> transitLines;
	
	public SingletonUmlaufBuilderImpl(Collection<TransitLine> transitLines) {
		this.transitLines = transitLines;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.pt.queuesim.UmlaufBuilder#build()
	 */
	public ArrayList<Umlauf> build() {
		int id = 0;
		ArrayList<Umlauf> umlaeufe = new ArrayList<Umlauf>();
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					Umlauf umlauf = new UmlaufImpl(new IdImpl(id++));
					umlauf.getUmlaufStuecke().add(umlaufStueck);
					umlaeufe.add(umlauf);
				}
			}
		}
		return umlaeufe;
	}

}
