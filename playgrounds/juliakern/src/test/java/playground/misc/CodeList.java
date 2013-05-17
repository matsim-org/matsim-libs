package playground.misc;

public class CodeList {

	/*
	 * Hbefa Cold EmissionFactorKey: equals - Methode funktioniert unterschiedlich in verschiedene Richtungen 
	 * (bei unvollst. daten)
	 * siehe TestHbefaColdEmissionFactorKey
	 * 
	 * Hbefa Cold EmissionFactorKey: equals - keinPollutant.equals.mitPollutant stuerzt nicht ab!
	 * 
	 * Hbefa Cold EmissionFactorKey: equals - keineParkingTime.equals.ParkingTime stuerzt nicht ab
	 * das liegt daran, wie Int verglichen werden
	 * 
	 * wenn einige Attribute unterschiedlich gesetzt sind, gibt die equals methode false zurueck, bevor 
	 * sie wegen fehlender werte abstuerzt -> umschreiebn?
	 * 
	 * genauso: Hbefa Warm Emission FactorKey
	 */
	
	
	/*
	 * HbefaVehicleAttributes - equals:
	 * 	         return
	            hbefaTechnology.equals(key.getHbefaTechnology())
	         && hbefaSizeClass.equals(key.getHbefaSizeClass())
	         && hbefaEmConcept.equals(key.getHbefaEmConcept()); 
	 * 
	 * aehnlich wie oben: ist der erste wert false, kommt false zurueck, auch wenn die anderen keine
	 * passenden objekte sind.
	 * 
	 * */
	
	/*
	 * Warm Pollutant hat keine getValue-Methode, cold schon
	 */
	
	/*
	 * EmissionUtils
	 * testEmissionUtils - 290 warum sortierte listen?
	 * ursprueglich EmissionUtils 258
	 * viel oefter zugreifen als schreiben?
	 */
	

	
}
