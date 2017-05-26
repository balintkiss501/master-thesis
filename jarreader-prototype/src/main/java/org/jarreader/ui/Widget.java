package org.jarreader.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jarreader.reflection.CodeInfoWithReflection;
import org.jarreader.visitor.JarVisitor;
import org.jarreader.visitor.impl.CodeInfoVisitor;
import org.jarreader.visitor.impl.DisassembleVisitor;
import org.jarreader.visitor.impl.MethodCallInfoVisitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * GUI frontend widget for JAR reader prototype. This frontend contains controls for browsing a JAR
 * file, selecting reading action and executing that action.
 * <p>
 * The following actions are supported:
 * <ul>
 * <li>Displaying code information using Java Reflection API.
 * <li>Display code information using Apache Commons BCEL.
 *     This also displays information related to class file structure
 * <li>Disassemble using Apache Commons BCEL. Disassembled class files can be still compiled, but
 *     instructions are substituted with commented out JVM assembly code.
 * <li>Retrieve method caller and callee information using Apache Commons BCEL.
 *     This one mimics the callgraph functionality of CodeCompass.
 * </ul>
 */
public final class Widget extends Application {

  private final static String COMBOBOX_CODEINFO_REFLECTION = "Display code information using Java Reflection API";
  private final static String COMBOBOX_CODEINFO_BCEL = "Display code information using Apache Commons BCEL";
  private final static String COMBOBOX_DISASSEMBLE_BCEL = "Disassemble using Apache Commons BCEL";
  private final static String COMBOBOX_CALLER_CALLEE_BCEL = "Retrieve method caller and callee information using Apache Commons BCEL";

  /**
   * Run GUI frontend and display controls.
   *
   * @param primaryStage  Primary Stage constructed by the platform
   */
  @Override
  public void start(final Stage primaryStage) {
    // Set up grid
    GridPane grid = new GridPane();
    grid.setAlignment(Pos.TOP_LEFT);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(25, 25, 25, 25));

    // Set up controls
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open JAR file");

    Button openFileButton = new Button("Choose JAR file");
    grid.add(openFileButton, 0, 0);

    TextField fileTextField = new TextField();
    grid.add(fileTextField, 1, 0);

    ComboBox<String> comboBox = new ComboBox<>();
    comboBox.getItems().addAll(
        COMBOBOX_CODEINFO_REFLECTION,
        COMBOBOX_CODEINFO_BCEL,
        COMBOBOX_DISASSEMBLE_BCEL,
        COMBOBOX_CALLER_CALLEE_BCEL
    );
    comboBox.getSelectionModel().selectFirst();
    grid.add(comboBox, 1, 1);

    // Set file browser action
    openFileButton.setOnAction(e ->
        Optional.ofNullable(fileChooser.showOpenDialog(primaryStage))
            .ifPresent(f -> fileTextField.setText(f.getAbsolutePath()))
    );

    // Set reader actions
    Button selectMethodButton = new Button("Execute action");
    selectMethodButton.setOnAction(event -> {
      Path jarPath = Paths.get(fileTextField.getText());
      if (jarPath.toString().isEmpty()) {
        errorPopup("File path is empty! Please select a JAR file.");
        return;
      }

      if (!Files.exists(jarPath)) {
        errorPopup("File does not exists!");
        return;
      }

      switch (comboBox.getValue()) {
        case COMBOBOX_CODEINFO_REFLECTION:
          try {
            printConsoleWindow(CodeInfoWithReflection.readJar(jarPath));
          } catch (ClassNotFoundException | NoClassDefFoundError e) {
            e.printStackTrace();
            errorPopup("One or more of the classes in JAR couldn't be parsed.\n" +
                "Make sure the folder structure follows the package hierarchy.");
          }
          break;

        case COMBOBOX_CODEINFO_BCEL:
          JarVisitor infoVisitor = new CodeInfoVisitor(jarPath);
          printConsoleWindow(infoVisitor.start().jarToString());
          break;

        case COMBOBOX_DISASSEMBLE_BCEL:
          JarVisitor disassembleVisitor = new DisassembleVisitor(jarPath);
          printConsoleWindow(disassembleVisitor.start().jarToString());
          break;

        case COMBOBOX_CALLER_CALLEE_BCEL:
          JarVisitor callerVisitor = new MethodCallInfoVisitor(jarPath);
          printConsoleWindow(callerVisitor.start().jarToString());
          break;
      }
    });
    grid.add(selectMethodButton, 0, 1);

    // Start GUI
    Scene scene = new Scene(grid);
    primaryStage.setTitle("Concept JAR Reader Prototype");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  /**
   * Print text in a new window containing a scrollable text area.
   *
   * @param text    Text to print in console window. This can contain multiple lines.
   */
  private void printConsoleWindow(final String text) {
    TextArea textArea = new TextArea();
    textArea.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
    textArea.setEditable(false);
    textArea.setText(text);
    BorderPane borderPane = new BorderPane(textArea);

    Stage stage = new Stage();
    stage.setScene(new Scene(borderPane, 700, 600));
    stage.show();
  }

  /**
   * Shorthand to create error pop-up messageboxes.
   *
   * @param message   Message to display in error pop-up
   */
  private void errorPopup(final String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(message);
    alert.showAndWait();
  }
}
