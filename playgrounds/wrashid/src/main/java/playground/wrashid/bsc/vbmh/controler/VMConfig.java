package playground.wrashid.bsc.vbmh.controler;

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
}
