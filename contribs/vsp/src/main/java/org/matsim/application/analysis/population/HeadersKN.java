package org.matsim.application.analysis.population;

class HeadersKN{
	// These are "excel" column headers so we use: (1) no spaces; (2) underscores instead of camel case.

	public static String personId = "personId";
	public static String tripIdx = "tripIdx";
	public static String mode = "mode";
	public static String vttsh = "VTTS_[Eu/h]";
	public static String muttsh = "mUTTS_[u/h]";
	public static String activity = "activity";
	public static String activityDuration = "act_dur";
	public static String typicalDuration = "typ_dur";
	public static String mUoM = "mUoM";
	public static String muslh = "mUSL_[u/h]";
	private HeadersKN(){} // do not instantiate
}
