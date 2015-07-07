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

/**
 * Keeps track of constraining lists for a motif node to quickly query for the
 * smallest list
 */
public class MPQ {

    private PriorityObject[] heap;
    private PriorityObject[] poByMotifnode;
    private int size = 0;

    public MPQ(int initial_size) {
        heap = new PriorityObject[initial_size];
        poByMotifnode = new PriorityObject[initial_size];
        size = 0;
    }

    public PriorityObject poll() {
        if (size == 0) {
            return null;
        }
        PriorityObject result = heap[0];
        poByMotifnode[result.getFrom()] = null;
        size--;
        heap[0] = heap[size];
        heap[0].qpos = 0;
        result.qpos = -1;
        bubbleDown(0);
        return result;
    }

    public PriorityObject peek() {
        if (size == 0) {
            return null;
        }
        return heap[0];
    }

    public void add(PriorityObject element) {
        heap[size] = element;
        poByMotifnode[element.getFrom()] = element;
        element.qpos = size;
        size++;
        bubbleUp(size - 1);
    }

    private void bubbleUp(int pos) {
        PriorityObject element = heap[pos], parent = heap[(pos - 1) / 2];
        if (element.getScore() < parent.getScore()) {
            heap[pos] = parent;
            parent.qpos = pos;
            heap[(pos - 1) / 2] = element;
            element.qpos = (pos - 1) / 2;
            bubbleUp((pos - 1) / 2);
        }
    }

    private void bubbleDown(int pos) {
        if (size <= 2 * pos + 1) {
            return;
        }
        PriorityObject element = heap[pos], kid = heap[2 * pos + 1];
        int change = 2 * pos + 1;
        if (change + 1 < size && heap[change + 1].getScore() < kid.getScore()) {
            kid = heap[change + 1];
            change = 2 * pos + 2;
        }
        if (kid.getScore() < element.getScore() && change < size) {
            heap[pos] = kid;
            kid.qpos = pos;
            heap[change] = element;
            element.qpos = change;
            bubbleDown(change);
        }
    }

    public boolean isEmpty() {
        return (size == 0);
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < size; i++) {
            s += (heap[i] + "\t");
        }
        return s;
    }

    void remove(PriorityObject ro) {
        int pos = ro.qpos;
        size--;
        heap[pos] = heap[size];
        heap[pos].qpos = pos;
        bubbleDown(pos);
    }

    void remove(int motifNode) {
        PriorityObject po = poByMotifnode[motifNode];
        poByMotifnode[motifNode] = null;
        if (po != null) {
            remove(po);
        }
    }
}
