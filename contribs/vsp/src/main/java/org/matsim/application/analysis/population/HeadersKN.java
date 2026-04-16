package org.matsim.application.analysis.population;

// These are "excel" column headers so we use: (1) no spaces; (2) underscores instead of camel case.
class HeadersKN{
	public static final String ACTS_SCORE = "actsScore";
	public static final String INCOME_DECILE = "income_decile";
	public static final String IX_DIFF_REMAINING = "ixDiffRem";
	public static final String IX_DIFF_SWITCHING = "ixDiffSwi";
	public static final String MONEY_SCORE = "moneyScore";
	public static final String ANALYSIS_POPULATION = "analysis_population";

	public static final String TTIME_DIFF_REM_PT = "d_tTimeRemPt";
	public static final String TTIME_DIFF_REM_OTHER = "d_tTimeRemOth";

	public static final String U_TTIME_DIFF_REM_UNI_PT = "d_uTtimeRemUniPt";
	public static final String U_TTIME_DIFF_REM_UNI_OTHER = "d_uTtimeRemUniOth";

	public static final String U_TTIME_DIFF_REM_HET_PT = "d_uTtimeRemHet";
	public static final String U_TTIME_DIFF_REM_HET_OTHER = "d_uTtimeRemOther";

	public static final String U_TTIME_DIFF_SWI_UNI = "d_uTtimeSwiUni";
	public static final String U_TTIME_DIFF_SWI_HET = "d_uTtimeSwiHet";

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
	public static final String MATSIM_SCORE = "MATSimScore[u]";
	public static final String SCORE = "SCORE[u]";
	public static final String BENEFIT = "wtp4score";
	public static final String UTL_OF_MONEY = "UoM";
	public static final String TTIME = "ttime[h]";
	public static final String ACT_AT_END = "act_at_end" ;
	public static final String MONEY = "money";
	public static final String ASCS = "ascs";
	public static final String STUCK = "stuck";
	public static final String MUSE_h = "mUSE_h";

	// do not instantiate
	private HeadersKN(){}

	static String keyTwoOf( String str ) {
		return "T2."+str ;
	}
	static String deltaOf( String str ) {
		return "d_" + str;
	}

}
