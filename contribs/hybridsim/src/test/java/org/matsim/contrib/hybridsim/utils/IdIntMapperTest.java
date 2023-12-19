package org.matsim.contrib.hybridsim.utils;
/****************************************************************************/
// casim, cellular automaton simulation for multi-destination pedestrian
// crowds; see https://github.com/CACrowd/casim
// Copyright (C) 2016 CACrowd and contributors
/****************************************************************************/
//
//   This file is part of casim.
//   casim is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 2 of the License, or
//   (at your option) any later version.
//
/****************************************************************************/

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by laemmel on 15/08/16.
 */
public class IdIntMapperTest {


    private IdIntMapper mapper;

    @BeforeEach
    public void setup() {
        this.mapper = new IdIntMapper();
    }

    @AfterEach
    public void tearDown() {
        this.mapper = null;
    }

	@Test
	void testLinkIdIntMapper() {
        int id1 = mapper.getIntLink(Id.createLinkId("l1"));
        assertThat(id1, is(0));
        mapper.getIntLink(Id.createLinkId("l2"));
        mapper.getIntLink(Id.createLinkId("l3"));
        int id1P = mapper.getIntLink(Id.createLinkId("l1"));
        assertThat(id1P, is(0));
        int id3 = mapper.getIntLink(Id.createLinkId("l3"));
        assertThat(id3, is(2));
    }

	@Test
	void testIntLinkIdMapper() {
        Id<Link> linkId1 = mapper.getLinkId(10);
        assertThat(linkId1, is(nullValue()));
        mapper.getIntLink(Id.createLinkId("l1"));
        Id<Link> linkId2 = mapper.getLinkId(0);
        assertThat(linkId2, is(Id.createLinkId("l1")));

    }

	@Test
	void testNodeIdIntMapper() {
        int id1 = mapper.getIntNode(Id.createNodeId("l1"));
        assertThat(id1, is(0));
        mapper.getIntNode(Id.createNodeId("l2"));
        mapper.getIntNode(Id.createNodeId("l3"));
        int id1P = mapper.getIntNode(Id.createNodeId("l1"));
        assertThat(id1P, is(0));
        int id3 = mapper.getIntNode(Id.createNodeId("l3"));
        assertThat(id3, is(2));
    }

	@Test
	void testIntNodeIdMapper() {
        Id<Node> nodeId1 = mapper.getNodeId(10);
        assertThat(nodeId1, is(nullValue()));
        mapper.getIntNode(Id.createNodeId("l1"));
        Id<Node> nodeId2 = mapper.getNodeId(0);
        assertThat(nodeId2, is(Id.createNodeId("l1")));

    }


	@Test
	void testPersonIdIntMapper() throws Exception {
        int id1 = mapper.getIntPerson(Id.createPersonId("l1"));
        assertThat(id1, is(0));
        mapper.getIntPerson(Id.createPersonId("l2"));
        mapper.getIntPerson(Id.createPersonId("l3"));
        int id1P = mapper.getIntPerson(Id.createPersonId("l1"));
        assertThat(id1P, is(0));
        int id3 = mapper.getIntPerson(Id.createPersonId("l3"));
        assertThat(id3, is(2));
    }

	@Test
	void testIntPersonIdMapper() throws Exception {
        Id<Person> personId1 = mapper.getPersonId(10);
        assertThat(personId1, is(nullValue()));
        mapper.getIntPerson(Id.createPersonId("l1"));
        Id<Person> personId2 = mapper.getPersonId(0);
        assertThat(personId2, is(Id.createPersonId("l1")));
    }
}
