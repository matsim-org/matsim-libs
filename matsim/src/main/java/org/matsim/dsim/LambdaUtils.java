package org.matsim.dsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Message;

import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used from <a href="https://gist.github.com/alexengrig/df1797d4d07c9f5d521d8c33c2a56563">This Gist</a>
 */
public class LambdaUtils {

	private static final Logger log = LogManager.getLogger(LambdaUtils.class);

	public static Consumer<? extends Message> createConsumer(Object lp, Class<?> msgType, String target) throws LambdaConversionException, ReflectiveOperationException {

		Class<?> clazz = lp.getClass();
		if (clazz.isSynthetic() && !clazz.isLocalClass() && !clazz.isAnonymousClass() && clazz.getDeclaredMethods().length == 1) {

			log.warn("Lambda functions as event handler currently only supported via reflection (slow): {}", clazz);

			Method method = clazz.getDeclaredMethods()[0];
			return message -> {
				try {
					method.invoke(lp, message);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			};
		}

		Method consumerMethod = clazz.getMethod(target, msgType);
		Function<Object, Consumer<Message>> consumerFactory = createLambdaFactory(Consumer.class, consumerMethod);

		return consumerFactory.apply(lp);
	}

	public static <T, L> Function<T, L> createLambdaFactory(Class<? super L> lambdaType, Method implMethod) throws LambdaConversionException, ReflectiveOperationException {
		Method lambdaMethod = LambdaUtils.findLambdaMethod(lambdaType);
		MethodType lambdaMethodType = MethodType.methodType(lambdaMethod.getReturnType(), lambdaMethod.getParameterTypes());

		Class<?> implType = implMethod.getDeclaringClass();
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MethodType implMethodType = MethodType.methodType(implMethod.getReturnType(), implMethod.getParameterTypes());

		MethodHandle implMethodHandle = null;
		try {
			// Lookup the method directly at the class, if the class is not accessible, this will throw an IllegalAccessException
			implMethodHandle = lookup.findVirtual(implType, implMethod.getName(), implMethodType);
		} catch (IllegalAccessException e) {
			for (Class<?> i : implType.getInterfaces()) {
				try {
					implMethodHandle = lookup.findVirtual(i, implMethod.getName(), implMethodType);
					break;
				} catch (NoSuchMethodException ex) {
					// Ignore
				}
			}
		}

		if (implMethodHandle == null) {
			throw new NoSuchMethodException("Method not found or not accessible: " + implMethod);
		}

		MethodType invokedMethodType = MethodType.methodType(lambdaType, implType);

		CallSite metafactory = LambdaMetafactory.metafactory(
			lookup,
			lambdaMethod.getName(), invokedMethodType, lambdaMethodType,
			implMethodHandle, implMethodType);

		MethodHandle factory = metafactory.getTarget();
		return instance -> {
			try {
				@SuppressWarnings("unchecked")
				L lambda = (L) factory.invoke(instance);
				return lambda;
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		};
	}

	public static Method findLambdaMethod(Class<?> type) {
		if (!type.isInterface()) {
			throw new IllegalArgumentException("This must be interface: " + type);
		}
		Method[] methods = getAllMethods(type);
		if (methods.length == 0) {
			throw new IllegalArgumentException("No methods in: " + type.getName());
		}
		Method targetMethod = null;
		for (Method method : methods) {
			if (isInterfaceMethod(method)) {
				if (targetMethod != null) {
					throw new IllegalArgumentException("This isn't functional interface: " + type.getName());
				}
				targetMethod = method;
			}
		}
		if (targetMethod == null) {
			throw new IllegalArgumentException("No method in: " + type.getName());
		}
		return targetMethod;
	}

	public static Method[] getAllMethods(Class<?> type) {
		LinkedList<Method> result = new LinkedList<>();
		Class<?> current = type;
		do {
			result.addAll(Arrays.asList(current.getMethods()));
		} while ((current = current.getSuperclass()) != null);
		return result.toArray(new Method[0]);
	}

	public static boolean isInterfaceMethod(Method method) {
		return !method.isDefault() && Modifier.isAbstract(method.getModifiers());
	}

}
