package org.matsim.codeexamples.guicewithoutmatsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

				Multibinder<MyInterface> multiBinder = Multibinder.newSetBinder( this.binder(), MyInterface.class, Names.named( "someAnnotation" ) );;
//				Multibinder<MyInterface> multiBinder = Multibinder.newSetBinder( this.binder(), MyInterface.class );;
				multiBinder.permitDuplicates() ;
				multiBinder.addBinding().to( MyImpl1.class ) ;
				multiBinder.addBinding().to( MyImpl2.class ) ;

			}
		} ) ;
		Injector injector = Guice.createInjector( modules );

		Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

		for( Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet() ){
			log.info("") ;
			log.info( "key=" + entry.getKey() ) ;
			log.info( "value=" + entry.getValue() ) ;
		}
		log.info("") ;

		Collection<Provider<MyInterface>> set = injector.getInstance( Key.get( new TypeLiteral<Collection<Provider<MyInterface>>>(){} , Names.named( "someAnnotation" )) );

		for( Provider<MyInterface> provider : set ){
			provider.get() ;
		}

	}

	private interface MyInterface{

	}

	private static class MyImpl1 implements MyInterface{
		@Inject MyImpl1() {
			log.info( "ctor 1 called" );
		}
	}

	private static class MyImpl2 implements MyInterface{
		@Inject MyImpl2() {
			log.info( "ctor 2 called" );
		}
	}
}
