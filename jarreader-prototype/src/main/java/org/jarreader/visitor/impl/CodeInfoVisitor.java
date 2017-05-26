package org.jarreader.visitor.impl;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.jarreader.visitor.JarVisitor;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Print class information with Apache Commons BCEL. Because BCEL represents the class
 * as the real class file structure, it's able to extract more internals from it.
 * <p>
 * The following are extracted:
 * <ul>
 * <li>Major and minor version of class
 * <li>Name of source file it was compiled from
 * <li>Constant pool
 * <li>Package name
 * <li>Class name
 * <li>Extended superclass name
 * <li>Implemented interfaces
 * <li>Inner classes
 * <li>Declared constructors
 * <li>Declared fields
 * <li>Declared qualified method signatures
 * </ul>
 */
public class CodeInfoVisitor extends JarVisitor {

  private StringBuilder codeInfoBuilder;

  public CodeInfoVisitor(final Path jarPath) {
    super(jarPath);
    codeInfoBuilder = new StringBuilder();
  }

  /**
   * Print class information.
   *
   * @param javaClass   BCEL representation of class to visit
   */
  @Override
  public void visitJavaClass(final JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    String superClass = javaClass.getSuperclassName();
    String[] interfaces = javaClass.getInterfaceNames();

    // Start printing
    codeInfoBuilder.append("\n================================");

    // Class meta information
    codeInfoBuilder.append("\nMajor version: ").append(javaClass.getMajor());
    codeInfoBuilder.append("\nMinor version: ").append(javaClass.getMinor());
    codeInfoBuilder.append("\nOriginal source file: ").append(javaClass.getSourceFileName());

    // Constant pool
    codeInfoBuilder.append("\nConstant pool:\n")
                   .append(javaClass.getConstantPool());

    // Package
    if (packageName.isEmpty()) {
      codeInfoBuilder.append("\nPackage: ").append(javaClass.getPackageName());
    }

    // Class
    codeInfoBuilder.append("\nClass: ").append(javaClass.getClassName());

    // Superclass
    if (!superClass.equals("java.lang.Object")) {
      codeInfoBuilder.append("\nExtended superclass: ").append(superClass);
    }

    // Interfaces
    if (0 < interfaces.length) {
      codeInfoBuilder.append("\nImplemented interfaces:");
      for (String iface : interfaces) {
        codeInfoBuilder.append("\n\t").append(iface);
      }
    }

    // Fields
    visitFields(javaClass.getFields());

    // Constructors
    visitConstructors(javaClass.getMethods());

    // Methods
    visitMethods(javaClass.getMethods());
  }

  /**
   * Iterate over fields and print information about them.
   *
   * @param classFields   BCEL representation of fields to visit
   */
  @Override
  public void visitFields(final Field[] classFields) {
    if (0 < classFields.length) {
      codeInfoBuilder.append("\nFields:");
      for (Field field : classFields) {
        field.accept(this);
      }
    }
  }

  /**
   * Print field information.
   *
   * @param field   BCEL representation of field
   */
  @Override
  public void visitField(final Field field) {
    codeInfoBuilder.append("\n\t")
        .append("(0x")
        .append(Integer.toHexString(field.getModifiers()))
        .append(") ")
        .append(field);
  }

  /**
   * Iterate over constructors and print information about them.
   *
   * @param classConstructors  BCEL representation of constructors to visit
   */
  @Override
  public void visitConstructors(final Method[] classConstructors) {
    Method[] constructors =
        Arrays.stream(classConstructors)
            .filter(m -> m.getName().equals("<init>"))
            .toArray(Method[]::new);

    if (0 < constructors.length) {
      codeInfoBuilder.append("\nConstructors:");
      for (Method constructor : constructors) {
        constructor.accept(this);
      }
    }
  }

  /**
   * Iterate over declared methods and print information about them.
   * This method excludes constructors.
   *
   * @param classMethods  BCEL representation of methods to visit
   */
  public void visitMethods(final Method[] classMethods) {
    Method[] methods =
        Arrays.stream(classMethods)
            .filter(m -> !m.getName().equals("<init>"))
            .toArray(Method[]::new);

    if (0 < methods.length) {
      codeInfoBuilder.append("\nMethods:");
      for (Method method : methods) {
        method.accept(this);
      }
    }
  }

  /**
   * Print method information.
   *
   * @param method  BCEL representation of method to visit
   */
  @Override
  public void visitMethod(final Method method) {
    codeInfoBuilder.append("\n\t")
        .append("(0x")
        .append(Integer.toHexString(method.getModifiers()))
        .append(") ")
        .append(method);
  }

  /**
   * Return textual code information about all classes in JAR file.
   *
   * @return  Textual representation of JAR file code information
   */
  @Override
  public String jarToString() {
    return codeInfoBuilder.toString();
  }
}
