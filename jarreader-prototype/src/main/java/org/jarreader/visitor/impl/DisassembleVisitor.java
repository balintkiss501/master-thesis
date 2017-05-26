package org.jarreader.visitor.impl;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.jarreader.visitor.JarVisitor;

import java.lang.Deprecated;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Visitor for disassembling classes in JAR. Class is represented as compilable Java source code,
 * but instructions are shown as commented out JVM Assembly code, recreating the functionality of
 * javap utility.
 */
public class DisassembleVisitor extends JarVisitor {

  // Regular expression patterns to strip package or class names
  private final static String STRIP_PACKAGE_NAMES_REGEXP = ".*(\\.)";
  private final static String MATCH_INNER_CLASS_TYPE_REGEXP = ".*(\\$)";

  private JavaClass currentClass;
  private ConstantPool constantPool;
  private StringBuilder codePrintBuilder;

  public DisassembleVisitor(final Path jarPath) {
    super(jarPath);
    codePrintBuilder = new StringBuilder();
  }

  /**
   * Disassemble class and print structure as compilable source.
   *
   * @param javaClass   BCEL representation of class to visit
   */
  @Override
  public void visitJavaClass(final JavaClass javaClass) {
    currentClass = javaClass;
    constantPool = currentClass.getConstantPool();

    codePrintBuilder.append("================================\n");

    // Package
    String packageName = currentClass.getPackageName();
    if (!packageName.isEmpty()) {
      codePrintBuilder.append("package ")
          .append(packageName)
          .append(";\n\n");
    }

    // Class
    if (currentClass.isClass()) {
      codePrintBuilder.append("public class ")
          .append(currentClass.getClassName().replaceAll(STRIP_PACKAGE_NAMES_REGEXP, ""));

      // Superclass name
      if (!currentClass.getSuperclassName().equals("java.lang.Object")) {
        codePrintBuilder.append(" extends ")
            .append(currentClass.getSuperclassName());
      }

      // Interface names
      String[] interfaceNames = currentClass.getInterfaceNames();
      if (0 < interfaceNames.length) {
        codePrintBuilder.append(" implements ");
        for (String interfaceName : interfaceNames) {
          codePrintBuilder.append(interfaceName);
        }
      }
      codePrintBuilder.append(" {\n");

      // Fields
      visitFields(currentClass.getFields());

      // Methods
      visitMethods(currentClass.getMethods());
    } else {
      // Interface
      codePrintBuilder.append("public interface ")
          .append(currentClass.getClassName().replaceAll(STRIP_PACKAGE_NAMES_REGEXP, ""))
          .append(" {\n");

      // Methods
      visitMethods(currentClass.getMethods());
    }
    codePrintBuilder.append("}\n");
  }

  /**
   * Print fields in source form.
   *
   * @param fields  BCEL representation of fields
   */
  @Override
  public void visitFields(final Field[] fields) {
    if (fields != null) {
      Field[] filteredFields = Arrays.stream(fields)
          .filter(f -> !f.isSynthetic())
          .toArray(org.apache.bcel.classfile.Field[]::new);

      if (0 < filteredFields.length) {
        for (Field field : filteredFields) {
          field.accept(this);
        }
        codePrintBuilder.append('\n');
      }
    }
  }

  /**
   * Print field in source form.
   *
   * @param field   BCEL representation of field
   */
  @Override
  public void visitField(final Field field) {
    codePrintBuilder.append('\t')
        .append(field.toString())
        .append(";\n");
  }

  /**
   * Print methods in source form.
   *
   * @param methods   BCEL representation of methods to visit
   */
  @Override
  public void visitMethods(final Method[] methods) {
    if (methods != null) {
      if (0 < methods.length) {
        for (int i = 0; i < methods.length; ++i) {
          methods[i].accept(this);

          if (i < methods.length - 1) {
            codePrintBuilder.append("\n\n");
          }
        }
        codePrintBuilder.append('\n');
      }
    }
  }

