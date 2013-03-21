Note: If you are not using JDK 7 and have manually included the JSR-166 libraries, you will have to override the default Java core classes.
 Otherwise you will encounter the following error: java.lang.SecurityException: Prohibited package name: java.util.concurrent To prevent this, 
 setup your JVM to override the classes by using the following argument: -Xbootclasspath/p:lib/jsr166.jar 
I have used the “lib/jsr166.jar” value because the JAR resides in a folder named “lib” inside my Eclipse project. 