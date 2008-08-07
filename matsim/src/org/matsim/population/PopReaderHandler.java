/* *********************************************************************** *
 * project: org.matsim.*
 * PopReaderHandler.java
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

package org.matsim.population;

import org.xml.sax.Attributes;

public interface PopReaderHandler {

	//////////////////////////////////////////////////////////////////////
	// <synthetic_population ... > ... </synthetic_population>
	//////////////////////////////////////////////////////////////////////

	public void startSynPop(final Attributes meta);

	public void endSynPop();

	//////////////////////////////////////////////////////////////////////
	// <agent ... > ... </agent>
	//////////////////////////////////////////////////////////////////////

	public void startAgent(final Attributes meta);

	public void endAgent();

	//////////////////////////////////////////////////////////////////////
	// <home ... > ... </home>
	//////////////////////////////////////////////////////////////////////

	public void startHome(final Attributes meta);

	public void endHome();

	//////////////////////////////////////////////////////////////////////
	// <workplace ... > ... </workplace>
	//////////////////////////////////////////////////////////////////////

	public void startWorkplace(final Attributes meta);

	public void endWorkplace();

	//////////////////////////////////////////////////////////////////////
	// <location ... />
	//////////////////////////////////////////////////////////////////////

	public void startLocation(final Attributes meta);

	public void endLocation();

	//////////////////////////////////////////////////////////////////////
	// <age ... />
	//////////////////////////////////////////////////////////////////////

	public void startAge(final Attributes meta);

	public void endAge();

	//////////////////////////////////////////////////////////////////////
	// <sex ... />
	//////////////////////////////////////////////////////////////////////

	public void startSex(final Attributes meta);

	public void endSex();

	//////////////////////////////////////////////////////////////////////
	// <driver_licence_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startDLicence(final Attributes meta);

	public void endDLicence();

	//////////////////////////////////////////////////////////////////////
	// <car_availibility ... />
	//////////////////////////////////////////////////////////////////////

	public void startCarAvail(final Attributes meta);

	public void endCarAvail();

	//////////////////////////////////////////////////////////////////////
	// <employed ... />
	//////////////////////////////////////////////////////////////////////

	public void startEmployed(final Attributes meta);

	public void endEmployed();

	//////////////////////////////////////////////////////////////////////
	// <half_fare_ticket_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startHalfFare(final Attributes meta);

	public void endHalfFare();

	//////////////////////////////////////////////////////////////////////
	// <general_abonnement_ownership ... />
	//////////////////////////////////////////////////////////////////////

	public void startGA(final Attributes meta);

	public void endGA();

	//////////////////////////////////////////////////////////////////////
	// <household_monthly_income ... />
	//////////////////////////////////////////////////////////////////////

	public void startIncome(final Attributes meta);

	public void endIncome();
}
