/* *********************************************************************** *
 * project: org.matsim.*
 * SocNetConfigGroup.java
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


package playground.jhackney;

import java.util.TreeMap;

import org.matsim.core.config.Module;

public class SocNetConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "socialnetwork";
	//public static final String SOCNET = "socialnetwork";
	private static final String SOCNET_GRAPHALGO = "socnetalgorithm";
	private static final String SOCNET_LINKSTRENGTHALGO = "socnetlinkstrengthalgorithm";
	private static final String SOCNET_LINKREMOVALALGO = "socnetlinkremovalalgorithm";
	private static final String SOCNET_LINKREMOVALP = "socnetlinkremovalp";
	private static final String SOCNET_LINKREMOVALAGE = "socnetlinkremovalage";
	private static final String SOCNET_KBAR = "kbar";
	private static final String SOCNET_INTERACTOR_TYPE2 = "spatial_interactor_type";
	private static final String SOCNET_NUM_NONSPATIAL_INTERACTIONS = "num_ns_interactions";
	private static final String SOCNET_TYPE_INFO_EXCHANGED_WEIGHT = "factype_ns";
	private static final String SOCNET_FACILITY_TYPE_WEIGHT = "s_weights";
	private static final String SOCNET_ACT_TYPES = "act_types";
	private static final String SOCNET_EDGE_TYPE = "edge_type";
	private static final String SOCNET_FRACT_NS = "fract_ns_interact";
	private static final String SOCNET_FRACT_INTRO = "fract_introduce_friends";
	private static final String SOCNET_SWITCH_WEIGHTS = "switch_weights";
	private static final String SOCNET_PROB_BEFRIEND = "prob_befriend";
	private static final String SOCNET_OUT_DIR = "outputDirSocialNets";
	private static final String SOCNET_DEG_SAT = "degree_saturation_rate";
	private static final String SOCNET_RP_INT = "replanning_interval";
	private static final String SN_INPUT_DIR = "inputSocNetDir";
	private static final String SN_INIT_ITER = "inputIter";
	private static final String BETA1 ="betafriendfoe";
	private static final String BETA2 =	"betanfriends";
	private static final String BETA3 ="betalognfriends";
	private static final String BETA4 ="betatimewithfriends";
	private static final String SN_ALPHA ="euclid_alpha";
	private static final String SN_RMIN = "euclid_rmin";
	private static final String SN_MEMSIZE = "memSize";
	private static final String SN_REPORT = "reporting_interval";


	private String graphalgo = null;
	private String linkstrengthalgo = null;
	private String linkremovalalgo = null;
	private String linkremovalp = null; // double
	private String linkremovalage= null; // double
	private String kbar = null; // double
	private String interactor1=null;
	private String interactor2=null;
	private String nsinteractions=null; // integer
	private String maxiterations=null; // integer
	private String xchangew=null; // double vector
	private String facweights=null; // double vector
	private String acttypes=null;//String
	private String edgetype=null;
	private String pctnsinteract=null; // double [0,1]
	private String pctintro=null; // double
	private String sweights=null; // own config module (strategy)
	private String pfriend=null; // double
//	private String outdir="socialnets/";
	private String outdir=null;//
	private String degsat=null; // double
	private String interval=null;//int
	private String input_sn_dir=null;// String
	private String init_iter=null;// int
	private String read_mm=null;// boolean
	private String beta1=null;//double
	private String beta2=null;//double
	private String beta3=null;//double
	private String beta4=null;//double
	private String alpha=null;//double
	private String rmin=null;//double
	private String memSize=null;//double
	private String reportInt=null;//int

	public SocNetConfigGroup() {
		super(GROUP_NAME);
	}
	@Override
	public final void addParam(final String key, final String value) {
		if(SOCNET_GRAPHALGO.equals(key)){
			setSocNetGraphAlgo(value);
		}else if(SOCNET_KBAR.equals(key)){
			setSocNetKbar(value);
		}else if (SOCNET_LINKSTRENGTHALGO.equals(key)) {
			setSocNetLinkStrengthAlgo(value);
		}else if (SOCNET_LINKREMOVALALGO.equals(key)) {
			setSocNetLinkRemovalAlgo(value);
		}else if (SOCNET_LINKREMOVALP.equals(key)) {
			setSocNetLinkRemovalP(value);
		}else if (SOCNET_LINKREMOVALAGE.equals(key)) {
			setSocNetLinkRemovalAge(value);
		}else if (SOCNET_INTERACTOR_TYPE2.equals(key)) {
			setSocNetInteractor2(value);
		}else if (SOCNET_NUM_NONSPATIAL_INTERACTIONS.equals(key)) {
			setSocNetNSInteractions(value);
		}else if (SOCNET_TYPE_INFO_EXCHANGED_WEIGHT.equals(key)) {
			setXchange(value);
		}else if (SOCNET_FACILITY_TYPE_WEIGHT.equals(key)) {
			setFacWt(value);
		}else if (SOCNET_ACT_TYPES.equals(key)) {
			setActTypes(value);
		}else if (SOCNET_EDGE_TYPE.equals(key)) {
			setEdgeType(value);
		}else if (SOCNET_FRACT_NS.equals(key)) {
			setFractNSInteract(value);
		}else if (SOCNET_FRACT_INTRO.equals(key)) {
			setFriendIntroProb(value);
		}else if (SOCNET_SWITCH_WEIGHTS.equals(key)) {
			setSWeights(value);
		}else if (SOCNET_PROB_BEFRIEND.equals(key)) {
			setPBefriend(value);
		}else if (SOCNET_OUT_DIR.equals(key)) {
			setOutDir(value);
		}else if (SOCNET_DEG_SAT.equals(key)) {
			setDegSat(value);
		}else if (SOCNET_RP_INT.equals(key)) {
			setRPInt(value);
		}else if (SN_INPUT_DIR.equals(key)) {
			setInDirName(value);
		}else if (SN_INIT_ITER.equals(key)){
			setInitIter(value);
		}else if (BETA1.equals(key)){
			setBeta1(value);
		}else if (BETA2.equals(key)){
			setBeta2(value);
		}else if (BETA3.equals(key)){
			setBeta3(value);
		}else if (BETA4.equals(key)){
			setBeta4(value);
		}else if (SN_ALPHA.equals(key)){
			setAlpha(value);
		}else if (SN_RMIN.equals(key)){
			setRmin(value);
		}else if (SN_MEMSIZE.equals(key)){
			setMemSize(value);
		}else if (SN_REPORT.equals(key)){
			setReportInterval(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}

//	@Override
	@Override
	public final String getValue(final String key) {
		if (SOCNET_GRAPHALGO.equals(key)){
			return getSocNetAlgo();
		} else if (SOCNET_KBAR.equals(key)) {
			return getSocNetKbar();
		} else if (SOCNET_LINKSTRENGTHALGO.equals(key)) {
			return getSocNetLinkStrengthAlgo();
		}else if (SOCNET_LINKREMOVALALGO.equals(key)) {
			return getSocNetLinkRemovalAlgo();
		}else if (SOCNET_LINKREMOVALP.equals(key)) {
			return getSocNetLinkRemovalP();
		}else if (SOCNET_LINKREMOVALAGE.equals(key)) {
			return getSocNetLinkRemovalAge();
		}else if (SOCNET_INTERACTOR_TYPE2.equals(key)) {
			return getSocNetInteractor2();
		}else if (SOCNET_NUM_NONSPATIAL_INTERACTIONS.equals(key)) {
			return getSocNetNSInteractions();
		}else if (SOCNET_TYPE_INFO_EXCHANGED_WEIGHT.equals(key)) {
			return getXchange();
		}else if (SOCNET_FACILITY_TYPE_WEIGHT.equals(key)) {
			return getFacWt();
		}else if (SOCNET_ACT_TYPES.equals(key)) {
			return getActTypes();
		}else if (SOCNET_EDGE_TYPE.equals(key)) {
			return getEdgeType();
		}else if (SOCNET_FRACT_NS.equals(key)) {
			return getFractNSInteract();
		}else if (SOCNET_FRACT_INTRO.equals(key)) {
			return getFriendIntroProb();
		}else if (SOCNET_SWITCH_WEIGHTS.equals(key)) {
			return getSWeights();
		}else if (SOCNET_PROB_BEFRIEND.equals(key)) {
			return getPBefriend();
		}else if (SOCNET_OUT_DIR.equals(key)) {
			return getOutDirName();
		}else if (SOCNET_DEG_SAT.equals(key)) {
			return getDegSat();
		}else if (SOCNET_RP_INT.equals(key)) {
			return getRPInt();
		}else if (SN_INPUT_DIR.equals(key)) {
			return getInDirName();
		}else if (SN_INIT_ITER.equals(key)){
			return getInitIter();
		}else if (BETA1.equals(key)){
			return getBeta1();
		}else if (BETA2.equals(key)){
			return getBeta2();
		}else if (BETA3.equals(key)){
			return getBeta3();
		}else if (BETA4.equals(key)){
			return getBeta4();
		}else if (SN_ALPHA.equals(key)){
			return getAlpha();
		}else if (SN_RMIN.equals(key)){
			return getRmin();
		}else if (SN_MEMSIZE.equals(key)){
			return getMemSize();
		}else if (SN_REPORT.equals(key)){
			return getReportInterval();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addParameterToMap(map, SOCNET_GRAPHALGO);
		addParameterToMap(map, SOCNET_KBAR);
		addParameterToMap(map, SOCNET_LINKSTRENGTHALGO);
		addParameterToMap(map, SOCNET_LINKREMOVALALGO);
		addParameterToMap(map, SOCNET_LINKREMOVALP);
		addParameterToMap(map, SOCNET_LINKREMOVALAGE);
		addParameterToMap(map, SOCNET_INTERACTOR_TYPE2);
		addParameterToMap(map, SOCNET_NUM_NONSPATIAL_INTERACTIONS);
		addParameterToMap(map, SOCNET_TYPE_INFO_EXCHANGED_WEIGHT);
		addParameterToMap(map, SOCNET_FACILITY_TYPE_WEIGHT);
		addParameterToMap(map, SOCNET_ACT_TYPES);
		addParameterToMap(map, SOCNET_EDGE_TYPE);
		addParameterToMap(map, SOCNET_FRACT_NS);
		addParameterToMap(map, SOCNET_FRACT_INTRO);
		addParameterToMap(map, SOCNET_SWITCH_WEIGHTS);
		addParameterToMap(map, SOCNET_PROB_BEFRIEND);
		addParameterToMap(map, SOCNET_OUT_DIR);
		addParameterToMap(map, SOCNET_DEG_SAT);
		addParameterToMap(map, SOCNET_RP_INT);
		addParameterToMap(map, SN_INPUT_DIR);
		addParameterToMap(map, SN_INIT_ITER);
		addParameterToMap(map, BETA1);
		addParameterToMap(map, BETA2);
		addParameterToMap(map, BETA3);
		addParameterToMap(map, BETA4);
		addParameterToMap(map, SN_ALPHA);
		addParameterToMap(map, SN_RMIN);
		addParameterToMap(map, SN_MEMSIZE);
		addParameterToMap(map, SN_REPORT);
		return map;
	}
	/* direct access */

	public String getSocNetAlgo() {
		return this.graphalgo;
	}

	public void setSocNetGraphAlgo(final String graphalgo){
		this.graphalgo = graphalgo;
	}

	public String getSocNetKbar() {
		return this.kbar;
	}

	public void setSocNetKbar(final String kbar){
		this.kbar = kbar;
	}

	public String getSocNetLinkStrengthAlgo() {
		return this.linkstrengthalgo;
	}
	public void setSocNetLinkStrengthAlgo(final String linkstrengthalgo) {
		this.linkstrengthalgo = linkstrengthalgo;
	}
	public String getSocNetLinkRemovalAlgo() {
		return this.linkremovalalgo;
	}
	public void setSocNetLinkRemovalAlgo(final String linkremovalalgo) {
		this.linkremovalalgo = linkremovalalgo;
	}
	public String getSocNetLinkRemovalP() {
		return this.linkremovalp;
	}
	public void setSocNetLinkRemovalP(final String linkremovalp) {
		this.linkremovalp = linkremovalp;
	}
	public String getSocNetLinkRemovalAge() {
		return this.linkremovalage;
	}
	public void setSocNetLinkRemovalAge(final String linkremovalage) {
		this.linkremovalage = linkremovalage;
	}
	public String getSocNetInteractor1() {
		return this.interactor1;
	}
	public void setSocNetInteractor1(final String interactor1) {
		this.interactor1 = interactor1;
	}
	public String getSocNetInteractor2() {
		return this.interactor2;
	}
	public void setSocNetInteractor2(final String interactor2) {
		this.interactor2 = interactor2;
	}
	public String getSocNetNSInteractions() {
		return this.nsinteractions;
	}
	public void setSocNetNSInteractions(final String nsinteractions) {
		this.nsinteractions = nsinteractions;
	}
	public String getNumIterations() {
		return this.maxiterations;
	}
	public void setNumIterations(final String maxiterations) {
		this.maxiterations = maxiterations;
	}
	public String getXchange() {
		return this.xchangew;
	}
	public void setXchange(final String xchangew) {
		this.xchangew = xchangew;
	}
	public String getFacWt() {
		return this.facweights;
	}
	public void setFacWt(final String facweights) {
		this.facweights = facweights;
	}
	public String getActTypes() {
		return this.acttypes;
	}
	public void setActTypes(final String acttypes) {
		this.acttypes = acttypes;
	}
	public String getEdgeType() {
		return this.edgetype;
	}
	public void setEdgeType(final String edgetype) {
		this.edgetype = edgetype;
	}
	public String getFractNSInteract() {
		return this.pctnsinteract;
	}
	public void setFractNSInteract(final String pctnsinteract) {
		this.pctnsinteract = pctnsinteract;
	}
	public String getFriendIntroProb() {
		return this.pctintro;
	}
	public void setFriendIntroProb(final String xx) {
		this.pctintro = xx;
	}
	public String getSWeights() {
		return this.sweights;
	}
	public void setSWeights(final String sweights) {
		this.sweights = sweights;
	}
	public String getPBefriend() {
		return this.pfriend;
	}
	public void setPBefriend(final String pfriend) {
		this.pfriend = pfriend;
	}
	public String getOutDirName() {
		return this.outdir;
	}
	public void setOutDir(final String outdir) {
		this.outdir = outdir;
	}
	public String getDegSat() {
		return this.degsat;
	}
	public void setDegSat(final String degsat) {
		this.degsat = degsat;
	}
	public String getRPInt() {
		return this.interval;
	}
	public void setRPInt(final String interval) {
		this.interval = interval;
	}
	public String getInDirName() {
		return this.input_sn_dir;
	}
	public void setInDirName(final String input_sn_file){
		this.input_sn_dir=input_sn_file;
	}
	public String getInitIter(){
		return this.init_iter;
	}
	public void setInitIter(final String init_iter){
		this.init_iter = init_iter;
	}
	public String getReadMentalMap(){
		return this.read_mm;
	}
	public void setReadMentalMap(final String read_mm){
		this.read_mm = read_mm;
	}
	public String getBeta1(){
		return this.beta1;
	}
	public void setBeta1(final String beta1){
		this.beta1 = beta1;
	}
	public String getBeta2(){
		return this.beta2;
	}
	public void setBeta2(final String beta2){
		this.beta2 = beta2;
	}
	public String getBeta3(){
		return this.beta3;
	}
	public void setBeta3(final String beta3){
		this.beta3 = beta3;
	}
	public String getBeta4(){
		return this.beta4;
	}
	public void setBeta4(final String beta4){
		this.beta4 = beta4;
	}
	public String getAlpha(){
		return this.alpha;
	}
	public void setAlpha(final String alpha){
		this.alpha = alpha;
	}
	public String getRmin(){
		return this.rmin;
	}
	public void setRmin(final String rmin){
		this.rmin = rmin;
	}
	public String getMemSize() {
		return this.memSize;
	}
	public void setMemSize(final String memSize){
		this.memSize=memSize;
	}
	public String getReportInterval() {
		return this.reportInt;
	}
	public void setReportInterval(final String value){
		this.reportInt=value;
	}
}
