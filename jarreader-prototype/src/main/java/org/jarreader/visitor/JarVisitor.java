package org.jarreader.visitor;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.InstructionList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Abstract Visitor class for traversing JAR file. This visitor wraps the file operation
 * on JAR file.
 */
public abstract class JarVisitor extends EmptyVisitor {

  private final Path absoluteJarPath;

  /**
   * Constructor for JAR visitor.
   *
   * @param jarPath   Relative or absolute file path
   */
  public JarVisitor(final Path jarPath) {
    absoluteJarPath = jarPath.toAbsolutePath();
  }

  /**
   * Start visitor and start traversing JAR file. Starting this visitor automatically
   * opens JAR file.
   *
   * @return    Reference to self
   */
  public JarVisitor start() {
    try (final JarFile jar = new JarFile(absoluteJarPath.toFile())) {
      visitJarFile(jar);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return this;
  }

  /**
   * Visit JAR file. Iterate over Java class files.
   *
   * @param jar   JAR file to visit
   */
  public void visitJarFile(final JarFile jar) {
    jar.stream()
       .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
       .forEach(this::visitJarEntry);
  }

  /**
   * Visit entry in JAR file. Parse class files into BCEL representation.
   *
   * @param entry   JAR entry to visit
   */
  public void visitJarEntry(final JarEntry entry) {
    try {
      ClassParser parser = new ClassParser(absoluteJarPath.toString(), entry.getName());
      JavaClass javaClass = parser.parse();

      javaClass.accept(this);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Visit Java class.
   *
   * @param javaClass   BCEL representation of class to visit
   */
  @Override
  public abstract void visitJavaClass(final JavaClass javaClass);

  /**
   * Visit fields
   *
   * @param classFields   BCEL representation of fields to visit
   */
  public void visitFields(final Field[] classFields) {}

  /**
   * Visit constructors
   *
   * @param classConstructors  BCEL representation of constructors to visit
   */
  public void visitConstructors(final Method[] classConstructors) {}

  /**
   * Visit methods
   *
   * @param classMethods  BCEL representation of methods to visit
   */
  public void visitMethods(final Method[] classMethods) {}

  /**
   * Visit list of instructions
   *
   * @param instructions  BCEL representation of method's JVM instructions
   */
  public void visitInstructionList(final InstructionList instructions) {}

  /**
   * Get information retrieved from JAR file.
   *
   * @return    Code information from JAR
   */
  public abstract String jarToString();
}
