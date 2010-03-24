/**
 * 
 */
package org.matsim.vis.otfvis.data;

import org.matsim.core.utils.collections.QuadTree.Executor;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class ClassCountExecutor implements Executor<OTFDataReader> {
	private final Class<?> targetClass;
	private int count = 0;

	public ClassCountExecutor(final Class<?> clazz) {
		this.targetClass = clazz;
	}

	public int getCount() {
		return this.count;
	}

	public void execute(final double x, final double y, final OTFDataReader reader) {
		if (this.targetClass.isAssignableFrom(reader.getClass())) this.count++;
	}
}