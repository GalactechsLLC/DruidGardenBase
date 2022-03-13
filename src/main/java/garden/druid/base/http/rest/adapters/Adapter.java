package garden.druid.base.http.rest.adapters;

@FunctionalInterface
public interface Adapter<T> {
	T convert(String input);
}
