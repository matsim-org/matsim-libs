package org.matsim.core.modal;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;

final class ModalInjectorImpl implements ModalInjector {
	private final ModalProviders.InstanceGetter<? extends Annotation> getter;

	public <M extends Annotation> ModalInjectorImpl(ModalProviders.InstanceGetter<M> getter) {
		this.getter = getter;
	}

	public <T> T get(Class<T> type) {
		return getter.get(type);
	}

	public <T> T get(Key<T> key) {
		return getter.get(key);
	}

	public <T> T get(TypeLiteral<T> typeLiteral) {
		return getter.get(typeLiteral);
	}

	public <T> T getModal(Class<T> type) {
		return getter.getModal(type);
	}

	public <T> T getModal(TypeLiteral<T> typeLiteral) {
		return getter.getModal(typeLiteral);
	}

	public <T> T getNamed(Class<T> type, String name) {
		return getter.getNamed(type, name);
	}

	public <T> T getNamed(TypeLiteral<T> typeLiteral, String name) {
		return getter.getNamed(typeLiteral, name);
	}
}
