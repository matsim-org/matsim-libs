package playground;

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
	 */
}
