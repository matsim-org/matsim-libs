package playground.pieter.singapore.hits;


import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.matsim.api.core.v01.Coord;


class HITSStage extends HITSElement implements Serializable {
	/**
	 * 
	 */
	HITSTrip trip;
	private int msno_main;
	private String h1_hhid;
	int pax_id;
	int trip_id;
	int stage_id;
	String t10_mode;
	int t10a_walktime;
	private String t10i_modeother;
	String t11_boardsvcstn;
	String t12_alightstn;
	int t12a_paxinveh;
	int t13_waittime;
	int t14_invehtime;
	private double t15_taxifare;
	private String t16_taxireimb;
	private double t17_erpcost;
	private String t18_erpreimb;
	private double t19_parkfee;
	private String t19a_parkftyp;
	private String t20_parkreimb;
	private int stageDuration;
	HITSStage prevStage;
	HITSStage nextStage;
	Coord stageOrigCoord;
	Coord stageDestCoord;

//	String mainmode;
	
	public HITSStage(ResultSet srs, HITSTrip trip) {
		try {
			this.trip = trip;
			this.msno_main = srs.getInt("msno_main");
			this.h1_hhid = getTrimmedStringFromResultSet(srs,"h1_hhid");
			this.pax_id = srs.getInt("pax_id");
			this.trip_id = srs.getInt("trip_id");
			this.stage_id = srs.getInt("stage_id");
			this.t10_mode = getTrimmedStringFromResultSet(srs,"t10_mode");
			this.t10a_walktime = srs.getInt("t10a_walktime");
			this.t10i_modeother = getTrimmedStringFromResultSet(srs,"t10i_modeother");
			this.t11_boardsvcstn = getTrimmedStringFromResultSet(srs,"t11_boardsvcstn");
			this.t12_alightstn = getTrimmedStringFromResultSet(srs,"t12_alightstn");
			this.t12a_paxinveh = srs.getInt("t12a_paxinveh");
			this.t13_waittime = srs.getInt("t13_waittime");
			this.t14_invehtime = srs.getInt("t14_invehtime");
			this.t15_taxifare = srs.getInt("t15_taxifare");
			this.t16_taxireimb = getTrimmedStringFromResultSet(srs,"t16_taxireimb");
			this.t17_erpcost = srs.getInt("t17_erpcost");
			this.t18_erpreimb = getTrimmedStringFromResultSet(srs,"t18_erpreimb");
			this.t19_parkfee = srs.getInt("t19_parkfee");
			this.t19a_parkftyp = getTrimmedStringFromResultSet(srs,"t19a_parkftyp");
			this.t20_parkreimb = getTrimmedStringFromResultSet(srs,"t20_parkreimb");
			this.stageDuration = t10a_walktime + t13_waittime + t14_invehtime;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public HITSStage(){
		
	}
	public String toString(){
	     String format = "|%1$6d|%2$10s|%3$3d|%4$3d|%5$3d|%6$10s|%7$3d|\n";
	     return String.format(format,msno_main, h1_hhid, pax_id, trip_id, stage_id, t10_mode,t14_invehtime );
	}
}
