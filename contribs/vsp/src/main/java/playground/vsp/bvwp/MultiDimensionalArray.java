package playground.vsp.bvwp;


import static playground.vsp.bvwp.Key.*;



public class MultiDimensionalArray {
	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "ODAttribute".  "Mengen" sind vielleicht nicht wirklich "Attribute" im Sinne der BVWP-Nomenklatur,
	 * aber abgesehen davon spricht eigentlich nichts dagegen.  kai,benjamin, sep'12
	 * </ul>
	 */
	enum Attribute { XX, Distanz_km, Reisezeit_h, Nutzerkosten_Eu, Produktionskosten_Eu, access_hrs }

	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "DemandSegment".  But refactoring does not seem to work.  Try with other eclipse (this one was
	 * Galileo).  kai/benjamin, sep'12
	 *</ul>
	 */
	enum DemandSegment { GV, PV_NON_COMMERCIAL, PV_GESCHAEFT, PV_ARBEIT, PV_AUSBILDUNG, PV_EINKAUF, PV_URLAUB, PV_SONST }

	enum Mode { Strasse, Bahn }
	
	public static void main(String[] args) {
		Values m = new Values() ;
		
		double sum = 0 ;

		for ( DemandSegment segm : DemandSegment.values() ) {
			for ( Mode mode : Mode.values() ) {
				sum += m.get(makeKey(mode,segm, Attribute.Distanz_km )) ;
			}
		}
		
		
		
		
	}
	
}
