package org.matsim.contrib.osm.networkReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Link;

public class LinkPropertiesTest {

	@Test
	void calculateSpeedIfNoTag_excludedHierarchy() {

        var motorwayProperties = new LinkProperties(LinkProperties.LEVEL_MOTORWAY, 1, 50 / 3.6, 1000, false);
        var residentialProperties = new LinkProperties(LinkProperties.LEVEL_RESIDENTIAL, 1, 50 / 3.6, 1000, false);

        assertEquals(motorwayProperties.freespeed, LinkProperties.calculateSpeedIfNoSpeedTag(200, motorwayProperties), 0.0);
        assertEquals(residentialProperties.freespeed, LinkProperties.calculateSpeedIfNoSpeedTag(200, residentialProperties), 0.0);
    }

	@Test
	void calculateSpeedIfNoTag_longLink() {

        var properties = new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 50 / 3.6, 1000, false);
        var linkLength = 350; // longer than the threshold

        var result = LinkProperties.calculateSpeedIfNoSpeedTag(linkLength, properties);

        assertEquals(properties.freespeed, result, 0.0);
    }

	@Test
	void calculateSpeedIfNoTag_shortLink() {

        var properties = new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 50 / 3.6, 1000, false);
        var linkLength = 1; // very shor link

        var result = LinkProperties.calculateSpeedIfNoSpeedTag(linkLength, properties);

        assertEquals(10 / 3.6, result, 0.1); // should be pretty much 10 km/h
    }

	@Test
	void calculateSpeedIfSpeedTag_withoutUrbanFactor() {

        var maxSpeed = 100 / 3.6;
        assertEquals(maxSpeed, LinkProperties.calculateSpeedIfSpeedTag(maxSpeed), 0.0);
    }

	@Test
	void calculateSpeedIfSpeedTag_withUrbanFactor() {

        var maxSpeed = 50 / 3.6;
        assertEquals(maxSpeed * 0.9, LinkProperties.calculateSpeedIfSpeedTag(maxSpeed), Double.MIN_VALUE);
    }

	@Test
	void getLaneCapacity_withIncrease() {

        var linkLength = 40;
        var properties = new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 50 / 3.6, 1000, false);

        assertEquals(properties.laneCapacity * 2, LinkProperties.getLaneCapacity(linkLength, properties), Double.MIN_VALUE);
    }

	@Test
	void getLaneCapacity_withoutIncrease() {

        var linkLength = 101;
        var properties = new LinkProperties(LinkProperties.LEVEL_PRIMARY, 1, 50 / 3.6, 1000, false);

        assertEquals(properties.laneCapacity, LinkProperties.getLaneCapacity(linkLength, properties), Double.MIN_VALUE);
    }
}
