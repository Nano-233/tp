package tassist.address.ui;

import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tassist.address.commons.core.GuiSettings;
import tassist.address.commons.core.LogsCenter;
import tassist.address.logic.Logic;
import tassist.address.logic.commands.CommandResult;
import tassist.address.logic.commands.exceptions.CommandException;
import tassist.address.logic.parser.exceptions.ParseException;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private CommandBox commandBox;
    private CalendarView calendarView;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private StackPane sendButtonPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    @FXML
    private SplitPane splitPane;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     *
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        calendarView = new CalendarView(logic.getTimedEventList());

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        // Add send button
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> {
            try {
                String commandText = commandBox.getCommandText();
                if (!commandText.isEmpty()) {
                    executeCommand(commandText);
                    commandBox.clearCommandText();
                }
            } catch (CommandException | ParseException e) {
                logger.warning("Error executing command: " + e.getMessage());
                resultDisplay.setFeedbackToUser(e.getMessage());
            }
        });
        sendButtonPlaceholder.getChildren().add(sendButton);
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see tassist.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }

    @FXML
    private void handleStudentCardsView() {
        // Restore person list panel and split pane position
        personListPanelPlaceholder.getChildren().clear();
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());
        splitPane.lookupAll(".split-pane-divider").forEach(div -> div.setVisible(true));
        splitPane.setDividerPositions(0.35);
        
        // Restore result display
        resultDisplayPlaceholder.getChildren().clear();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());
        
        // Restore command box and send button
        commandBoxPlaceholder.getChildren().clear();
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> {
            try {
                String commandText = commandBox.getCommandText();
                if (!commandText.isEmpty()) {
                    executeCommand(commandText);
                    commandBox.clearCommandText();
                }
            } catch (CommandException | ParseException e) {
                logger.warning("Error executing command: " + e.getMessage());
                resultDisplay.setFeedbackToUser(e.getMessage());
            }
        });
        sendButtonPlaceholder.getChildren().clear();
        sendButtonPlaceholder.getChildren().add(sendButton);
    }

    @FXML
    private void handleCalendarView() {
        // Hide the person list panel and collapse the split pane
        personListPanelPlaceholder.getChildren().clear();
        splitPane.setDividerPositions(0.0);
        splitPane.lookupAll(".split-pane-divider").forEach(div -> div.setVisible(false));
        
        // Clear and show calendar in the main area
        resultDisplayPlaceholder.getChildren().clear();
        resultDisplayPlaceholder.getChildren().add(calendarView.getRoot());
        calendarView.updateEvents(logic.getTimedEventList());
        
        // Hide command box and send button
        commandBoxPlaceholder.getChildren().clear();
        sendButtonPlaceholder.getChildren().clear();
    }

    @FXML
    private void handleDarkTheme() {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
    }

    @FXML
    private void handleBrightTheme() {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/view/BrightTheme.css").toExternalForm());
    }

    @FXML
    private void handlePinkTheme() {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/view/PinkTheme.css").toExternalForm());
    }
}
