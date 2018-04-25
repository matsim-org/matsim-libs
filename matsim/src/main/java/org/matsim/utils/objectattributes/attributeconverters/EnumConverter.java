package org.matsim.utils.objectattributes.attributeconverters;

import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * {@link AttributeConverter} for enum types.
 * @author thibautd
 */
public class EnumConverter<E extends Enum<E>> implements AttributeConverter<E> {
	private final Class<E> clazz;

	public EnumConverter( final Class<E> clazz ) {
		this.clazz = clazz;
	}

	@Override
	public E convert( final String value ) {
		return Enum.valueOf( clazz , value );
	}

	@Override
	public String convertToString( final Object o ) {
		if (o.getClass() != clazz) throw new IllegalArgumentException( "got "+o.getClass().getCanonicalName()+", expected "+clazz.getCanonicalName() );
		return ((Enum) o).name();
	}
}
