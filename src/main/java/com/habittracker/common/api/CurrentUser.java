package com.habittracker.common.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the calling user's id into a controller method parameter of type
 * {@code UUID}. Where the id comes from depends on the auth mode: the signed-in
 * Google account (the {@code google} profile), or the {@code X-User-Id} header
 * (local development and tests). Controllers never read identity themselves.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
