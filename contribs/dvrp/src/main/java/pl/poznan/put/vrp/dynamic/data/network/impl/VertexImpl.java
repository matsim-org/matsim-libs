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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import pl.poznan.put.vrp.dynamic.data.network.*;

public class VertexImpl
    implements Vertex
{
    private static final VertexBuilder BUILDER = new VertexImplBuilder();

    private final int id;
    private final String name;
    private final double x;
    private final double y;


    protected VertexImpl(int id, String name, double x, double y)
    {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }


    @Override
    public String toString()
    {
        return "Vertex_" + id;
    }


    public int getId()
    {
        return id;
    }


    public String getName()
    {
        return name;
    }


    public double getX()
    {
        return x;
    }


    public double getY()
    {
        return y;
    }


    public static VertexBuilder getBuilder()
    {
        return BUILDER;
    }


    private static class VertexImplBuilder
        implements VertexBuilder
    {
        private int id = -1;

        private String name;
        private double x;
        private double y;


        @Override
        public VertexImplBuilder setName(String name)
        {
            this.name = name;
            return this;
        }


        @Override
        public VertexImplBuilder setX(double x)
        {
            this.x = x;
            return this;
        }


        @Override
        public VertexImplBuilder setY(double y)
        {
            this.y = y;
            return this;
        }


        @Override
        public Vertex build()
        {
            id++;

            if (name == null) {
                return new VertexImpl(id, Integer.toString(id), x, y);
            }

            return new VertexImpl(id, name, x, y);
        }
    }
}
