package com.eventhub.tests.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * IAnnotationTransformer that wires RetryAnalyzer onto every smoke-tagged test
 * at suite startup — no @Test annotation changes needed in test classes.
 *
 * How it works:
 *   TestNG calls transform() for every @Test method before execution.
 *   If the method declares groups containing "smoke", setRetryAnalyzer() is called,
 *   which makes TestNG invoke RetryAnalyzer.retry() on any failure for that method.
 *
 * Registration: add this listener to testng-smoke.xml and testng-full.xml.
 */
public class RetryListener implements IAnnotationTransformer {

    private static final Logger log = LoggerFactory.getLogger(RetryListener.class);

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {

        boolean isSmokeTest = Arrays.asList(annotation.getGroups()).contains("smoke");

        if (isSmokeTest) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
            log.debug("[RetryListener] RetryAnalyzer applied to smoke test: {}",
                    testMethod != null ? testMethod.getName() : "unknown");
        }
    }
}
