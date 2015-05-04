/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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

package playground.southafrica.utilities.analysis;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 *
 * @author jwjoubert
 */
public class VktEstimatorTest {
	private Geometry geom = null;
	List<Plan> plans = new ArrayList<>(2);

	/**
	 * Builds the following geometry:
	 *          (5,10)
	 *            B
	 *           /\
	 *          /  \
	 *         /    \
	 *        /      \  
	 *       /    D   \ E
	 *      /          \
	 *     /            \
	 *    /_______F______\
	 *   A                C
	 * (0,0)            (10,0)
	 * 
	 * with three plans: 
	 *   1) D -> E
	 *   2) D -> F
	 *   3) F -> D -> E
	 *   
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* Build the geometry. */
		GeometryFactory gf = new GeometryFactory();
		Coordinate a = new Coordinate(0.0, 0.0);
		Coordinate b = new Coordinate(5.0, 10.0);
		Coordinate c = new Coordinate(10.0, 0.0);
		Coordinate[] ca = {a, b, c, a};
		geom = gf.createPolygon(ca);
		
		/* Create the plans. */
		PopulationFactory pf = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		Plan plan1 = new PlanImpl();
		plan1.addActivity(pf.createActivityFromCoord("D", new CoordImpl(5.0, 5.0)));
		plan1.addLeg(new LegImpl("walk"));
		plan1.addActivity(pf.createActivityFromCoord("E", new CoordImpl(9.0, 5.0)));
		plans.add(plan1);
		
		Plan plan2 = new PlanImpl();
		plan2.addActivity(pf.createActivityFromCoord("D", new CoordImpl(5.0, 5.0)));
		plan2.addLeg(new LegImpl("walk"));
		plan2.addActivity(pf.createActivityFromCoord("F", new CoordImpl(5.0, 0.0)));
		plans.add(plan2);
		
		Plan plan3 = new PlanImpl();
		plan3.addActivity(pf.createActivityFromCoord("F", new CoordImpl(5.0, 0.0)));
		plan3.addLeg(new LegImpl("walk"));
		plan3.addActivity(pf.createActivityFromCoord("D", new CoordImpl(5.0, 5.0)));
		plan3.addLeg(new LegImpl("walk"));
		plan3.addActivity(pf.createActivityFromCoord("E", new CoordImpl(9.0, 5.0)));
		plans.add(plan3);
	}

	@Test
	public void test() {
		double d1 = VktEstimator.estimateVktFromPlan(plans.get(0), geom);
		Assert.assertEquals("Wrong length for plan 1.", 2.5*VktEstimator.DISTANCE_MULTIPLIER/1000.0, d1, MatsimTestUtils.EPSILON);
		
		double d2 = VktEstimator.estimateVktFromPlan(plans.get(1), geom);
		Assert.assertEquals("Wrong length for plan 2.", 5.0*VktEstimator.DISTANCE_MULTIPLIER/1000.0, d2, MatsimTestUtils.EPSILON);

		double d3 = VktEstimator.estimateVktFromPlan(plans.get(2), geom);
		Assert.assertEquals("Wrong length for plan 3.", (5.0+2.5)*VktEstimator.DISTANCE_MULTIPLIER/1000.0, d3, MatsimTestUtils.EPSILON);
	}

}
