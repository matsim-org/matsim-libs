package org.matsim.vis.netvis;

import java.awt.geom.AffineTransform;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vis.netvis.drawableNet.DrawableLinkI;

public interface DisplayableLinkI extends DrawableLinkI, Link {

	public double getStartEasting();

	public double getEndEasting();

	public double getStartNorthing();

	public double getEndNorthing();

	public AffineTransform getLinear2PlaneTransform();

	public double getLength_m();

	public int getLanesAsInt(double time);

  public void build();

}