package com.framework.core.retry;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * TestNG listener that automatically applies {@link RetryAnalyzer}
 * to every test method — no per-test annotation needed.
 *
 * <p>Register in testng.xml:
 * <pre>{@code
 * <listener class-name="com.framework.core.retry.RetryListener"/>
 * }</pre>
 *
 * <p>Note: {@link IAnnotationTransformer} declares raw types in its contract
 * ({@code Class}, {@code Constructor}, {@code Method}). The {@code @SuppressWarnings}
 * below suppresses the resulting unchecked-cast compiler warning; this cannot be
 * fixed without changing the TestNG interface itself.
 */
@SuppressWarnings("rawtypes")
public class RetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
