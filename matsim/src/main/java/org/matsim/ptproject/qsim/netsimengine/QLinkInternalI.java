package org.matsim.ptproject.qsim.netsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QSimEngine;
import org.matsim.ptproject.qsim.interfaces.QVehicle;

public abstract class QLinkInternalI extends QBufferItem implements QLink {

	abstract void setQSimEngine(QSimEngine qsimEngine);

	abstract boolean moveLink(double now);

	abstract boolean hasSpace();

	abstract void clearVehicles();

	abstract QVehicle removeParkedVehicle(Id vehicleId);

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);

	abstract QSimEngine getQSimEngine() ;

}
