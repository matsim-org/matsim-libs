package playground.vbmh.controler;

public class VMConfig {
	public static double walkingSpeed = 4860; //m/h //!!  BIS Skript II S. 105
	public static double pricePerKWH = 0.12; //$/KWH !!Gehoert nicht hier her (Quelle http://shrinkthatfootprint.com/average-electricity-prices-kwh)
	public static int maxDistance = 1000; //Maximaler Umkreis in dem Parkplaetze gesucht werden
	public static double evSavingsPerKM = 8.308/1000000; //[util/m] Gespartes Benzin pro KM
	public static double betaPayMoney = - 0.062; //Util/Dollar <0
	public static double betaWalkPMetre = -0.000234567 ; //Util/Meter gehen <0
	public static double betaSOCMean = 4.5; //mittelwert in $
	public static double betaSOCSD = 1; // standardabweichung in $
	public static double betaReserve = 30;
	
	public static double LMSOCa = -3.889; //x^2 term
	public static double LMSOCb = 10.97; // x term
	public static double LMSOCc = -2.159; // constant
	public static double LMSOCd = 2.425; // constant outside of exp
	public static double LMSOCe = 0.0454411; //shift down
	public static double LMSOCf = 1.067; //scale up
	
	
}
