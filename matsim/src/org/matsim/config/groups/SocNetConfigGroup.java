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


package org.matsim.config.groups;

import java.util.TreeMap;

import org.matsim.config.Module;

public class SocNetConfigGroup extends Module {
	public static final String GROUP_NAME = "socialnetwork";
	//public static final String SOCNET = "socialnetwork";
	private static final String SOCNET_GRAPHALGO = "socnetalgorithm";
	private static final String SOCNET_LINKSTRENGTHALGO = "socnetlinkstrengthalgorithm";
	private static final String SOCNET_LINKREMOVALALGO = "socnetlinkremovalalgorithm";
	private static final String SOCNET_LINKREMOVALP = "socnetlinkremovalp";
	private static final String SOCNET_LINKREMOVALAGE = "socnetlinkremovalage";
	private static final String SOCNET_KBAR = "kbar";
	private static final String SOCNET_INTERACTOR_TYPE1 = "nonspatial_interactor_type";
	private static final String SOCNET_INTERACTOR_TYPE2 = "spatial_interactor_type";
	private static final String SOCNET_NUM_NONSPATIAL_INTERACTIONS = "num_ns_interactions";
	private static final String SOCNET_NUM_SN_ITERATIONS = "max_sn_iter";
	private static final String SOCNET_TYPE_INFO_EXCHANGED_WEIGHT = "factype_ns";
	private static final String SOCNET_FACILITY_TYPE_WEIGHT = "s_weights";
	private static final String SOCNET_EDGE_TYPE = "edge_type";
	private static final String SOCNET_FRACT_NS = "fract_ns_interact";
	private static final String SOCNET_FRACT_INTRO = "fract_introduce_friends";
	private static final String SOCNET_FRACT_S = "fract_s_interact";
	private static final String SOCNET_SWITCH_WEIGHTS = "switch_weights";
	private static final String SOCNET_PROB_BEFRIEND = "prob_befriend";
	private static final String SOCNET_OUT_DIR = "outputDirSocialNets";
	private static final String SOCNET_DEG_SAT = "degree_saturation_rate";
	private static final String SOCNET_RP_INT = "replanning_interval";
	private static final String SOCNET_GRID = "grid_spacing";
	private static final String INPUT_SN_DIR = "inputSocNetDir";
	private static final String INIT_ITER = "inputIter";
	private static final String READ_MENTALMAP = "readMentalMap";

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
	private String edgetype=null;
	private String pctnsinteract=null; // double [0,1]
	private String triangles=null; // double
	private String pctsinteract=null; // double [0,1]
	private String sweights=null; // own config module (strategy)
	private String pfriend=null; // double
	private String outdir="socialnets/";
	private String degsat=null; // double
	private String interval=null;//int
	private String grid_spacing=null;// int
	private String input_sn_dir=null;// String
	private String init_iter=null;// int
	private String read_mm=null;// boolean


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
		}else if (SOCNET_INTERACTOR_TYPE1.equals(key)) {
			setSocNetInteractor1(value);
		}else if (SOCNET_INTERACTOR_TYPE2.equals(key)) {
			setSocNetInteractor2(value);
		}else if (SOCNET_NUM_NONSPATIAL_INTERACTIONS.equals(key)) {
			setSocNetNSInteractions(value);
		}else if (SOCNET_NUM_SN_ITERATIONS.equals(key)) {
			setNumIterations(value);
		}else if (SOCNET_TYPE_INFO_EXCHANGED_WEIGHT.equals(key)) {
			setXchange(value);
		}else if (SOCNET_FACILITY_TYPE_WEIGHT.equals(key)) {
			setFacWt(value);
		}else if (SOCNET_EDGE_TYPE.equals(key)) {
			setEdgeType(value);
		}else if (SOCNET_FRACT_NS.equals(key)) {
			setFractNSInteract(value);
		}else if (SOCNET_FRACT_INTRO.equals(key)) {
			setTriangles(value);
		}else if (SOCNET_FRACT_S.equals(key)) {
			setFractSInteract(value);
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
		}else if (SOCNET_GRID.equals(key)) {
			setGridSpace(value);
		}else if (INPUT_SN_DIR.equals(key)) {
			setInDirName(value);
		}else if (INIT_ITER.equals(key)){
			setInitIter(value);
		}else if (READ_MENTALMAP.equals(key)){
			setReadMentalMap(value);
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
		}else if (SOCNET_INTERACTOR_TYPE1.equals(key)) {
			return getSocNetInteractor1();
		}else if (SOCNET_INTERACTOR_TYPE2.equals(key)) {
			return getSocNetInteractor2();
		}else if (SOCNET_NUM_NONSPATIAL_INTERACTIONS.equals(key)) {
			return getSocNetNSInteractions();
		}else if (SOCNET_NUM_SN_ITERATIONS.equals(key)) {
			return getNumIterations();
		}else if (SOCNET_TYPE_INFO_EXCHANGED_WEIGHT.equals(key)) {
			return getXchange();
		}else if (SOCNET_FACILITY_TYPE_WEIGHT.equals(key)) {
			return getFacWt();
		}else if (SOCNET_EDGE_TYPE.equals(key)) {
			return getEdgeType();
		}else if (SOCNET_FRACT_NS.equals(key)) {
			return getFractNSInteract();
		}else if (SOCNET_FRACT_INTRO.equals(key)) {
			return getTriangles();
		}else if (SOCNET_FRACT_S.equals(key)) {
			return getFractSInteract();
		}else if (SOCNET_SWITCH_WEIGHTS.equals(key)) {
			return getSWeights();
		}else if (SOCNET_PROB_BEFRIEND.equals(key)) {
			return getPBefriend();
		}else if (SOCNET_OUT_DIR.equals(key)) {
			return getOutDir();
		}else if (SOCNET_DEG_SAT.equals(key)) {
			return getDegSat();
		}else if (SOCNET_RP_INT.equals(key)) {
			return getRPInt();
		}else if (SOCNET_GRID.equals(key)) {
			return getGridSpace();
		}else if (INPUT_SN_DIR.equals(key)) {
			return getInDirName();
		}else if (INIT_ITER.equals(key)){
			return getInitIter();
		}else if (READ_MENTALMAP.equals(key)){
			return getReadMentalMap();
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		addNotNullParameterToMap(map, SOCNET_GRAPHALGO);
		addNotNullParameterToMap(map, SOCNET_KBAR);
		addNotNullParameterToMap(map, SOCNET_LINKSTRENGTHALGO);
		addNotNullParameterToMap(map, SOCNET_LINKREMOVALALGO);
		addNotNullParameterToMap(map, SOCNET_LINKREMOVALP);
		addNotNullParameterToMap(map, SOCNET_LINKREMOVALAGE);
		addNotNullParameterToMap(map, SOCNET_INTERACTOR_TYPE1);
		addNotNullParameterToMap(map, SOCNET_INTERACTOR_TYPE2);
		addNotNullParameterToMap(map, SOCNET_NUM_NONSPATIAL_INTERACTIONS);
		addNotNullParameterToMap(map, SOCNET_NUM_SN_ITERATIONS);
		addNotNullParameterToMap(map, SOCNET_TYPE_INFO_EXCHANGED_WEIGHT);
		addNotNullParameterToMap(map, SOCNET_FACILITY_TYPE_WEIGHT);
		addNotNullParameterToMap(map, SOCNET_EDGE_TYPE);
		addNotNullParameterToMap(map, SOCNET_FRACT_NS);
		addNotNullParameterToMap(map, SOCNET_FRACT_INTRO);
		addNotNullParameterToMap(map, SOCNET_FRACT_S);
		addNotNullParameterToMap(map, SOCNET_SWITCH_WEIGHTS);
		addNotNullParameterToMap(map, SOCNET_PROB_BEFRIEND);
		addNotNullParameterToMap(map, SOCNET_OUT_DIR);
		addNotNullParameterToMap(map, SOCNET_DEG_SAT);
		addNotNullParameterToMap(map, SOCNET_RP_INT);
		addNotNullParameterToMap(map, SOCNET_GRID);
		addNotNullParameterToMap(map, INPUT_SN_DIR);
		addNotNullParameterToMap(map, INIT_ITER);
		addNotNullParameterToMap(map, READ_MENTALMAP);
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
	public String getTriangles() {
		return this.triangles;
	}
	public void setTriangles(final String triangles) {
		this.triangles = triangles;
	}
	public String getFractSInteract() {
		return this.pctsinteract;
	}
	public void setFractSInteract(final String pctsinteract) {
		this.pctsinteract = pctsinteract;
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
	public String getOutDir() {
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
	public String getGridSpace() {
		return this.grid_spacing;
	}
	public void setGridSpace(final String grid_spacing) {
		this.grid_spacing = grid_spacing;
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
}