  /**
   * Print method in source form.
   *
   * @param method   BCEL representation of method to visit
   */
  @Override
  public void visitMethod(final Method method) {
    MethodGen methodG = new MethodGen(method, currentClass.getClassName(), new ConstantPoolGen(constantPool));

    // Is this a constructor?
    if (methodG.getName().equals("<init>")) {
      // Change name "<init>" to name of the class
      String constructorName = currentClass.getClassName().substring(currentClass.getClassName().lastIndexOf('.') + 1);
      methodG.setName(constructorName);

      codePrintBuilder.append("\t");

      // Remove "void" return type from constructor signature
      codePrintBuilder.append(methodG.toString().replaceFirst("void ", ""));
    }
    // Use regular method otherwise
    else {
      codePrintBuilder.append('\t')
                      .append(methodG);
    }

    // Check if method is abstract
    if (methodG.isAbstract()) {
      codePrintBuilder.append(";\n");
    } else {
      codePrintBuilder.append(" {\n");
      //method.getCode().accept(this);
      visitInstructionList(methodG.getInstructionList());
      codePrintBuilder.append("\t}");
    }
  }

  /**
   * Disassemble bytecode as commented out JVM instructions.
   * Use this instead of visitCode().
   *
   * @param instructions    Generic BCEL representation of method instructions.
   */
  @Override
  public void visitInstructionList(final InstructionList instructions) {
    // Initial instruction iterator
    InstructionHandle ihandle = instructions.getStart();

    // Iterate over instructions
    while (ihandle != null) {
      Instruction instruction = ihandle.getInstruction();

      // If instruction uses index to the constant pool, get the
      // name of the symbolic constant. Otherwise leave it empty.
      String constantPoolReference = "";
      if (instruction instanceof CPInstruction) {
        int poolIndex = ((CPInstruction) instruction).getIndex();
        Constant constant = constantPool.getConstant(poolIndex);
        String constantName = constantPool.constantToString(constant);

        switch (instruction.getOpcode()) {
          case Const.NEWARRAY:   /* fallthrough */  // Primitive array
          case Const.NEW:        /* fallthrough */  // Reference to classes
          case Const.CHECKCAST:  /* fallthrough */
          case Const.INSTANCEOF: /* fallthrough */
          case Const.ANEWARRAY:  /* fallthrough */  // Array containing reference types
          case Const.MULTIANEWARRAY:                // Multidimensional reference array
            constantPoolReference = "<" + constantName + ">";
            break;
          default:
            constantPoolReference = constantName;
        }
      }

      // Return disassembled output in
      // "// [instruction index] [instruction name] [optional constant pool reference]" form
      String disassemblyOutput = String.format("\t\t// %1$-4d: %2$-20s %3$s\n",
                                               ihandle.getPosition(),
                                               instruction.getName(),
                                               constantPoolReference);
      codePrintBuilder.append(disassemblyOutput);

      // Move iterator
      ihandle = ihandle.getNext();
    }
  }

  /**
   * Visit Code attribute of method.
   *
   * @param code    BCEL representation of Code attribute
   * @deprecated use visitInstructionList() instead
   */
  @Deprecated
  @Override
  public void visitCode(final Code code) {
    String[] instructions = code.toString(false).split("\n");

    for (int i = 1; i < instructions.length - 4; ++i) {
      if (instructions[i].equals("0:    return")) {
        codePrintBuilder.append('\n');
        break;
      }

      if (instructions[i].isEmpty()) {
        break;
      }

      codePrintBuilder.append("\t\t// ")
          .append(instructions[i])
          .append('\n');
    }
  }

  /**
   * Return textual disassembled source about all classes in JAR file.
   *
   * @return  Textual representation of JAR file disassembled source
   */
  @Override
  public String jarToString() {
    return codePrintBuilder.toString();
  }
}
