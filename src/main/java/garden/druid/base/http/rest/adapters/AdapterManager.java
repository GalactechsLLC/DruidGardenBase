package garden.druid.base.http.rest.adapters;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AdapterManager {

	private static final ConcurrentHashMap<Class<?>, Adapter<?>> classAdapters = new ConcurrentHashMap<>();
	private static final Gson gson = new GsonBuilder().serializeNulls().create();
	
	{
		register(Boolean.class, s -> s == null ? null : Boolean.parseBoolean(s));
		register(Byte.class, s -> s == null ? null : Byte.parseByte(s));
		register(Short.class, s -> s == null ? null : Short.parseShort(s));
		register(Integer.class, s -> s == null ? null : Integer.parseInt(s));
		register(Long.class, s -> s == null ? null : Long.parseLong(s));
		register(Float.class, s -> s == null ? null : Float.parseFloat(s));
		register(Double.class, s -> s == null ? null : Double.parseDouble(s));
		register(String.class, s -> s);
		
		register(Boolean.TYPE, Boolean::parseBoolean);
		register(Byte.TYPE, Byte::parseByte);
		register(Short.TYPE, Short::parseShort);
		register(Integer.TYPE, Integer::parseInt);
		register(Long.TYPE, Long::parseLong);
		register(Float.TYPE, Float::parseFloat);
		register(Double.TYPE, Double::parseDouble);
		
	}
	
	public <T> void register(Class<T> cls, Adapter<T> adapter) {
		classAdapters.put(cls, adapter);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Adapter<T> getAdapter(Class<T> cls) {
		Adapter<T> rtn = (Adapter<T>) classAdapters.get(cls);
		if(rtn == null) {
			rtn = (s) -> {
				if(s == null) return null;
				T innerRtn;
				//first try parsing as json
				try {
					innerRtn = gson.fromJson(s, cls);
					if(innerRtn != null) {
						return innerRtn;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				//Then from the class constructor3
				try {
					return cls.getConstructor(String.class).newInstance(s);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				//Lastly try parsing as xml
				try {
					JAXBContext jc = JAXBContext.newInstance(cls);
				    Unmarshaller u = jc.createUnmarshaller();
				    innerRtn = (T) u.unmarshal(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
					if(innerRtn != null) {
						return innerRtn;
					}
				} catch (JAXBException e) {
					e.printStackTrace();
				} 
				
				//Use the cast from the cls
				try {
					return cls.cast(s);
				} catch(ClassCastException e) {
					e.printStackTrace();
				}
				
				//Failed to convert it return null
				return null;
			};
		}
		return rtn;
	}
}
