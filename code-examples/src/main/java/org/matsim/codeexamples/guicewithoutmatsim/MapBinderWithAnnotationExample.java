package org.matsim.codeexamples.guicewithoutmatsim;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.annotation.Annotation;
import java.util.*;

public final class MapBinderWithAnnotationExample{
	private static final Logger log = LogManager.getLogger( MapBinderWithAnnotationExample.class ) ;

	public static void main ( String [] args ) {
		new MapBinderWithAnnotationExample().run() ;
	}

//	@Inject Map<Annotation, Set<Provider<MyInterface>>> map ;

	void run() {
		List<com.google.inject.Module> modules = new ArrayList<>() ;
		// (there is also a Module in java.lang, and maven gets confused about that.  kai, jul'19)

		modules.add(  new AbstractModule(){
			@Override
			protected void configure(){
				MapBinder<Annotation, MyInterface> mapBinder = MapBinder.newMapBinder( this.binder(), Annotation.class, MyInterface.class );
				mapBinder.permitDuplicates() ;
				mapBinder.addBinding( Names.named("abc") ).to( MyImpl1.class ) ;
				mapBinder.addBinding(Names.named("abc") ).to( MyImpl2.class ) ;

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

		Map<Annotation, Set<Provider<MyInterface>>> map = injector.getInstance( Key.get( new TypeLiteral<Map<Annotation, Set<Provider<MyInterface>>>>(){} ) );;
		Set<Provider<MyInterface>> set = map.get( Names.named("abc" ) ) ;

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
