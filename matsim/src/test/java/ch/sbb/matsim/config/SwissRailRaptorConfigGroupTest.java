/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.config;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.RangeQuerySettingsParameterSet;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.RouteSelectorParameterSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorConfigGroupTest {

    @BeforeEach
    public void setup() {
        System.setProperty("matsim.preferLocalDtds", "true");
    }

	@Test
	void testConfigIO_general() {
        SwissRailRaptorConfigGroup config1 = new SwissRailRaptorConfigGroup();

        { // prepare config1
            config1.setUseRangeQuery(true);
            config1.setUseIntermodalAccessEgress(true);
            config1.setUseModeMappingForPassengers(true);
            config1.setTransferPenaltyCostPerTravelTimeHour(0.0031 * 3600);
        }

        SwissRailRaptorConfigGroup config2 = writeRead(config1);

        // do checks
        Assertions.assertTrue(config2.isUseRangeQuery());
        Assertions.assertTrue(config2.isUseIntermodalAccessEgress());
        Assertions.assertTrue(config2.isUseModeMappingForPassengers());
        Assertions.assertEquals(0.0031 * 3600, config2.getTransferPenaltyCostPerTravelTimeHour(), 0.0);
    }

	@Test
	void testConfigIO_rangeQuery() {
        SwissRailRaptorConfigGroup config1 = new SwissRailRaptorConfigGroup();

        { // prepare config1
            config1.setUseRangeQuery(true);

            RangeQuerySettingsParameterSet range1 = new RangeQuerySettingsParameterSet();
            range1.setSubpopulations("");
            range1.setMaxEarlierDeparture(10*60);
            range1.setMaxLaterDeparture(59*60);
            config1.addRangeQuerySettings(range1);

            RangeQuerySettingsParameterSet range2 = new RangeQuerySettingsParameterSet();
            range2.setSubpopulations("inflexible");
            range2.setMaxEarlierDeparture(60);
            range2.setMaxLaterDeparture(15*60);
            config1.addRangeQuerySettings(range2);
        }

        SwissRailRaptorConfigGroup config2 = writeRead(config1);

        // do checks
        Assertions.assertTrue(config2.isUseRangeQuery());

        RangeQuerySettingsParameterSet range1 = config2.getRangeQuerySettings(null);
        Assertions.assertNotNull(range1);
        Assertions.assertEquals(0, range1.getSubpopulations().size());
        Assertions.assertEquals(10*60, range1.getMaxEarlierDeparture());
        Assertions.assertEquals(59*60, range1.getMaxLaterDeparture());

        RangeQuerySettingsParameterSet range2 = config2.getRangeQuerySettings("inflexible");
        Assertions.assertNotNull(range2);
        Assertions.assertEquals(1, range2.getSubpopulations().size());
        Assertions.assertEquals("inflexible", range2.getSubpopulations().iterator().next());
        Assertions.assertEquals(60, range2.getMaxEarlierDeparture());
        Assertions.assertEquals(15*60, range2.getMaxLaterDeparture());
    }

	@Test
	void testConfigIO_routeSelector() {
        SwissRailRaptorConfigGroup config1 = new SwissRailRaptorConfigGroup();

        { // prepare config1
            config1.setUseRangeQuery(true);

            RouteSelectorParameterSet selector1 = new RouteSelectorParameterSet();
            selector1.setSubpopulations("");
            selector1.setBetaTransfers(600);
            selector1.setBetaDepartureTime(1.6);
            selector1.setBetaTravelTime(1.3);
            config1.addRouteSelector(selector1);

            RouteSelectorParameterSet selector2 = new RouteSelectorParameterSet();
            selector2.setSubpopulations("inflexible");
            selector2.setBetaTransfers(500);
            selector2.setBetaDepartureTime(5);
            selector2.setBetaTravelTime(1.2);
            config1.addRouteSelector(selector2);
        }

        SwissRailRaptorConfigGroup config2 = writeRead(config1);

        // do checks
        Assertions.assertTrue(config2.isUseRangeQuery());

        RouteSelectorParameterSet selector1 = config2.getRouteSelector(null);
        Assertions.assertNotNull(selector1);
        Assertions.assertEquals(0, selector1.getSubpopulations().size());
        Assertions.assertEquals(600, selector1.getBetaTransfers(), 0.0);
        Assertions.assertEquals(1.6, selector1.getBetaDepartureTime(), 0.0);
        Assertions.assertEquals(1.3, selector1.getBetaTravelTime(), 0.0);

        RouteSelectorParameterSet selector2 = config2.getRouteSelector("inflexible");
        Assertions.assertNotNull(selector2);
        Assertions.assertEquals(1, selector2.getSubpopulations().size());
        Assertions.assertEquals(500, selector2.getBetaTransfers(), 0.0);
        Assertions.assertEquals(5, selector2.getBetaDepartureTime(), 0.0);
        Assertions.assertEquals(1.2, selector2.getBetaTravelTime(), 0.0);
    }

	@Test
	void testConfigIO_intermodalAccessEgress() {
        SwissRailRaptorConfigGroup config1 = new SwissRailRaptorConfigGroup();

        { // prepare config1
            config1.setUseIntermodalAccessEgress(true);

            IntermodalAccessEgressParameterSet paramset1 = new IntermodalAccessEgressParameterSet();
            paramset1.setMode(TransportMode.bike);
            paramset1.setMaxRadius(2000);
            paramset1.setInitialSearchRadius(1500);
            paramset1.setSearchExtensionRadius(1000);
            paramset1.setShareTripSearchRadius(0.01);
            paramset1.setPersonFilterAttribute(null);
            paramset1.setStopFilterAttribute("bikeAndRail");
            paramset1.setStopFilterValue("true");
            config1.addIntermodalAccessEgress(paramset1);

            IntermodalAccessEgressParameterSet paramset2 = new IntermodalAccessEgressParameterSet();
            paramset2.setMode("sff");
            paramset2.setMaxRadius(5000);
            paramset2.setInitialSearchRadius(3000);
            paramset2.setSearchExtensionRadius(2000);
            paramset2.setPersonFilterAttribute("sff_user");
            paramset2.setPersonFilterValue("true");
            paramset2.setLinkIdAttribute("linkId_sff");
            paramset2.setStopFilterAttribute("stop-type");
            paramset2.setStopFilterValue("hub");
            config1.addIntermodalAccessEgress(paramset2);
        }

        SwissRailRaptorConfigGroup config2 = writeRead(config1);

        // do checks
        Assertions.assertTrue(config2.isUseIntermodalAccessEgress());

        List<IntermodalAccessEgressParameterSet> parameterSets = config2.getIntermodalAccessEgressParameterSets();
        Assertions.assertNotNull(parameterSets);
        Assertions.assertEquals(2, parameterSets.size(), "wrong number of parameter sets");

        IntermodalAccessEgressParameterSet paramSet1 = parameterSets.get(0);
        Assertions.assertEquals(TransportMode.bike, paramSet1.getMode());
        Assertions.assertEquals(2000, paramSet1.getMaxRadius(), 0.0);
        Assertions.assertEquals(1500, paramSet1.getInitialSearchRadius(), 0.0);
        Assertions.assertEquals(1000, paramSet1.getSearchExtensionRadius(), 0.0);
        Assertions.assertEquals(0.01, paramSet1.getShareTripSearchRadius(), 0.0);
        Assertions.assertNull(paramSet1.getPersonFilterAttribute());
        Assertions.assertNull(paramSet1.getPersonFilterValue());
        Assertions.assertNull(paramSet1.getLinkIdAttribute());
        Assertions.assertEquals("bikeAndRail", paramSet1.getStopFilterAttribute());
        Assertions.assertEquals("true", paramSet1.getStopFilterValue());

        IntermodalAccessEgressParameterSet paramSet2 = parameterSets.get(1);
        Assertions.assertEquals("sff", paramSet2.getMode());
        Assertions.assertEquals(5000, paramSet2.getMaxRadius(), 0.0);
        Assertions.assertEquals(3000, paramSet2.getInitialSearchRadius(), 0.0);
        Assertions.assertEquals(2000, paramSet2.getSearchExtensionRadius(), 0.0);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, paramSet2.getShareTripSearchRadius(), 0.0);
        Assertions.assertEquals("sff_user", paramSet2.getPersonFilterAttribute());
        Assertions.assertEquals("true", paramSet2.getPersonFilterValue());
        Assertions.assertEquals("linkId_sff", paramSet2.getLinkIdAttribute());
        Assertions.assertEquals("stop-type", paramSet2.getStopFilterAttribute());
        Assertions.assertEquals("hub", paramSet2.getStopFilterValue());
    }

	@Test
	void testConfigIO_modeMappings() {
        SwissRailRaptorConfigGroup config1 = new SwissRailRaptorConfigGroup();

        { // prepare config1
            config1.setUseModeMappingForPassengers(true);

            ModeMappingForPassengersParameterSet mapping1 = new ModeMappingForPassengersParameterSet();
            mapping1.setRouteMode("train");
            mapping1.setPassengerMode("rail");
            config1.addModeMappingForPassengers(mapping1);

            ModeMappingForPassengersParameterSet mapping2 = new ModeMappingForPassengersParameterSet();
            mapping2.setRouteMode("tram");
            mapping2.setPassengerMode("rail");
            config1.addModeMappingForPassengers(mapping2);

            ModeMappingForPassengersParameterSet mapping3 = new ModeMappingForPassengersParameterSet();
            mapping3.setRouteMode("bus");
            mapping3.setPassengerMode("road");
            config1.addModeMappingForPassengers(mapping3);
        }

        SwissRailRaptorConfigGroup config2 = writeRead(config1);

        // do checks
        Assertions.assertTrue(config2.isUseModeMappingForPassengers());

        ModeMappingForPassengersParameterSet trainMapping = config2.getModeMappingForPassengersParameterSet("train");
        Assertions.assertNotNull(trainMapping);
        Assertions.assertEquals("train", trainMapping.getRouteMode());
        Assertions.assertEquals("rail", trainMapping.getPassengerMode());

        ModeMappingForPassengersParameterSet tramMapping = config2.getModeMappingForPassengersParameterSet("tram");
        Assertions.assertNotNull(tramMapping);
        Assertions.assertEquals("tram", tramMapping.getRouteMode());
        Assertions.assertEquals("rail", tramMapping.getPassengerMode());

        ModeMappingForPassengersParameterSet busMapping = config2.getModeMappingForPassengersParameterSet("bus");
        Assertions.assertNotNull(busMapping);
        Assertions.assertEquals("bus", busMapping.getRouteMode());
        Assertions.assertEquals("road", busMapping.getPassengerMode());

        Assertions.assertNull(config2.getModeMappingForPassengersParameterSet("road"));
        Assertions.assertNull(config2.getModeMappingForPassengersParameterSet("ship"));
    }

    private SwissRailRaptorConfigGroup writeRead(SwissRailRaptorConfigGroup config) {
        Config fullConfig1 = ConfigUtils.createConfig(config);

        // write config1
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output);
        new ConfigWriter(fullConfig1).writeStream(writer);

        // read config in again as config2
        SwissRailRaptorConfigGroup config2 = new SwissRailRaptorConfigGroup();
        Config fullConfig2 = ConfigUtils.createConfig(config2);

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        new ConfigReader(fullConfig2).parse(input);

        return config2;
    }
}
