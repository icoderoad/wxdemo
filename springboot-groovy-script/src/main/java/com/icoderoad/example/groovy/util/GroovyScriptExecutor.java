package com.icoderoad.example.groovy.util;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

public class GroovyScriptExecutor {

    public static String executeScript(String scriptName, String methodName) {
        GroovyClassLoader loader = new GroovyClassLoader();
        try {
            Class groovyClass = loader.parseClass(
                GroovyScriptExecutor.class.getClassLoader().getResourceAsStream("scripts/" + scriptName), methodName
            );

            GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
            return (String) groovyObject.invokeMethod(methodName, null);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing script";
        }
    }
}