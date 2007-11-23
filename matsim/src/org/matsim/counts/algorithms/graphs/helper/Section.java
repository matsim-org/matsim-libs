/* *********************************************************************** *
 * project: org.matsim.*
 * Section.java
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

package org.matsim.counts.algorithms.graphs.helper;

import java.util.List;
import java.util.Vector;



public class Section {
	private String title_;
	private List<MyURL> urls_;
	
	public Section(){
		urls_=new Vector<MyURL>();
	}
	public Section(String title) {
		urls_=new Vector<MyURL>();
		this.title_=title;	
	}

	public void setTitle(String title) {
		this.title_=title;
	}
	
	public void addURL(MyURL url){
		urls_.add(url);
	}
	
	public String getTitle() {
		return this.title_;
	}
	
	public List<MyURL> getURLs() {
		return this.urls_;
	}	
}
