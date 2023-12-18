/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.noise;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author nkuehnel
 *
 */
public class NoiseRLS19IT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testShielding() {
        final RLS19ShieldingCorrection shieldingCorrection = new RLS19ShieldingCorrection();


        Assertions.assertEquals(34.463933081239965,
                shieldingCorrection.calculateShieldingCorrection(10,15,15,15), MatsimTestUtils.EPSILON, "Wrong shielding value z!");

        Config config = ConfigUtils.createConfig();
        NoiseConfigGroup noiseConfigGroup = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
        noiseConfigGroup.setConsiderNoiseBarriers(true);
        Collection<FeatureNoiseBarrierImpl> barriers = new HashSet<>();

        Coordinate[] shell = new Coordinate[5];
        shell[0] = new Coordinate(3,3);
        shell[1] = new Coordinate(6,3);
        shell[2] = new Coordinate(6,6);
        shell[3] = new Coordinate(3,6);
        shell[4] = new Coordinate(3,3);

        Geometry geom = new GeometryFactory().createPolygon(shell);
        FeatureNoiseBarrierImpl barrier = new FeatureNoiseBarrierImpl(Id.create("1", NoiseBarrier.class), geom, 10);
        barriers.add(barrier);
        BarrierContext barrierContext = new BarrierContext(barriers);
        ShieldingContext context = new ShieldingContext(config, shieldingCorrection, barrierContext);


        ReceiverPoint rp = new NoiseReceiverPoint(Id.create("a", ReceiverPoint.class), new Coord(0, 0));
        Network network = NetworkUtils.createNetwork();
        Node from = NetworkUtils.createNode(Id.createNodeId("node1"), new Coord(6, 7));
        Node to = NetworkUtils.createNode(Id.createNodeId("node1"), new Coord(7,6));
        Link link = NetworkUtils.createLink(Id.createLinkId("link"), from, to, network, 10, 0,0,0);
        final Coord coord = CoordUtils.orthogonalProjectionOnLineSegment(
                link.getFromNode().getCoord(), link.getToNode().getCoord(), rp.getCoord());
        final double v = context.determineShieldingValue(rp, link, coord);

        Assertions.assertEquals(30.78517993490919,
                v, MatsimTestUtils.EPSILON, "Wrong shielding correction!");

    }

	@Test
	void testEmission() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        Node from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
        Node to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(400, 0));
        final Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId("1"), from, to, 400, 40 / 3.6, 0, 0);

        NoiseLink noiseLink = new NoiseLink(link.getId());

        RLS19NoiseEmission emission = new RLS19NoiseEmission(scenario, new RoadSurfaceContext(network), new DEMContextImpl(scenario.getConfig()));

        final double basePkwEmission = emission.calculateBaseVehicleTypeEmission(RLS19VehicleType.pkw, 40);
        Assertions.assertEquals(97.70334139531323, basePkwEmission, MatsimTestUtils.EPSILON, "Wrong base pkw emission!");

        double singleVehicleEmission =
                emission.calculateSingleVehicleEmission(noiseLink, RLS19VehicleType.pkw, 40);
        Assertions.assertEquals(97.70334139531323, singleVehicleEmission, MatsimTestUtils.EPSILON, "Wrong single pkw emission!");

        final double vehicleTypePart = emission.calculateVehicleTypeNoise(1, 40, singleVehicleEmission);
        Assertions.assertEquals(1.4732421924637252E8, vehicleTypePart, MatsimTestUtils.EPSILON, "Wrong pkw emission sum part!");

        final double noiseLinkEmission = emission.calculateEmission(noiseLink, 40, 40, 40, 1800, 0, 0);
        Assertions.assertEquals(84.23546653306667, noiseLinkEmission, MatsimTestUtils.EPSILON, "Wrong noise link emission!");

        for (int i = 0; i < 1800; i++) {
            noiseLink.addEnteringAgent(RLS19VehicleType.pkw);
        }

        emission.calculateEmission(noiseLink);
        Assertions.assertEquals(84.23546653306667, noiseLink.getEmission(), MatsimTestUtils.EPSILON, "Wrong final noise link emission!");
    }

	@Test
	void testImmission() {

        final RLS19ShieldingCorrection shieldingCorrection = new RLS19ShieldingCorrection();

        Config config = ConfigUtils.createConfig();
        NoiseConfigGroup noiseConfigGroup = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
        noiseConfigGroup.setConsiderNoiseBarriers(true);
        Collection<FeatureNoiseBarrierImpl> barriers = new HashSet<>();

        Coordinate[] shell = new Coordinate[5];
        shell[0] = new Coordinate(3,3);
        shell[1] = new Coordinate(6,3);
        shell[2] = new Coordinate(6,6);
        shell[3] = new Coordinate(3,6);
        shell[4] = new Coordinate(3,3);

        Geometry geom = new GeometryFactory().createPolygon(shell);
        FeatureNoiseBarrierImpl barrier = new FeatureNoiseBarrierImpl(Id.create("1", NoiseBarrier.class), geom, 10);
        barriers.add(barrier);

        BarrierContext barrierContext = new BarrierContext(barriers);
        ShieldingContext shieldingContext = new ShieldingContext(shieldingCorrection, barrierContext);
        ReflectionContext reflectionContext = new ReflectionContext(barrierContext);


        Scenario scenario = ScenarioUtils.createScenario(config);
        final NoiseContextStub noiseContext = new NoiseContextStub(scenario);

        Network network = scenario.getNetwork();
        Node from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(6, 7));
        Node to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(7, 6));

        final Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId("link"), from, to, 10, 40 / 3.6, 0, 0);
        final Link link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId("link2"), from, to, 10, 40 / 3.6, 0, 0);

        final NoiseLink noiseLink = new NoiseLink(link.getId());
        final NoiseLink noiseLink2 = new NoiseLink(link2.getId());

        noiseContext.getNoiseLinks().put(link.getId(), noiseLink);
        noiseContext.getNoiseLinks().put(link2.getId(), noiseLink2);

        RLS19NoiseEmission emission = new RLS19NoiseEmission(scenario, new RoadSurfaceContext(network), new DEMContextImpl(scenario.getConfig()));
        for (int i = 0; i < 1800; i++) {
            noiseLink.addEnteringAgent(RLS19VehicleType.pkw);
            noiseLink2.addEnteringAgent(RLS19VehicleType.pkw);
        }

        emission.calculateEmission(noiseLink);
        emission.calculateEmission(noiseLink2);
        Assertions.assertEquals(84.23546653306667, noiseLink.getEmission(), MatsimTestUtils.EPSILON, "Wrong final noise link emission!");

        NoiseReceiverPoint rp = new NoiseReceiverPoint(Id.create("a", ReceiverPoint.class), new Coord(0,0));


        RLS19NoiseImmission immission = new RLS19NoiseImmission(noiseContext, shieldingContext, new IntersectionContext(network), reflectionContext);

        noiseLink.setEmission(65);
        noiseLink2.setEmission(55);

        rp.setLinkId2Correction(link.getId(), 6.6622);
        rp.setLinkId2Correction(link2.getId(), 8.794733);

        immission.calculateImmission(rp, 8*3600);
        final double currentImmission = rp.getCurrentImmission();

        Assertions.assertEquals(73.77467715144601,
                currentImmission, MatsimTestUtils.EPSILON, "Wrong immission!");
    }
}
