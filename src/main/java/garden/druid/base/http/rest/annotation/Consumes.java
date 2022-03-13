package garden.druid.base.http.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import garden.druid.base.http.rest.enums.ConsumerTypes;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Consumes {
	ConsumerTypes consumer() default ConsumerTypes.NONE;
}
