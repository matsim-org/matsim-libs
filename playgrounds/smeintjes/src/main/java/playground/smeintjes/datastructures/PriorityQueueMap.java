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

import java.util.*;
import playground.smeintjes.network.*;

public class PriorityQueueMap {

    private MPQ[] map;

    public PriorityQueueMap(int n) {
        map = new MPQ[n];
        for (int i = 0; i < n; i++) {
            map[i] = new MPQ(n);
        }
    }

    public void add(Node node, int from, int to, int nbs) {
        map[to].add(new PriorityObject(node, from, to, nbs));
    }

    public void add(PriorityObject ro) {
        map[ro.getTo()].add(ro);
    }

    public PriorityObject poll(Set<Integer> indices) {
        Iterator<Integer> iterator = indices.iterator();

        int element = (int) iterator.next();
        MPQ minpq = map[element];
        if (iterator.hasNext()) {
            PriorityObject po = minpq.peek();
            int minScore = po == null ? Integer.MAX_VALUE : po.getScore();
            do {
                element = iterator.next();
                MPQ pq = map[element];
                if (!pq.isEmpty()) {
                    int score = pq.peek().getScore();
                    if (score < minScore) {
                        minScore = score;
                        minpq = pq;
                    }
                }
            } while (iterator.hasNext());
        }
        return minpq.peek();
    }

    @Override
    public String toString() {
        return "" + map;
    }

    public void remove(PriorityObject ro) {
        map[ro.getTo()].remove(ro);
    }

    public void remove(int motifNode, int i) {
        map[i].remove(motifNode);
    }
}
