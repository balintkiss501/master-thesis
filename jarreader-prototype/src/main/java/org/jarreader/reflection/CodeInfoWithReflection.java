package org.jarreader.reflection;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class to print class information with Java Reflection API.
 * <p>
 * The following members are extracted:
 * <ul>
 * <li>Package name
 * <li>Class name
 * <li>Extended superclass name
 * <li>Implemented interfaces
 * <li>Inner classes
 * <li>Declared constructors
 * <li>Declared fields
 * <li>Declared qualified method signatures
 * </ul>
 * <p>
 * A known caveat is that declared fields or methods are not sorted in the order
 * they are declared, but randomly. The purpose of this class is to illustrate
 * the use of reflection to extract code information.
 */
public final class CodeInfoWithReflection {

  /**
   * Extract code information about all classes from a JAR file.
   *
   * @param jarPath   Relative or absolute file path to JAR file
   * @return          Code information retrieved from classes in JAR
   * @throws NoClassDefFoundError   Class definition couldn't be found
   * @throws ClassNotFoundException Class itself couldn't be found
   */
  public static String readJar(final Path jarPath)
      throws NoClassDefFoundError, ClassNotFoundException {
    final StringBuilder sb = new StringBuilder();
    final Path absoluteJarPath = jarPath.toAbsolutePath();

    try (final JarFile jar = new JarFile(absoluteJarPath.toFile())) {
      for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
        final JarEntry entry = entries.nextElement();

        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
          sb.append(readClass(absoluteJarPath, entry));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return sb.toString();
  }

  /**
   * Read code information from class in JAR file.
   *
   * @param jarPath   Absolute path to JAR file
   * @param entry     Current JAR entry in iteration
   * @return          Code information retrieved from current class in JAR
   * @throws NoClassDefFoundError
   * @throws ClassNotFoundException
   */
  private static String readClass(final Path jarPath, final JarEntry entry)
      throws NoClassDefFoundError, ClassNotFoundException {

    // Get class name and cut off ".class" file extension
    final int CLASS_EXTENSION_LENGTH = 6;
    final String canonicalClassName =
      entry.getName()
           .substring(0, entry.getName().length() - CLASS_EXTENSION_LENGTH)
           .replace('/', '.');

    final StringBuilder sb = new StringBuilder();

    try {
      // Get class
      final URL[] urls = {new URL("jar:file:" + jarPath + "!/")};
      final URLClassLoader classLoader = URLClassLoader.newInstance(urls);
      final Class<?> clazz = classLoader.loadClass(canonicalClassName);

      // Get class information using reflection
      final Package classPackage = clazz.getPackage();
      final Class<?> superClass = clazz.getSuperclass();
      final Class<?>[] interfaces = clazz.getInterfaces();
      final Class<?>[] innerClasses = clazz.getDeclaredClasses();

      final Field[] fields = clazz.getDeclaredFields();
      final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      final Method[] methods = clazz.getDeclaredMethods();

      // Start printing
      sb.append("================================");

      // Package
      if (classPackage != null) {
        sb.append("\nPackage: ").append(clazz.getPackage().getName());
      }

      // Class
      sb.append("\nClass: ").append(clazz.getName());

      // Superclass
      if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
        sb.append("\nExtended superclass: ").append(superClass.getName());
      }

      // Intefaces
      if (0 < interfaces.length) {
        sb.append("\nImplemented interfaces:");
        for (final Class<?> iface : interfaces) {
          sb.append("\n\t").append(iface.getName());
        }
      }

      // Inner classes
      if (0 < innerClasses.length) {
        sb.append("\nInner classes:");
        for (final Class<?> innerClass : innerClasses) {
          sb.append("\n\t")
              .append("(0x")
              .append(Integer.toHexString(innerClass.getModifiers()))
              .append(") ")
              .append(' ')
              .append(Modifier.toString(innerClass.getModifiers()))
              .append(' ')
              .append(innerClass.getName());
        }
      }

      // Fields
      // Uncomment if you want to filter out synthetic fields
//            fields = Arrays.stream(fields)
//                    .filter(f -> !f.isSynthetic())
//                    .toArray(Field[]::new);

      if (0 < fields.length) {
        sb.append("\nFields:");
        for (final Field field : fields) {
          sb.append("\n\t")
              .append("(0x")
              .append(Integer.toHexString(field.getModifiers()))
              .append(") ")
              .append(Modifier.toString(field.getModifiers()))
              .append(' ')
              .append(field.getType().getSimpleName())
              .append(' ')
              .append(field.getName());
        }
      }

      // Constructors
      if (0 < constructors.length) {
        sb.append("\nConstructors:");
        for (final Constructor<?> constructor : constructors) {
          sb.append("\n\t")
              .append("(0x")
              .append(Integer.toHexString(constructor.getModifiers()))
              .append(") ")
              .append(Modifier.toString(constructor.getModifiers()))
              .append(' ')
              .append(constructor.getName())
              .append(printParameters(constructor.getParameters()));
        }
      }

      // Methods
      if (0 < methods.length) {
        sb.append("\nQualified method signatures:");
        for (final Method method : methods) {
          sb.append("\n\t")
              .append("(0x")
              .append(Integer.toHexString(method.getModifiers()))
              .append(") ")
              .append(Modifier.toString(method.getModifiers()))
              .append(' ')
              .append(method.getReturnType().getSimpleName())
              .append(' ')
              .append(canonicalClassName)
              .append('.')
              .append(method.getName())
              .append(printParameters(method.getParameters()));
        }
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return sb.append('\n').toString();
  }

  /**
   * Helper function for printing parameters extracted with reflection.
   *
   * @param params    Parameters or none
   * @return          Textual form that can be appended to method signature
   */
  private static String printParameters(final Parameter[] params) {
    final StringBuilder sb = new StringBuilder("(");

    for (int i = 0; i < params.length; ++i) {
      sb.append(Modifier.toString(params[i].getModifiers()))
          .append(params[i].getType().getSimpleName())
          .append(' ')
          .append(params[i].getName());

      if (i < params.length - 1) {
        sb.append(", ");
      }
    }
    sb.append(')');

    return sb.toString();
  }
}
