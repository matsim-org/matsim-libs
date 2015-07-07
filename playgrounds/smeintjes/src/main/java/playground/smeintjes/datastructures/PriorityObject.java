/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */
package playground.smeintjes.datastructures;

import playground.smeintjes.network.*;

public class PriorityObject implements Comparable {

    private Node start;
    private int fromPosition;
    private int toPosition;
    private int numberOfNeighbours;
    public int qpos;

    public PriorityObject(Node start, int fromPosition, int toPosition, int numberOfNeighbours) {
        this.start = start;
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.numberOfNeighbours = numberOfNeighbours;
    }

    public Node getNode() {
        return start;
    }

    public int getFrom() {
        return fromPosition;
    }

    public int getTo() {
        return toPosition;
    }

    public int getScore() {
        return numberOfNeighbours;
    }

    public int compareTo(Object o) {
        if (o instanceof PriorityObject) {
            return this.numberOfNeighbours - ((PriorityObject) o).numberOfNeighbours;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "<" + start + "," + fromPosition + "," + toPosition + "," + numberOfNeighbours + ">";
    }
}
