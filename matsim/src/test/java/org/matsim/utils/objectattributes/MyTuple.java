/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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


package org.matsim.utils.objectattributes;

public class MyTuple {
    public final int a;
    public final int b;
    public MyTuple(final int a, final int b) {
        this.a = a;
        this.b = b;
    }
    @Override
    public String toString() {
        return a + "/" + b;
    }

    public static class MyTupleConverter implements AttributeConverter<MyTuple> {
        @Override
        public MyTuple convert(String value) {
            String[] parts = value.split(",");
            return new MyTuple(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
        }
        @Override
        public String convertToString(Object o) {
            MyTuple t = (MyTuple) o;
            return t.a + "," + t.b; // make it something different from MyTuple.toString()
        }
    }
}
