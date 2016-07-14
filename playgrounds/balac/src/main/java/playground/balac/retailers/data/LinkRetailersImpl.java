/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.balac.retailers.data;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class LinkRetailersImpl implements Link
{
	private Link delegate ;
	
  protected int maxFacOnLink = 0;
  private double potentialCustomers;
  private double potentialCompetitors;
  private double scoreSum;
  private int haveToPay;
  private int dontHaveToPay;
  private double landPrice;
  
  public LinkRetailersImpl(Link link, Network network, Double potentialCustomers, Double potentialCompetitors)
  {
    delegate = NetworkUtils.createLink(link.getId(), link.getFromNode(), link.getToNode(), network, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
    this.potentialCustomers = potentialCustomers.doubleValue();
    this.potentialCompetitors = potentialCompetitors.doubleValue();
    scoreSum = 0.0;
    haveToPay = 0;
    dontHaveToPay = 0;
  }

  public void setMaxFacOnLink(int max_number_facilities) {
    this.maxFacOnLink = max_number_facilities; }

  public void setPotentialCustomers(int potentialCustomers) {
    this.potentialCustomers = potentialCustomers; }

  public void setPotentialCustomers(double potentialCustomers) {
    this.potentialCustomers = potentialCustomers;
  }

  public void setPotentialCompetitors(int potentialCompetitors) {
    this.potentialCompetitors = potentialCompetitors;
  }

  public void setScoreSum(double scoreSum) {
	  this.scoreSum = scoreSum;
  }
  public void setHaveToPay(int x) {
	  
	  haveToPay = x;
  }
  public void setDontHaveToPay(int x) {
	  
	  dontHaveToPay = x;
  }
  public void setLandPrice(double x) {
	  landPrice = x;
  }
  public double getLandPrice() {
	  return landPrice;
  }
  public int getHaveToPay(int x) {
	  
	  return haveToPay;
  }
  public int getDontHaveToPay(int x) {
	  
	  return dontHaveToPay;
  }
  public double getScoreSum(){
	  return scoreSum;
  }
  public int getMaxFacOnLink() {
    return this.maxFacOnLink; }

  public double getPotentialCustomers() {
    return this.potentialCustomers; }

  public double getPotentialCompetitors() {
    return this.potentialCompetitors;
  }

public Id<Link> getId() {
	return this.delegate.getId();
}

public Coord getCoord() {
	return this.delegate.getCoord();
}

public boolean setFromNode(Node node) {
	return this.delegate.setFromNode(node);
}

public boolean setToNode(Node node) {
	return this.delegate.setToNode(node);
}

public Node getToNode() {
	return this.delegate.getToNode();
}

public Node getFromNode() {
	return this.delegate.getFromNode();
}

public double getLength() {
	return this.delegate.getLength();
}

public double getNumberOfLanes() {
	return this.delegate.getNumberOfLanes();
}

public double getNumberOfLanes(double time) {
	return this.delegate.getNumberOfLanes(time);
}

public double getFreespeed() {
	return this.delegate.getFreespeed();
}

public double getFreespeed(double time) {
	return this.delegate.getFreespeed(time);
}

public double getCapacity() {
	return this.delegate.getCapacity();
}

public double getCapacity(double time) {
	return this.delegate.getCapacity(time);
}

public void setFreespeed(double freespeed) {
	this.delegate.setFreespeed(freespeed);
}

public void setLength(double length) {
	this.delegate.setLength(length);
}

public void setNumberOfLanes(double lanes) {
	this.delegate.setNumberOfLanes(lanes);
}

public void setCapacity(double capacity) {
	this.delegate.setCapacity(capacity);
}

public void setAllowedModes(Set<String> modes) {
	this.delegate.setAllowedModes(modes);
}

public Set<String> getAllowedModes() {
	return this.delegate.getAllowedModes();
}

public double getFlowCapacityPerSec() {
	return this.delegate.getFlowCapacityPerSec();
}

public double getFlowCapacityPerSec(double time) {
	return this.delegate.getFlowCapacityPerSec(time);
}
}