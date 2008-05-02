package org.matsim.utils.vis.netvis;

import java.awt.geom.AffineTransform;

import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.utils.vis.netvis.drawableNet.DrawableLinkI;

public interface DisplayableLinkI extends DrawableLinkI, BasicLink {

	public double getStartEasting();

	public double getEndEasting();

	public double getStartNorthing();

	public double getEndNorthing();

	public AffineTransform getLinear2PlaneTransform();

	public double getLength_m();

	public int getLanesAsInt(double time);

  public void build();

}