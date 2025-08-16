package org.matsim.core.router.speedy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author mrieser / Simunto
 */
public class DAryMinHeapTest {

	@Test
	void testPoll() {
		double cost[] = new double[10];
		DAryMinHeap pq = new DAryMinHeap(20, 3);

		cost[0] = 5;
		cost[1] = 2;
		cost[2] = 4;
		cost[3] = 8;
		cost[4] = 10;
		cost[5] = 1;
		cost[6] = 3;
		cost[7] = 6;

		pq.insert(2, cost[2]);
		pq.insert(1, cost[1]);
		pq.insert(0, cost[0]);

		Assertions.assertEquals(3, pq.size());

		Assertions.assertEquals(1, pq.poll());
		Assertions.assertEquals(2, pq.poll());
		Assertions.assertEquals(0, pq.poll());

		Assertions.assertTrue(pq.isEmpty());

		for (int i = 0; i < 8; i++) {
			pq.insert(i, cost[i]);
		}

		Assertions.assertEquals(5, pq.poll());
		Assertions.assertEquals(1, pq.poll());
		Assertions.assertEquals(6, pq.poll());
		Assertions.assertEquals(2, pq.poll());
		Assertions.assertEquals(0, pq.poll());
		Assertions.assertEquals(7, pq.poll());
		Assertions.assertEquals(3, pq.poll());
		Assertions.assertEquals(4, pq.poll());
		Assertions.assertTrue(pq.isEmpty());
	}

	@Test
	void testDecreaseKey() {
		DAryMinHeap pq = new DAryMinHeap(20, 4);

		pq.insert(2, 4);
		pq.insert(1, 2);
		pq.insert(0, 5);

		pq.decreaseKey(2, 1.0);

		Assertions.assertEquals(2, pq.poll());
		Assertions.assertEquals(1, pq.poll());
		Assertions.assertEquals(0, pq.poll());
		Assertions.assertTrue(pq.isEmpty());
	}

	@Test
	void stresstest() {
		int cnt = 2000;
		double[] cost = new double[cnt];
		Random r = new Random(20190210L);
		for (int i = 0; i < cnt; i++) {
			cost[i] = (int) (r.nextDouble() * cnt * 100);
		}

		DAryMinHeap pq = new DAryMinHeap(cnt, 2);

		for (int i = 0; i < cnt; i++) {
			pq.insert(i, cost[i]);
		}
		double lastCost = -1;
		int step = 0;
		while (!pq.isEmpty()) {
			step++;
			int node = pq.poll();
			double nodeCost = cost[node];
			Assertions.assertTrue(lastCost <= nodeCost, step + ": " + lastCost + " <= " + nodeCost);
			lastCost = nodeCost;
		}

		// start again, but add some decreaseKey operations
		for (int i = 0; i < cnt; i++) {
			pq.insert(i, cost[i]);
		}

		for (int i = 0; i < cnt; i++) {
			double newCost = (int) (r.nextDouble() * cnt * 100);
			if (newCost < cost[i]) {
				pq.decreaseKey(i, newCost);
				cost[i] = newCost;
			}
		}

		lastCost = -1;
		step = 0;
		while (!pq.isEmpty()) {
			step++;
			int node = pq.poll();
			double nodeCost = cost[node];
			Assertions.assertTrue(lastCost <= nodeCost, step + ": " + lastCost + " <= " + nodeCost);
			lastCost = nodeCost;
		}

	}

	@Test
	void testIterator() {
		DAryMinHeap pq = new DAryMinHeap(10, 3);

		// Insert elements with different costs
		pq.insert(0, 5.0);
		pq.insert(1, 3.0);
		pq.insert(2, 7.0);
		pq.insert(3, 1.0);

		// Get the iterator
		DAryMinHeap.IntIterator it = pq.iterator();

		// Track visited nodes
		boolean[] seen = new boolean[4];
		int count = 0;

		while (it.hasNext()) {
			int node = it.next();
			Assertions.assertTrue(node >= 0 && node < 4, "Unexpected node value: " + node);
			Assertions.assertFalse(seen[node], "Duplicate node returned by iterator: " + node);
			seen[node] = true;
			count++;
		}

		// Ensure all inserted nodes were visited
		for (int i = 0; i < 4; i++) {
			Assertions.assertTrue(seen[i], "Node " + i + " was not returned by the iterator.");
		}

		Assertions.assertEquals(4, count, "Iterator did not return all elements.");
	}

}
