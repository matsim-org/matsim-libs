/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.fhuelsmann.emissions;
public class VisumObject {

	private int VISUM_RT_NR;
	private String VISUM_RT_NAME;
	private String HBEFA_RT_NR;
	private String HBEFA_RT_NAME;
	
	public int getVISUM_RT_NR() {
		return VISUM_RT_NR;
	}
	public void setVISUM_RT_NR(int vISUM_RT_NR) {
		VISUM_RT_NR = vISUM_RT_NR;
	}
	public String getVISUM_RT_NAME() {
		return VISUM_RT_NAME;
	}
	public void setVISUM_RT_NAME(String vISUM_RT_NAME) {
		VISUM_RT_NAME = vISUM_RT_NAME;
	}
	public String getHBEFA_RT_NR() {
		return HBEFA_RT_NR;
	}
	public void setHBEFA_RT_NR(String hBEFA_RT_NR) {
		HBEFA_RT_NR = hBEFA_RT_NR;
	}
	public String getHBEFA_RT_NAME() {
		return HBEFA_RT_NAME;
	}
	public void setHBEFA_RT_NAME(String hBEFA_RT_NAME) {
		HBEFA_RT_NAME = hBEFA_RT_NAME;
	}
	public VisumObject(int vISUM_RT_NR, String Hbefa_rt_nr) {
		super();
		VISUM_RT_NR = vISUM_RT_NR;
		this.HBEFA_RT_NR = Hbefa_rt_nr;
	
	}
	
	
	
}
