/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractView.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.view.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import playground.johannes.plans.ModCount;

/**
 * @author illenberger
 *
 */
public abstract class AbstractView <T extends ModCount> {

	protected T delegate;
	
	protected long delegateVersion;
	
	protected AbstractView(T delegate) {
		this.delegate = delegate;
		delegateVersion = delegate.getModCount() - 1;
	}
	
	protected void synchronize() {
		if(delegate.getModCount() > delegateVersion) {
			update();
			delegateVersion = delegate.getModCount();
		}
	}
	
	protected abstract void update();
	
	public T getDelegate() {
		return delegate;
	}
	
	protected <S, V extends AbstractView<?>> Collection<S>  synchronizeCollections(Collection<S> source, Collection<V> target) {
		List<V> removedElements = new LinkedList<V>();
		List<S> newElements = new LinkedList<S>();
		
		removedElements.addAll(target);
		for(S e : source) {
//			if(removedElements.contains(e))
			boolean contains = false;
			V element = null;
			for(V e2 : removedElements) {
				if(e2.getDelegate().equals(e)) {
					contains = true;
					element = e2;
					break;
				}
					
			}
			if(contains)
				removedElements.remove(element);
			else
				newElements.add(e);
		}
		
		target.removeAll(removedElements);
		
		return newElements;
	}
}
