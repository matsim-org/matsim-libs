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

import org.matsim.core.config.Module;

/**
 * @author thibautd
 */
public class ReducedModelParametersConfigGroup extends Module {
	private static final long serialVersionUID = 1L;

	public static final String NAME = "ReducedSPModelParameters";

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
	public static final String BETA_TT_CPD = "betaTtCpd";
	public static final String BETA_TT_CPP = "betaTtCpp";
	public static final String BETA_TT_CAR = "betaTtCar";
	public static final String BETA_TT_PT = "betaTtPt";
	public static final String BETA_TRANSFERS_PT = "betaTransfersPt";
	public static final String BETA_WAIT_PT = "betaWaitPt";
	public static final String BETA_CAR_AVAIL = "betaCarAvail";
	public static final String BETA_MALE_CAR = "betaMaleCar";

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
	private double betaTtCpd = -0.0378;
	private double betaTtCpp = -0.0399;
	private double betaTtCar = -0.0348;
	private double betaTtPt = -0.00892;
	private double betaTransfersPt = -0.118;
	private double betaWaitPt = -0.0939;
	private double betaCarAvail = 0.708;
	private double betaMaleCar = 0.355;

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
	}

	@Override
	public String getValue(final String param_name) {
		if (param_name.equals( ASC_CPD )) {
			return ""+ascCpd;
		}
		else if (param_name.equals( ASC_CAR )) {
			return ""+ascCar;
		}
		else if (param_name.equals( ASC_PT )) {
			return ""+ascPt;
		}
		else if (param_name.equals( BETA_ABO_PT )) {
			return ""+betaAboPt;
		}
		else if (param_name.equals( BETA_LOG_AGE_PT )) {
			return ""+betaLogAgePt;
		}
		else if (param_name.equals( BETA_WALK_CAR )) {
			return ""+betaWalkCar;
		}
		else if (param_name.equals( BETA_WALK_CPD )) {
			return ""+betaWalkCpd;
		}
		else if (param_name.equals( BETA_WALK_CPP )) {
			return ""+betaWalkCpp;
		}
		else if (param_name.equals( BETA_WALK_PT )) {
			return ""+betaWalkPt;
		}
		else if (param_name.equals( BETA_FEMALE_CP )) {
			return ""+betaFemaleCp;
		}
		else if (param_name.equals( BETA_GERMAN_CP )) {
			return ""+betaGermanCp;
		}
		else if (param_name.equals( BETA_PARK_CPD )) {
			return ""+betaParkCpd;
		}
		else if (param_name.equals( BETA_PARK_CAR )) {
			return ""+betaParkCar;
		}
		else if (param_name.equals( BETA_COST )) {
			return ""+betaCost;
		}
		else if (param_name.equals( BETA_TT_CPD )) {
			return ""+betaTtCpd;
		}
		else if (param_name.equals( BETA_TT_CPP )) {
			return ""+betaTtCpp;
		}
		else if (param_name.equals( BETA_TT_CAR )) {
			return ""+betaTtCar;
		}
		else if (param_name.equals( BETA_TT_PT )) {
			return ""+betaTtPt;
		}
		else if (param_name.equals( BETA_TRANSFERS_PT )) {
			return ""+betaTransfersPt;
		}
		else if (param_name.equals( BETA_WAIT_PT )) {
			return ""+betaWaitPt;
		}
		else if (param_name.equals( BETA_CAR_AVAIL )) {
			return ""+betaCarAvail;
		}
		else if (param_name.equals( BETA_MALE_CAR )) {
			return ""+betaMaleCar;
		}
		return null;
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

		return map;
	}


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

}

