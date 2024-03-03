package de.tudbut.mod.client.ttcp.utils.category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author TudbuT
 * @since 17 Mar 2022
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Category
public @interface Chat {

}
