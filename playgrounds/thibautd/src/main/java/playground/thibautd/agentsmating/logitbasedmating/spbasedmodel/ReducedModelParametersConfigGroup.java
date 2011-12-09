/* *********************************************************************** *
 * project: org.matsim.*
 * ReducedModelParametersConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.core.config.Module;

/**
 * @author thibautd
 */
public class ReducedModelParametersConfigGroup extends Module {
	private static final long serialVersionUID = 1L;

	public static final String NAME = "ReducedSPModelParameters";

	// not-really-model-parameters parameters: names
	// -------------------------------------------------------------------------
	public static final String LANG_FILE = "id2languageFile";

	// not-really-model-parameters parameters: values
	// -------------------------------------------------------------------------
	private String langFile = null;

	// parameters of the model: names
	// -------------------------------------------------------------------------
	public static final String ASC_CPD = "ascCpd";
	public static final String ASC_CAR = "ascCar";
	public static final String ASC_PT = "ascPt";
	public static final String BETA_ABO_PT = "betaAboPt";
	public static final String BETA_LOG_AGE_PT = "betaLogAgePt";
	public static final String BETA_WALK_CAR = "betaWalkCar";
	public static final String BETA_WALK_CPD = "betaWalkCpd";
	public static final String BETA_WALK_CPP = "betaWalkCpp";
	public static final String BETA_WALK_PT = "betaWalkPt";
	public static final String BETA_FEMALE_CP = "betaFemaleCp";
	public static final String BETA_GERMAN_CP = "betaGermanCp";
	public static final String BETA_PARK_CPD = "betaParkCpd";
	public static final String BETA_PARK_CAR = "betaParkCar";
	public static final String BETA_COST = "betaCost";
	/**
	 * The parameter for times <b>in seconds</b>.
	 */
	public static final String BETA_TT_CPD = "betaTtCpd";
	/**
	 * The parameter for times <b>in seconds</b>.
	 */
	public static final String BETA_TT_CPP = "betaTtCpp";
	/**
	 * The parameter for times <b>in seconds</b>.
	 */
	public static final String BETA_TT_CAR = "betaTtCar";
	/**
	 * The parameter for times <b>in seconds</b>.
	 */
	public static final String BETA_TT_PT = "betaTtPt";
	public static final String BETA_TRANSFERS_PT = "betaTransfersPt";
	public static final String BETA_WAIT_PT = "betaWaitPt";
	public static final String BETA_CAR_AVAIL = "betaCarAvail";
	public static final String BETA_MALE_CAR = "betaMaleCar";

	// parameters of the model: values
	// -------------------------------------------------------------------------
	private double ascCpd = 0.201;
	private double ascCar = -0.890;
	private double ascPt = -6.25;
	private double betaAboPt = 1.94;
	private double betaLogAgePt = 1.00;
	private double betaWalkCar = -0.0480;
	private double betaWalkCpd = -0.0285;
	private double betaWalkCpp = -0.0766;
	private double betaWalkPt = -0.0227;
	private double betaFemaleCp = -0.272;
	private double betaGermanCp = 0.215;
	private double betaParkCpd = -0.165;
	private double betaParkCar = -0.0314;
	private double betaCost = -0.0541;
	// the parameters where estimated with time in minutes
	private double betaTtCpd = -0.0378 / 60d;
	private double betaTtCpp = -0.0399 / 60d;
	private double betaTtCar = -0.0348 / 60d;
	private double betaTtPt = -0.00892 / 60d;
	private double betaTransfersPt = -0.118;
	private double betaWaitPt = -0.0939;
	private double betaCarAvail = 0.708;
	private double betaMaleCar = 0.355;

	// cost estimation parameters: names
	// -------------------------------------------------------------------------
	private static final String CAR_COST_PER_M = "carCostPerM"; // CHF/m
	// consider a driver usually drives 10 minutes more when he picks up a passenger 
	private static final String SURPLUS_DRIVER = "timeSurplusCpDriver";
	// cost of pt, when GA, Halbtax or nothing
	private static final String GA_COST_PER_M = "gaCostPerM"; // 0.08 CHF/km
	private static final String HT_COST_PER_M = "htCostPerM"; // 0.15 CHF/km
	private static final String PT_COST_PER_M = "ptCostPerM"; // 0.28 CHF/km

