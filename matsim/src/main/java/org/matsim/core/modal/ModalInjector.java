package org.matsim.core.modal;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public interface ModalInjector {

	<T> T get(Class<T> type);

	<T> T get(Key<T> key);

	<T> T get(TypeLiteral<T> typeLiteral);

	<T> T getModal(Class<T> type);

	<T> T getModal(TypeLiteral<T> typeLiteral);

	<T> T getNamed(Class<T> type, String name);

	<T> T getNamed(TypeLiteral<T> typeLiteral, String name);
}
