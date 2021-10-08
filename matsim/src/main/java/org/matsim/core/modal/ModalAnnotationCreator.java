/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.modal;

import java.lang.annotation.Annotation;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface ModalAnnotationCreator<M extends Annotation> {
	M mode(String mode);

	default <T> Key<T> key(Class<T> type, String mode) {
		return Key.get(type, mode(mode));
	}

	default <T> Key<T> key(TypeLiteral<T> typeLiteral, String mode) {
		return Key.get(typeLiteral, mode(mode));
	}
}
