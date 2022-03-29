package kaktusz.geopolitika.util;

import java.lang.reflect.Field;

public class ReflectionUtils {

	@SuppressWarnings("unchecked")
	public static <T, U> T getPrivateField(U object, Class<? super U> accessSuperclass, String fieldName) throws IllegalAccessException, NoSuchFieldException {
		Field field = accessSuperclass.getDeclaredField(fieldName);
		field.setAccessible(true);
		return  (T) field.get(object);
	}
}
