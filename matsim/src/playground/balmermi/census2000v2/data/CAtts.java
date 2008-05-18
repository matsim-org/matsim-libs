/* *********************************************************************** *
 * project: org.matsim.*
 * Gbl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.balmermi.census2000v2.data;

public abstract class CAtts {

	//////////////////////////////////////////////////////////////////////
	// activity types
	//////////////////////////////////////////////////////////////////////

	public static final String ACT_HOME = "home";

	public static final String ACT_W2 = "work_sector2";
	public static final String ACT_W3 = "work_sector3";

	public static final String ACT_EKIGA = "education_kindergarten";
	public static final String ACT_EPRIM = "education_primary";
	public static final String ACT_ESECO = "education_secondary";
	public static final String ACT_EHIGH = "education_higher";
	public static final String ACT_EOTHR = "education_other";

	public static final String ACT_S1 = "shop_retail_lt100sqm";
	public static final String ACT_S2 = "shop_retail_get100sqm";
	public static final String ACT_S3 = "shop_retail_get400sqm";
	public static final String ACT_S4 = "shop_retail_get1000sqm";
	public static final String ACT_S5 = "shop_retail_gt2500sqm";
	public static final String ACT_SOTHR = "shop_other";
	
	public static final String ACT_LC = "leisure_culture";
	public static final String ACT_LG = "leisure_gastro";
	public static final String ACT_LS = "leisure_sports";

	//////////////////////////////////////////////////////////////////////
	// household identifiers
	//////////////////////////////////////////////////////////////////////

	public static final String HH_Z = "hh_z";
	public static final String HH_W = "hh_w";

	//////////////////////////////////////////////////////////////////////
	// census 2000 global constants
	//////////////////////////////////////////////////////////////////////

	// indizes and names of the census2000 data
	public static final int I_KANT = 0;	public static final String P_KANT = "P_KANT"; 
	public static final int I_ZGDE = 1;	public static final String P_ZGDE = "P_ZGDE"; 
	public static final int I_GEBAEUDE_ID = 2;	public static final String P_GEBAEUDE_ID = "P_GEBAEUDE_ID"; 
	public static final int I_HHNR = 3;	public static final String P_HHNR = "P_HHNR"; 
	public static final int I_WOHNUNG_NR = 4;	public static final String P_WOHNUNG_NR = "P_WOHNUNG_NR"; 
	public static final int I_PERSON_ID = 5;	public static final String P_PERSON_ID = "P_PERSON_ID"; 
	public static final int I_ZKRS = 6;	public static final String P_ZKRS = "P_ZKRS"; 
	public static final int I_GLOC2 = 7;	public static final String P_GLOC2 = "P_GLOC2"; 
	public static final int I_GLOC3 = 8;	public static final String P_GLOC3 = "P_GLOC3"; 
	public static final int I_GLOC4 = 9;	public static final String P_GLOC4 = "P_GLOC4"; 
	public static final int I_WKAT = 10;	public static final String P_WKAT = "P_WKAT"; 
	public static final int I_GEM2 = 11;	public static final String P_GEM2 = "P_GEM2"; 
	public static final int I_PARTNR = 12;	public static final String P_PARTNR = "P_PARTNR"; 
	public static final int I_GETG = 13;	public static final String P_GETG = "P_GETG"; 
	public static final int I_GEMT = 14;	public static final String P_GEMT = "P_GEMT"; 
	public static final int I_GEJA = 15;	public static final String P_GEJA = "P_GEJA"; 
	public static final int I_ALTJ = 16;	public static final String P_ALTJ = "P_ALTJ"; 
	public static final int I_AKL5 = 17;	public static final String P_AKL5 = "P_AKL5"; 
	public static final int I_AGRP = 18;	public static final String P_AGRP = "P_AGRP"; 
	public static final int I_VALTJ = 19;	public static final String P_VALTJ = "P_VALTJ"; 
	public static final int I_GORT = 20;	public static final String P_GORT = "P_GORT"; 
	public static final int I_GORTCH = 21;	public static final String P_GORTCH = "P_GORTCH"; 
	public static final int I_GORTAUS = 22;	public static final String P_GORTAUS = "P_GORTAUS"; 
	public static final int I_GESL = 23;	public static final String P_GESL = "P_GESL"; 
	public static final int I_ZIVL = 24;	public static final String P_ZIVL = "P_ZIVL"; 
	public static final int I_ZIVJ = 25;	public static final String P_ZIVJ = "P_ZIVJ"; 
	public static final int I_HMAT = 26;	public static final String P_HMAT = "P_HMAT"; 
	public static final int I_CHJA = 27;	public static final String P_CHJA = "P_CHJA"; 
	public static final int I_ZNAT = 28;	public static final String P_ZNAT = "P_ZNAT"; 
	public static final int I_NATI = 29;	public static final String P_NATI = "P_NATI"; 
	public static final int I_NATUNO = 30;	public static final String P_NATUNO = "P_NATUNO"; 
	public static final int I_AUSW = 31;	public static final String P_AUSW = "P_AUSW"; 
	public static final int I_WO5M = 32;	public static final String P_WO5M = "P_WO5M"; 
	public static final int I_WO5CH = 33;	public static final String P_WO5CH = "P_WO5CH"; 
	public static final int I_WO5AUS = 34;	public static final String P_WO5AUS = "P_WO5AUS"; 
	public static final int I_ELTERN = 35;	public static final String P_ELTERN = "P_ELTERN"; 
	public static final int I_ZKIND = 36;	public static final String P_ZKIND = "P_ZKIND"; 
	public static final int I_GJKIND_1 = 37;	public static final String P_GJKIND_1 = "P_GJKIND_1"; 
	public static final int I_GJKIND_2 = 38;	public static final String P_GJKIND_2 = "P_GJKIND_2"; 
	public static final int I_GJKIND_3 = 39;	public static final String P_GJKIND_3 = "P_GJKIND_3"; 
	public static final int I_GJKIND_4 = 40;	public static final String P_GJKIND_4 = "P_GJKIND_4"; 
	public static final int I_GJKIND_L = 41;	public static final String P_GJKIND_L = "P_GJKIND_L"; 
	public static final int I_GJKIND_J = 42;	public static final String P_GJKIND_J = "P_GJKIND_J"; 
	public static final int I_STHHZ = 43;	public static final String P_STHHZ = "P_STHHZ"; 
	public static final int I_STHHW = 44;	public static final String P_STHHW = "P_STHHW"; 
	public static final int I_RPHHZ = 45;	public static final String P_RPHHZ = "P_RPHHZ"; 
	public static final int I_RPHHW = 46;	public static final String P_RPHHW = "P_RPHHW"; 
	public static final int I_EPNRZ = 47;	public static final String P_EPNRZ = "P_EPNRZ"; 
	public static final int I_EPNRW = 48;	public static final String P_EPNRW = "P_EPNRW"; 
	public static final int I_HHTPZ = 49;	public static final String P_HHTPZ = "P_HHTPZ"; 
	public static final int I_HHTPW = 50;	public static final String P_HHTPW = "P_HHTPW"; 
	public static final int I_APERZ = 51;	public static final String P_APERZ = "P_APERZ"; 
	public static final int I_APERW = 52;	public static final String P_APERW = "P_APERW"; 
	public static final int I_WKATA = 53;	public static final String P_WKATA = "P_WKATA"; 
	public static final int I_SPRA = 54;	public static final String P_SPRA = "P_SPRA"; 
	public static final int I_MSPR = 55;	public static final String P_MSPR = "P_MSPR"; 
	public static final int I_HSPR = 56;	public static final String P_HSPR = "P_HSPR"; 
	public static final int I_SHSD = 57;	public static final String P_SHSD = "P_SHSD"; 
	public static final int I_SHHD = 58;	public static final String P_SHHD = "P_SHHD"; 
	public static final int I_SHPR = 59;	public static final String P_SHPR = "P_SHPR"; 
	public static final int I_SHFR = 60;	public static final String P_SHFR = "P_SHFR"; 
	public static final int I_SHTB = 61;	public static final String P_SHTB = "P_SHTB"; 
	public static final int I_SHIT = 62;	public static final String P_SHIT = "P_SHIT"; 
	public static final int I_SHRR = 63;	public static final String P_SHRR = "P_SHRR"; 
	public static final int I_SHEN = 64;	public static final String P_SHEN = "P_SHEN"; 
	public static final int I_SHAN = 65;	public static final String P_SHAN = "P_SHAN"; 
	public static final int I_BSPR = 66;	public static final String P_BSPR = "P_BSPR"; 
	public static final int I_SBSD = 67;	public static final String P_SBSD = "P_SBSD"; 
	public static final int I_SBHD = 68;	public static final String P_SBHD = "P_SBHD"; 
	public static final int I_SBPR = 69;	public static final String P_SBPR = "P_SBPR"; 
	public static final int I_SBFR = 70;	public static final String P_SBFR = "P_SBFR"; 
	public static final int I_SBTB = 71;	public static final String P_SBTB = "P_SBTB"; 
	public static final int I_SBIT = 72;	public static final String P_SBIT = "P_SBIT"; 
	public static final int I_SBRR = 73;	public static final String P_SBRR = "P_SBRR"; 
	public static final int I_SBEN = 74;	public static final String P_SBEN = "P_SBEN"; 
	public static final int I_SBAN = 75;	public static final String P_SBAN = "P_SBAN"; 
	public static final int I_GEGW = 76;	public static final String P_GEGW = "P_GEGW"; 
	public static final int I_HABG = 77;	public static final String P_HABG = "P_HABG"; 
	public static final int I_UHAB = 78;	public static final String P_UHAB = "P_UHAB"; 
	public static final int I_FHAB = 79;	public static final String P_FHAB = "P_FHAB"; 
	public static final int I_HFAB = 80;	public static final String P_HFAB = "P_HFAB"; 
	public static final int I_HBAB = 81;	public static final String P_HBAB = "P_HBAB"; 
	public static final int I_LSAB = 82;	public static final String P_LSAB = "P_LSAB"; 
	public static final int I_MPAB = 83;	public static final String P_MPAB = "P_MPAB"; 
	public static final int I_BLAB = 84;	public static final String P_BLAB = "P_BLAB"; 
	public static final int I_BSAB = 85;	public static final String P_BSAB = "P_BSAB"; 
	public static final int I_OSAB = 86;	public static final String P_OSAB = "P_OSAB"; 
	public static final int I_KAUS = 87;	public static final String P_KAUS = "P_KAUS"; 
	public static final int I_AMS = 88;	public static final String P_AMS = "P_AMS"; 
	public static final int I_BGRAD = 89;	public static final String P_BGRAD = "P_BGRAD"; 
	public static final int I_KAZEIT = 90;	public static final String P_KAZEIT = "P_KAZEIT"; 
	public static final int I_ERWS = 91;	public static final String P_ERWS = "P_ERWS"; 
	public static final int I_MAMS = 92;	public static final String P_MAMS = "P_MAMS"; 
	public static final int I_KAMS = 93;	public static final String P_KAMS = "P_KAMS"; 
	public static final int I_VOLL = 94;	public static final String P_VOLL = "P_VOLL"; 
	public static final int I_TZT1 = 95;	public static final String P_TZT1 = "P_TZT1"; 
	public static final int I_TZT2 = 96;	public static final String P_TZT2 = "P_TZT2"; 
	public static final int I_ETOA = 97;	public static final String P_ETOA = "P_ETOA"; 
	public static final int I_ARLO = 98;	public static final String P_ARLO = "P_ARLO"; 
	public static final int I_STSU = 99;	public static final String P_STSU = "P_STSU"; 
	public static final int I_KSTZ = 100;	public static final String P_KSTZ = "P_KSTZ"; 
	public static final int I_NENSS = 101;	public static final String P_NENSS = "P_NENSS"; 
	public static final int I_IAUS = 102;	public static final String P_IAUS = "P_IAUS"; 
	public static final int I_RENT = 103;	public static final String P_RENT = "P_RENT"; 
	public static final int I_HAFA = 104;	public static final String P_HAFA = "P_HAFA"; 
	public static final int I_FRTA = 105;	public static final String P_FRTA = "P_FRTA"; 
	public static final int I_HVOLL = 106;	public static final String P_HVOLL = "P_HVOLL"; 
	public static final int I_HTZS = 107;	public static final String P_HTZS = "P_HTZS"; 
	public static final int I_HAZEIT = 108;	public static final String P_HAZEIT = "P_HAZEIT"; 
	public static final int I_HIAUS = 109;	public static final String P_HIAUS = "P_HIAUS"; 
	public static final int I_HHAFA = 110;	public static final String P_HHAFA = "P_HHAFA"; 
	public static final int I_HFRTA = 111;	public static final String P_HFRTA = "P_HFRTA"; 
	public static final int I_ERBE = 112;	public static final String P_ERBE = "P_ERBE";
	public static final int I_PBER = 113;	public static final String P_PBER = "P_PBER"; 
	public static final int I_ISCO = 114;	public static final String P_ISCO = "P_ISCO"; 
	public static final int I_SOPK = 115;	public static final String P_SOPK = "P_SOPK"; 
	public static final int I_BETGR = 116;	public static final String P_BETGR = "P_BETGR"; 
	public static final int I_UNTGR = 117;	public static final String P_UNTGR = "P_UNTGR"; 
	public static final int I_ANOGA = 118;	public static final String P_ANOGA = "P_ANOGA"; 
	public static final int I_AREFO = 119;	public static final String P_AREFO = "P_AREFO"; 
	public static final int I_AGDE = 120;	public static final String P_AGDE = "P_AGDE"; 
	public static final int I_AZKRS = 121;	public static final String P_AZKRS = "P_AZKRS"; 
	public static final int I_AGLOC2 = 122;	public static final String P_AGLOC2 = "P_AGLOC2"; 
	public static final int I_AGLOC3 = 123;	public static final String P_AGLOC3 = "P_AGLOC3"; 
	public static final int I_AGLOC4 = 124;	public static final String P_AGLOC4 = "P_AGLOC4"; 
	public static final int I_AORT = 125;	public static final String P_AORT = "P_AORT"; 
	public static final int I_ADIST = 126;	public static final String P_ADIST = "P_ADIST"; 
	public static final int I_APEND = 127;	public static final String P_APEND = "P_APEND"; 
	public static final int I_AWMIN = 128;	public static final String P_AWMIN = "P_AWMIN"; 
	public static final int I_AWOFT = 129;	public static final String P_AWOFT = "P_AWOFT"; 
	public static final int I_AWTAGE = 130;	public static final String P_AWTAGE = "P_AWTAGE"; 
	public static final int I_AVEMI = 131;	public static final String P_AVEMI = "P_AVEMI"; 
	public static final int I_AWEGM = 132;	public static final String P_AWEGM = "P_AWEGM"; 
	public static final int I_AVMKE = 133;	public static final String P_AVMKE = "P_AVMKE"; 
	public static final int I_AVELO = 134;	public static final String P_AVELO = "P_AVELO"; 
	public static final int I_AMOFA = 135;	public static final String P_AMOFA = "P_AMOFA"; 
	public static final int I_AMRAD = 136;	public static final String P_AMRAD = "P_AMRAD"; 
	public static final int I_APKWL = 137;	public static final String P_APKWL = "P_APKWL"; 
	public static final int I_APKWM = 138;	public static final String P_APKWM = "P_APKWM"; 
	public static final int I_AWBUS = 139;	public static final String P_AWBUS = "P_AWBUS"; 
	public static final int I_ABAHN = 140;	public static final String P_ABAHN = "P_ABAHN"; 
	public static final int I_ATRAM = 141;	public static final String P_ATRAM = "P_ATRAM"; 
	public static final int I_APOST = 142;	public static final String P_APOST = "P_APOST"; 
	public static final int I_AVAND = 143;	public static final String P_AVAND = "P_AVAND"; 
	public static final int I_SGDE = 144;	public static final String P_SGDE = "P_SGDE"; 
	public static final int I_SZKRS = 145;	public static final String P_SZKRS = "P_SZKRS"; 
	public static final int I_SGLOC2 = 146;	public static final String P_SGLOC2 = "P_SGLOC2"; 
	public static final int I_SGLOC3 = 147;	public static final String P_SGLOC3 = "P_SGLOC3"; 
	public static final int I_SGLOC4 = 148;	public static final String P_SGLOC4 = "P_SGLOC4"; 
	public static final int I_SORT = 149;	public static final String P_SORT = "P_SORT"; 
	public static final int I_SDIST = 150;	public static final String P_SDIST = "P_SDIST"; 
	public static final int I_SPEND = 151;	public static final String P_SPEND = "P_SPEND"; 
	public static final int I_SWMIN = 152;	public static final String P_SWMIN = "P_SWMIN"; 
	public static final int I_SWOFT = 153;	public static final String P_SWOFT = "P_SWOFT"; 
	public static final int I_SWTAGE = 154;	public static final String P_SWTAGE = "P_SWTAGE"; 
	public static final int I_SVEMI = 155;	public static final String P_SVEMI = "P_SVEMI"; 
	public static final int I_SWEGM = 156;	public static final String P_SWEGM = "P_SWEGM"; 
	public static final int I_SVMKE = 157;	public static final String P_SVMKE = "P_SVMKE"; 
	public static final int I_SVELO = 158;	public static final String P_SVELO = "P_SVELO"; 
	public static final int I_SMOFA = 159;	public static final String P_SMOFA = "P_SMOFA"; 
	public static final int I_SMRAD = 160;	public static final String P_SMRAD = "P_SMRAD"; 
	public static final int I_SPKWL = 161;	public static final String P_SPKWL = "P_SPKWL"; 
	public static final int I_SPKWM = 162;	public static final String P_SPKWM = "P_SPKWM"; 
	public static final int I_SSBUS = 163;	public static final String P_SSBUS = "P_SSBUS"; 
	public static final int I_SBAHN = 164;	public static final String P_SBAHN = "P_SBAHN"; 
	public static final int I_STRAM = 165;	public static final String P_STRAM = "P_STRAM"; 
	public static final int I_SPOST = 166;	public static final String P_SPOST = "P_SPOST"; 
	public static final int I_SVAND = 167;	public static final String P_SVAND = "P_SVAND"; 
	public static final int I_SNOGA = 168;	public static final String P_SNOGA = "P_SNOGA"; 
	public static final int I_SREFO = 169;	public static final String P_SREFO = "P_SREFO"; 
	public static final int I_XACH = 170;	public static final String P_XACH = "P_XACH"; 
	public static final int I_YACH = 171;	public static final String P_YACH = "P_YACH"; 
}