	// cost estimation parameters: values 
	// -------------------------------------------------------------------------
	private double carCostPerM = (1d / 10000d) * 1.50; // 1.50 CHF/L and 10L/100km
	// consider a driver usually drives 10 minutes more when he picks up a passenger 
	private double surplusDriver = 10 * 60;
	// cost of pt, when GA, Halbtax or nothing
	private double gaCostPerM = 0.08 / 1000d; // 0.08 CHF/km
	private double htCostPerM = 0.15 / 1000d; // 0.15 CHF/km
	private double ptCostPerM = 0.28 / 1000d; // 0.28 CHF/km

	public ReducedModelParametersConfigGroup() {
		super( NAME );
	}

	@Override
	public void addParam(
			final String param_name,
			final String value) {
		if (param_name.equals( ASC_CPD )) {
			ascCpd = Double.parseDouble( value );
		}
		else if (param_name.equals( ASC_CAR )) {
			ascCar = Double.parseDouble( value );
		}
		else if (param_name.equals( ASC_PT )) {
			ascPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_ABO_PT )) {
			betaAboPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_LOG_AGE_PT )) {
			betaLogAgePt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_WALK_CAR )) {
			betaWalkCar = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_WALK_CPD )) {
			betaWalkCpd = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_WALK_CPP )) {
			betaWalkCpp = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_WALK_PT )) {
			betaWalkPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_FEMALE_CP )) {
			betaFemaleCp = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_GERMAN_CP )) {
			betaGermanCp = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_PARK_CPD )) {
			betaParkCpd = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_PARK_CAR )) {
			betaParkCar = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_COST )) {
			betaCost = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_TT_CPD )) {
			betaTtCpd = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_TT_CPP )) {
			betaTtCpp = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_TT_CAR )) {
			betaTtCar = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_TT_PT )) {
			betaTtPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_TRANSFERS_PT )) {
			betaTransfersPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_WAIT_PT )) {
			betaWaitPt = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_CAR_AVAIL )) {
			betaCarAvail = Double.parseDouble( value );
		}
		else if (param_name.equals( BETA_MALE_CAR )) {
			betaMaleCar = Double.parseDouble( value );
		}
		else if (param_name.equals( CAR_COST_PER_M )) {
			setCarCostPerM( value );
		}
		else if (param_name.equals( SURPLUS_DRIVER )) {
			setSurplusDriver( value );
		}
		else if (param_name.equals( GA_COST_PER_M )) {
			setGaCostPerM( value );
		}
		else if (param_name.equals( HT_COST_PER_M )) {
			setHtCostPerM( value );
		}
		else if (param_name.equals( PT_COST_PER_M )) {
			setPtCostPerM( value );
		}
		else if (param_name.equals( LANG_FILE )) {
			setLanguageFile( value );
		}
	}

	@Override
	public String getValue(final String param_name) {
		return getParams().get( param_name );
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();

		map.put( ASC_CPD , ""+ascCpd );
		map.put( ASC_CAR , ""+ascCar );
		map.put( ASC_PT , ""+ascPt );
		map.put( BETA_ABO_PT , ""+betaAboPt );
		map.put( BETA_LOG_AGE_PT , ""+betaLogAgePt );
		map.put( BETA_WALK_CAR , ""+betaWalkCar );
		map.put( BETA_WALK_CPD , ""+betaWalkCpd );
		map.put( BETA_WALK_CPP , ""+betaWalkCpp );
		map.put( BETA_WALK_PT , ""+betaWalkPt );
		map.put( BETA_FEMALE_CP , ""+betaFemaleCp );
		map.put( BETA_GERMAN_CP , ""+betaGermanCp );
		map.put( BETA_PARK_CPD , ""+betaParkCpd );
		map.put( BETA_PARK_CAR , ""+betaParkCar );
		map.put( BETA_COST , ""+betaCost );
		map.put( BETA_TT_CPD , ""+betaTtCpd );
		map.put( BETA_TT_CPP , ""+betaTtCpp );
		map.put( BETA_TT_CAR , ""+betaTtCar );
		map.put( BETA_TT_PT , ""+betaTtPt );
		map.put( BETA_TRANSFERS_PT , ""+betaTransfersPt );
		map.put( BETA_WAIT_PT , ""+betaWaitPt );
		map.put( BETA_CAR_AVAIL , ""+betaCarAvail );
		map.put( BETA_MALE_CAR , ""+betaMaleCar );
		map.put( CAR_COST_PER_M, ""+carCostPerM );
		map.put( SURPLUS_DRIVER, ""+surplusDriver );
		map.put( GA_COST_PER_M, ""+gaCostPerM );
		map.put( HT_COST_PER_M, ""+htCostPerM );
		map.put( PT_COST_PER_M, ""+ptCostPerM );
		map.put( LANG_FILE, ""+langFile );

		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// model parameters
	// /////////////////////////////////////////////////////////////////////////
	public double ascCpd() {
		return ascCpd;
	}
	public double ascCar() {
		return ascCar;
	}
	public double ascPt() {
		return ascPt;
	}
	public double betaAboPt() {
		return betaAboPt;
	}
	public double betaLogAgePt() {
		return betaLogAgePt;
	}
	public double betaWalkCar() {
		return betaWalkCar;
	}
	public double betaWalkCpd() {
		return betaWalkCpd;
	}
	public double betaWalkCpp() {
		return betaWalkCpp;
	}
	public double betaWalkPt() {
		return betaWalkPt;
	}
	public double betaFemaleCp() {
		return betaFemaleCp;
	}
	public double betaGermanCp() {
		return betaGermanCp;
	}
	public double betaParkCpd() {
		return betaParkCpd;
	}
	public double betaParkCar() {
		return betaParkCar;
	}
	public double betaCost() {
		return betaCost;
	}
	public double betaTtCpd() {
		return betaTtCpd;
	}
	public double betaTtCpp() {
		return betaTtCpp;
	}
	public double betaTtCar() {
		return betaTtCar;
	}
	public double betaTtPt() {
		return betaTtPt;
	}
	public double betaTransfersPt() {
		return betaTransfersPt;
	}
	public double betaWaitPt() {
		return betaWaitPt;
	}
	public double betaCarAvail() {
		return betaCarAvail;
	}
	public double betaMaleCar() {
		return betaMaleCar;
	}

	// /////////////////////////////////////////////////////////////////////////
	// cost parameters
	// /////////////////////////////////////////////////////////////////////////
	public double getCarCostPerM() {
		return carCostPerM;
	}

	public void setCarCostPerM( final String value) {
		this.carCostPerM = Double.parseDouble( value );
	}

	public double getSurplusDriver() {
		return surplusDriver;
	}

	public void setSurplusDriver( final String value) {
		this.surplusDriver = Double.parseDouble( value );
	}

	public double getGaCostPerM() {
		return gaCostPerM;
	}

	public void setGaCostPerM( final String value ) {
		this.gaCostPerM = Double.parseDouble( value );
	}

	public double getHtCostPerM() {
		return htCostPerM;
	}

	public void setHtCostPerM( final String value ) {
		this.htCostPerM = Double.parseDouble( value );
	}

	public double getPtCostPerM() {
		return ptCostPerM;
	}

	public void setPtCostPerM( final String value ) {
		this.ptCostPerM = Double.parseDouble( value );
	}

	// /////////////////////////////////////////////////////////////////////////
	// language map
	// /////////////////////////////////////////////////////////////////////////
	private void setLanguageFile(final String file) {
		langFile = file;
	}

	public String getLanguageFile() {
		return langFile;
	}
}

