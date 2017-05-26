package org.jarreader.visitor.impl;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.jarreader.visitor.JarVisitor;

import java.nio.file.Path;
import java.util.*;

/**
 * Visitor for retrieving method caller and callee information.
 */
public class MethodCallInfoVisitor extends JarVisitor {

  private Map<String, MethodCallNode> methodCallMap;

  public MethodCallInfoVisitor(final Path jarPath) {
    super(jarPath);
    methodCallMap = new LinkedHashMap<>();
  }

  /**
   * Start collecting methods then connecting callers and callees.
   *
   * @return  Reference to self
   */
  @Override
  public JarVisitor start() {
    super.start();
    connectMethods();

    return this;
  }

  /**
   * Collect methods of a class.
   *
   * @param javaClass   BCEL representation of class to visit
   */
  @Override
  public void visitJavaClass(final JavaClass javaClass) {
    for (Method method : javaClass.getMethods()) {
      if (method.isAbstract() || method.isNative()) {
        continue;
      }

      String qualifiedName = javaClass.getClassName() + '.' + method.getName();
      if (!methodCallMap.containsKey(qualifiedName)) {
        methodCallMap.put(qualifiedName, new MethodCallNode(javaClass, method));
      }
    }
  }

  /**
   * Examine bytecode instructions and connect methods in method collection.
   */
  private void connectMethods() {
    // Size of map might grow with methods not in JAR file, keep track of map keys
    List<String> keyList = new ArrayList<>(methodCallMap.keySet());
    for (int i = 0; i < keyList.size(); i++) {
      MethodCallNode initialNode = methodCallMap.get(keyList.get(i));
      if (initialNode.isInJar()) {
        MethodGen method = initialNode.getMethod().get();
        ConstantPoolGen methodConstantPool = method.getConstantPool();

        // Iterate through bytecode instructions
        InstructionHandle ihandle = method.getInstructionList().getStart();
        while (ihandle != null) {
          Instruction instruction = ihandle.getInstruction();
          switch (instruction.getOpcode()) {
            case Const.INVOKEINTERFACE:
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
            case Const.INVOKEVIRTUAL:
              // Retrieve called method name
              InvokeInstruction invokeInstruction = (InvokeInstruction) instruction;
              String calleeName =
                  invokeInstruction.getReferenceType(methodConstantPool) + "." + invokeInstruction.getMethodName(methodConstantPool);

              MethodCallNode calleeNode;
              // Create new node if it not exists yet
              if (!methodCallMap.containsKey(calleeName)) {
                calleeNode = new MethodCallNode(calleeName);
                methodCallMap.put(calleeName, calleeNode);
                keyList = new ArrayList<>(methodCallMap.keySet());
              } else {
                calleeNode = methodCallMap.get(calleeName);
              }

              // Connect two methods
              calleeNode.addCaller(initialNode);
              initialNode.addCallee(calleeNode);
            default:
              break;
          }
          // Move iterator
          ihandle = ihandle.getNext();
        }
      }
    }
  }

  /**
   * Print method caller and callee information.
   *
   * @return  Textual representation of connected methods
   */
  @Override
  public String jarToString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, MethodCallNode> entry : methodCallMap.entrySet()) {
      MethodCallNode methodNode = entry.getValue();

      sb.append("================================\n");
      sb.append("Method name:\t");
      sb.append(methodNode.getName());
      sb.append("\nCallers:\n");
      methodNode.getCallers().forEach(caller -> {
        sb.append('\t');
        sb.append(caller.getName());
        sb.append('\n');
      });
      sb.append("Callees:\n");
      methodNode.getCallees().forEach(callee -> {
        sb.append('\t');
        sb.append(callee.getName());
        sb.append('\n');
      });
    }
    return sb.append('\n').toString();
  }

  /**
   * Methodcall node containing method name and references to its callers and callees.
   */
  private class MethodCallNode {
    private Optional<MethodGen> method;
    private String name;
    private List<MethodCallNode> callers;
    private List<MethodCallNode> callees;

    /**
     * Create method node that was in JAR.
     *
     * @param javaClass
     * @param method
     */
    MethodCallNode(final JavaClass javaClass, final Method method) {
      this.method = Optional.of(new MethodGen(method, javaClass.getClassName(),
          new ConstantPoolGen(javaClass.getConstantPool())));

      this.name = javaClass.getClassName() + '.' + method.getName();
      this.callers = new ArrayList<>();
      this.callees = new ArrayList<>();
    }

    /**
     * Create method node that is not in JAR, but referenced from
     * an outside library.
     *
     * @param methodName
     */
    MethodCallNode(final String methodName) {
      this.method = Optional.empty();

      this.name = methodName;
      this.callers = new ArrayList<>();
      this.callees = new ArrayList<>();
    }

    void addCaller(final MethodCallNode caller) {
      callers.add(caller);
    }

    void addCallee(final MethodCallNode callee) {
      callees.add(callee);
    }

    boolean isInJar() {
      return method.isPresent();
    }

    Optional<MethodGen> getMethod() {
      return method;
    }

    String getName() {
      return name;
    }

    List<MethodCallNode> getCallers() {
      return callers;
    }

    List<MethodCallNode> getCallees() {
      return callees;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("{Method: ")
          .append(name)
          .append(", callers: [");
      for (int i = 0; i < callers.size(); i++) {
        sb.append(callers.get(i).getName());

        if (i < callers.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append("], callees: [");
      for (int i = 0; i < callees.size(); i++) {
        sb.append(callees.get(i).getName());

        if (i < callees.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append("]}");

      return sb.toString();
    }
  }
}
