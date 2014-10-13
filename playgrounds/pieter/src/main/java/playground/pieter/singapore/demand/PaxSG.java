package playground.pieter.singapore.demand;

import java.io.Serializable;

import org.matsim.core.utils.collections.Tuple;

class PaxSG implements Serializable{
	final int paxId;
	final String foreigner;
	final HouseholdSG household;
	final boolean carLicenseHolder;
	final int age;
	final String sex;
	final int income;
	final String occup;
	final String chain;
	final String chainType;
	final double mainActStart;
	final double mainActDur;
	final String mainActType;
	String mainActFacility;
	final String modeSuggestion;
	Tuple<String[],double[]> bizActFrequencies;
	Tuple<String[],double[]> leisureActFrequencies;
	double distanceToWork;
	
	public PaxSG(int paxId, String foreigner, HouseholdSG household,
			boolean carLicenseHolder, int age, String sex, int income,
			String occup, String chain,String chainType, double mainActStart, double mainActDur,
			String mainActType, String modeSuggestion) {
		super();
		this.paxId = paxId;
		this.foreigner = foreigner;
		this.household = household;
		this.carLicenseHolder = carLicenseHolder;
		this.age = age;
		this.sex = sex;
		this.income = income;
		this.occup = occup;
		this.chain = chain;
		this.chainType = chainType;
		this.mainActStart = mainActStart;
		this.mainActDur = mainActDur;
		this.mainActType = mainActType;
		this.modeSuggestion = modeSuggestion;
	}
	
}
//full_pop_pid	foreigner	synth_hh_id	pax_id	car_lic	h1_hhid	pax_idx	id_res_facility	pcode_assigned	car_av	x_utm48n	y_utm48n
//1	foreigner	1	1	0	258371AO	258371AO_1	39466	507600	0	384955.4943	150647.587200001
//2	foreigner	1	2	0	258371AO	258371AO_2	39466	507600	0	384955.4943	150647.587200001
//3	foreigner	2	1	0	509332DR7	509332DR7_1	38951	409042	0	376723.0886	145649.8993
//4	foreigner	3	1	0	228064ER1	228064ER1_1	38058	449284	0	380918.53	144718.339299999
//5	foreigner	4	1	0	424169AO	424169AO_1	38738	575578	0	369952.7072	150871.133199999
//6	foreigner	4	2	0	424169AO	424169AO_2	38738	575578	0	369952.7072	150871.133199999