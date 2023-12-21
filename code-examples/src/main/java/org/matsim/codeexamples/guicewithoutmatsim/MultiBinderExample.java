package org.matsim.codeexamples.guicewithoutmatsim;

import java.util.*;

import com.google.inject.name.Named;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public final class MultiBinderExample{
	private static final Logger log = LogManager.getLogger( MultiBinderExample.class ) ;

	public static void main ( String [] args ) {
		new MultiBinderExample().run() ;
	}

	void run() {
		List<Module> modules = new ArrayList<>() ;
		modules.add(  new AbstractModule(){
			@Override
			protected void configure(){
				bind( MyRunner.class );

				Multibinder<MyInterface> multiBinder = Multibinder.newSetBinder( this.binder(), MyInterface.class );;
//				Multibinder<MyInterface> multiBinder = Multibinder.newSetBinder( this.binder(), MyInterface.class, Names.named( "someAnnotation" ) );;
				multiBinder.addBinding().to( MyImpl1.class ) ;
				multiBinder.addBinding().to( MyImpl2.class ) ;

//				multiBinder.addBinding().to( MyImpl2.class ) ;

//				multiBinder.permitDuplicates() ;

			}
		} ) ;


		modules.add( new AbstractModule(){
			@Override protected void configure(){
				Multibinder<MyInterface> multibinder = Multibinder.newSetBinder( this.binder(), MyInterface.class );
				multibinder.addBinding().to( MyImpl3.class );
			}
		} );

		Injector injector = Guice.createInjector( modules );

		Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

		for( Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet() ){
			log.info("") ;
			log.info( "key=" + entry.getKey() ) ;
			log.info( "value=" + entry.getValue() ) ;
		}
		log.info("") ;

		MyRunner myRunner = injector.getInstance( MyRunner.class );
		myRunner.run();

//		Collection<Provider<MyInterface>> set = injector.getInstance( Key.get( new TypeLiteral<Collection<Provider<MyInterface>>>(){} , Names.named( "someAnnotation" )) );
//		for( Provider<MyInterface> provider : set ){
//			provider.get() ;
//		}

	}

	private static class MyRunner {
		@Inject
//		@Named("someAnnotation")
		private Set<MyInterface> myInterfaces;
		void run() {
			for( MyInterface myInterface : myInterfaces ){
				myInterface.doSomething();
			}
		}
	}

	private interface MyInterface{
		void doSomething();
	}

	private static class MyImpl1 implements MyInterface{
		public void doSomething() {
			log.warn("calling doSomething method of MyImpl1");
		}
	}

	private static class MyImpl2 implements MyInterface{
		public void doSomething() {
			log.warn("calling doSomething method of MyImpl2");
		}
	}
	private static class MyImpl3 implements MyInterface{
		public void doSomething() {
			log.warn("calling doSomething method of MyImpl3");
		}
	}
}
