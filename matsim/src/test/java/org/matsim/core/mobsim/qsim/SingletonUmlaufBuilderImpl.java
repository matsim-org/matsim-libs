package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class SingletonUmlaufBuilderImpl implements UmlaufBuilder {
	
	private Collection<TransitLine> transitLines;
	
	public SingletonUmlaufBuilderImpl(Collection<TransitLine> transitLines) {
		this.transitLines = transitLines;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.pt.queuesim.UmlaufBuilder#build()
	 */
	@Override
	public ArrayList<Umlauf> build() {
		int id = 0;
		ArrayList<Umlauf> umlaeufe = new ArrayList<Umlauf>();
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				Gbl.assertNotNull(route.getRoute()); // will fail much later if this is null.  kai, may'17
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					Umlauf umlauf = new UmlaufImpl(Id.create(id++, Umlauf.class));
					umlauf.getUmlaufStuecke().add(umlaufStueck);
					umlaeufe.add(umlauf);
				}
			}
		}
		return umlaeufe;
	}

}
