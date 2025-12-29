package org.matsim.application.analysis.population;

// These are "excel" column headers so we use: (1) no spaces; (2) underscores instead of camel case.
class HeadersKN{
	public static final String ACTS_SCORE = "acts_score";
	public static final String IX_DIFF_REMAINING = "ixDiffRem";
	public static final String IX_DIFF_SWITCHING = "ixDiffSwi";
	public static final String MONEY_SCORE = "money_score";
	public static final String ANALYSIS_POPULATION = "analysis_population";
	public static final String W1_TTIME_DIFF_REM = "wUniTtimeDiffRem";

	public static final String W2_TTIME_DIFF_REM = "wHetTtimeDiffRem";
	public static final String W2_TTIME_DIFF_SWI = "wHetTtimeDiffSwi";
	public static final String W1_TTIME_DIFF_SWI = "wUniTtimeDiffSwi";

	public static final String personId = "personId";
	public static final String tripIdx = "tripIdx";
	public static final String mode = "mode";
	public static final String vttsh = "VTTS_[Eu/h]";
	public static final String muttsh = "mUTTS_[u/h]";
	public static final String activity = "activity";
	public static final String activityDuration = "actDur";
	public static final String typicalDuration = "typDur";
	public static final String mUoM = "mUoM";
	public static final String muslh = "mUSL_[u/h]";

	public static final String ACT_SEQ = "actSeq";
	public static final String MODE_SEQ = "modeSeq";
//	public static final String ADDTL_TRAV_SCORE = "addtlTravScore";
	public static final String U_TRAV_DIRECT = "UTravDir";
	public static final String U_LINESWITCHES = "U_iX";
	public static final String MUTTS_H = "mUTTS[/1h]";
	public static final String TRIP_IDX = "tripNr";
//	public static final String WEIGHTED_MONEY = "w_money[u]";
//	public static final String WEIGHTED_TTIME = "w_ttime[u]";
	public static final String MODE ="modeSeq";
	public static final String PERSON_ID = "personId";
	public static final String INCOME = "income";
	public static final String SCORE = "SCORE[u]";
	public static final String BENEFIT = "wtp4score";
	public static final String UTL_OF_MONEY = "UoM";
	public static final String TTIME = "ttime[h]";
	public static final String ACT_AT_END = "act_at_end" ;
	public static final String MONEY = "money";
	public static final String ASCS = "ascs";
	public static final String STUCK = "stuck";
	public static final String MUSL_h = "mUSL_h";

	// do not instantiate
	private HeadersKN(){}

	static String keyTwoOf( String str ) {
		return "T2."+str ;
	}
	static String deltaOf( String str ) {
		return "d_" + str;
	}

}
