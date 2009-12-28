package org.matsim.api.core.v01.network;

import java.io.Serializable;
import java.util.Set;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.world.Location;

/**
 * This interface deliberately does NOT have a back pointer ...
 * ... since, at this level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)
 */
public interface Link extends Identifiable, Serializable, Location {


	/**
	 * Sets this link's non-<code>null</code> upstream node.
	 *
	 * @param node
	 *            the <code>BasicNodeI</code> to be set
	 *
	 * @return <true> if <code>node</code> has been set and <code>false</code>
	 *         otherwise
	 *
	 * @throws IllegalArgumentException
	 *             if <code>node</code> is <code>null</code>
	 */
	public boolean setFromNode(Node node);

	/**
	 * Sets this link's non-<code>null</code> downstream node.
	 *
	 * @param node
	 *            the <code>BasicNodeI</code> to be set
	 *
	 * @return <code>true</code> if <code>node</code> has been set and
	 *         <code>false</code> otherwise
	 *
	 * @throws IllegalArgumentException
	 *             if <code>node</code> is <code>null</code>
	 */
	public boolean setToNode(Node node);

	/**
	 * @return this link's downstream node
	 */
	public Node getToNode();

	/**
	 * @return this link's upstream node
	 */
	public Node getFromNode();


	public double getLength();

	public double getNumberOfLanes(double time);

	public double getFreespeed(final double time);

	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 * @param time the time at which the capacity is requested. Use {@link Time#UNDEFINED_TIME} to get the default value.
	 * @return the capacity per network's capperiod timestep
	 * 
	 * @see NetworkLayer#getCapacityPeriod()
	 */
	public double getCapacity(double time);

	public void setFreespeed(double freespeed);

	public void setLength(double length);

	public void setNumberOfLanes(double lanes);

	public void setCapacity(double capacity);

	public void setAllowedModes(Set<TransportMode> modes);

	public Set<TransportMode> getAllowedModes();
}