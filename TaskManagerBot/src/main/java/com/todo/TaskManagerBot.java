package com.todo;

import org.alicebot.ab.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskManagerBot extends JPanel {
    private Bot bot;
    private Chat chatSession;
    private JTextPane chatPane;
    private Style botStyle;
    private Style youStyle;
    private Style regularStyle;
    private JTextField userInputField;
    private boolean isAddingTask = false;
    private boolean isMarkingTaskComplete = false;
    private List<String> taskList; // Properly declared list variable

    // Create a Send button
    private JButton sendButton;

    // Create a button to display user commands
    private JButton showCommandsButton;

    public TaskManagerBot() {
        try {
            // Initialize the bot and load AIML files
            String resourcesPath = getResourcesPath();
            MagicBooleans.trace_mode = false; // Disable trace mode
            bot = new Bot("super", resourcesPath);
            chatSession = new Chat(bot);

            // Create UI components
            chatPane = new JTextPane();
            chatPane.setEditable(false);
            Font font = new Font(chatPane.getFont().getName(), Font.PLAIN, 14);
            chatPane.setFont(font);
            chatPane.setPreferredSize(new Dimension(200, 100)); // Adjust as needed

            StyledDocument doc = chatPane.getStyledDocument();
            botStyle = doc.addStyle("BotStyle", null);
            StyleConstants.setBold(botStyle, true);
            StyleConstants.setForeground(botStyle, Color.BLACK);

            youStyle = doc.addStyle("YouStyle", null);
            StyleConstants.setBold(youStyle, true);
            StyleConstants.setForeground(youStyle, Color.BLACK);

            regularStyle = doc.addStyle("RegularStyle", null);
            StyleConstants.setForeground(regularStyle, Color.BLACK);

            // Create a JTextField for user input with a smaller height
            userInputField = new JTextField(10);
            userInputField.setPreferredSize(new Dimension(userInputField.getPreferredSize().width, 30)); // Set the preferred height

            // Create a Send button
            sendButton = new JButton("Send");
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleUserInput(userInputField, chatPane);
                }
            });

            // Create a button to display user commands
            showCommandsButton = new JButton("Commands");
            showCommandsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    displayUserCommands();
                }
            });

            // Create an inputPanel to hold the user input field and buttons
            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new BorderLayout());

            // Create a JPanel for the user input field and set its preferred height
            JPanel userInputPanel = new JPanel();
            userInputPanel.setLayout(new BorderLayout());
            userInputPanel.add(userInputField, BorderLayout.CENTER);

            // Set the preferred height for the userInputPanel
            userInputPanel.setPreferredSize(new Dimension(userInputPanel.getPreferredSize().width, 10)); // Set the preferred height here

            inputPanel.add(userInputPanel, BorderLayout.CENTER);


            // Create a buttonsPanel for Send and Show Commands buttons on the right side
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            buttonsPanel.add(sendButton);
            buttonsPanel.add(showCommandsButton);
            inputPanel.add(buttonsPanel, BorderLayout.EAST);

            // Set up layout for the TaskManagerBot JPanel
            setLayout(new BorderLayout());

            // Create a scroll pane for chatPane and set the preferred size (adjust as needed)
            JScrollPane scrollPane = new JScrollPane(chatPane);
            scrollPane.setPreferredSize(new Dimension(200, 100)); // Adjust your preferred width and height

            add(scrollPane, BorderLayout.CENTER);

            // Add the inputPanel to the main panel
            add(inputPanel, BorderLayout.SOUTH);

            // Add an ActionListener to the userInputField to handle Enter key presses
            userInputField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleUserInput(userInputField, chatPane);
                }
            });

            // Display the greeting message
            appendToChat("🤖 Bot: Hi! Welcome. What can I do for you?\n", botStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getResourcesPath() {
        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        path = path.substring(0, path.length() - 2);
        String resourcesPath = path + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        return resourcesPath;
    }

    private void appendToChat(String message, Style style) {
        StyledDocument doc = chatPane.getStyledDocument();

        int botIndex = message.indexOf("🤖 Bot:");
        int youIndex = message.indexOf("👤 You:");

        Style boldStyle = doc.addStyle("BoldStyle", null);
        StyleConstants.setBold(boldStyle, true);

        Style iconStyle = doc.addStyle("IconStyle", null);
        StyleConstants.setFontSize(iconStyle, 18); // Set font size to 16 for icons

        try {
            if (style == botStyle && botIndex != -1) {
                doc.insertString(doc.getLength(), message.substring(0, botIndex), regularStyle);
                doc.insertString(doc.getLength(), "🤖", iconStyle); // Make "🤖" larger
                doc.insertString(doc.getLength(), " Bot:", boldStyle); // Make " Bot:" bold
                doc.insertString(doc.getLength(), message.substring(botIndex + 8), regularStyle);
            } else if (style == youStyle && youIndex != -1) {
                doc.insertString(doc.getLength(), message.substring(0, youIndex), regularStyle);
                doc.insertString(doc.getLength(), "👤", iconStyle); // Make "👤" larger
                doc.insertString(doc.getLength(), " You:", boldStyle); // Make " You:" bold
                doc.insertString(doc.getLength(), message.substring(youIndex + 8), regularStyle);
            } else {
                doc.insertString(doc.getLength(), message, style);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        chatPane.setCaretPosition(doc.getLength());
    }

    // Save a task to the database
    private void saveTaskToDatabase(String task) {
        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO TBDB (task_description) VALUES (?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, task);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Failed to save task to the database.\n", botStyle);
        }
    }

    // Load tasks from the database
    private List<String> loadTasksFromDatabase() {
        List<String> TBDB = new ArrayList<>();
        try (Connection connection = getConnection()) {
            String sql = "SELECT id, task_description, due_date FROM TBDB";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int taskId = resultSet.getInt("id");
                    String taskDescription = resultSet.getString("task_description");
                    Date dueDate = resultSet.getDate("due_date");
                    String taskLine = "- Task #" + taskId + ": " + taskDescription;
                    if (dueDate != null) {
                        taskLine += " (Due Date: " + dueDate.toString() + ")";
                    }
                    TBDB.add(taskLine);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Failed to load tasks from the database.\n", botStyle);
        }
        return TBDB;
    }

    // Get a database connection
    private Connection getConnection() {
        Connection connection = null;
        try {
            String url = "jdbc:mysql://localhost:3307/TBDB";
            String username = "root";
            String password = "9845621";
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to the database.");
        }
        return connection;
    }

    // Handle user input
    private void handleUserInput(JTextField userInputField, JTextPane chatPane) {
        String userInput = userInputField.getText().trim(); // Declare userInput outside of if statements

        // Check if the user input matches any of the commands and call the respective methods
        appendToChat("👤 You: " + userInput + "\n", youStyle);

        if (isMarkingTaskComplete) {
            int taskNumber = extractTaskNumber(userInput);
            if (taskNumber != -1) {
                markTaskAsComplete(taskNumber);
            } else {
                appendToChat("🤖 Bot: Please provide a valid task number.\n", botStyle);
            }
            isMarkingTaskComplete = false;
        } else {
            if (isAddingTask) {
                saveTaskToDatabase(userInput);
                appendToChat("🤖 Bot: Task '" + userInput + "' added to the database!\n", botStyle);
                isAddingTask = false;
            } else {
                userInput = userInput.toLowerCase();

                if (userInput.contains("hi") || userInput.contains("hello")) {
                    appendToChat("🤖 Bot: Hello! How can I assist you today?\n", botStyle);
                } else if (userInput.toLowerCase().startsWith("delete task")) {
                    String[] parts = userInput.split(" ");
                    if (parts.length >= 3) {
                        int taskNumber = extractTaskNumber(parts[2]);
                        if (taskNumber != -1) {
                            deleteTask(taskNumber);
                        } else {
                            appendToChat("🤖 Bot: Please provide a valid task number to delete.\n", botStyle);
                        }
                    } else {
                        appendToChat("🤖 Bot: To delete a task, please provide the task number, e.g., 'delete task 1'.\n", botStyle);
                    }
                } else if (userInput.startsWith("set due date for task")) {
                    String[] parts = userInput.split(" ");
                    if (parts.length >= 6) {
                        int taskNumber = extractTaskNumber(parts[5]);
                        String dueDate = parts[parts.length - 1];
                        if (taskNumber != -1) {
                            setDueDateForTask(taskNumber, dueDate);
                        } else {
                            appendToChat("🤖 Bot: Please provide a valid task number to set the due date.\n", botStyle);
                        }
                    } else {
                        appendToChat("🤖 Bot: Please provide a valid 'set due date' command.\n", botStyle);
                    }
                } else if (userInput.startsWith("add priority task")) {
                    String taskDescription = userInput.substring("add priority task".length()).trim();
                    if (!taskDescription.isEmpty()) {
                        addPriorityTaskToDatabase(taskDescription);
                        appendToChat("🤖 Bot: Priority task '" + taskDescription + "' added to the database!\n", botStyle);
                    } else {
                        appendToChat("🤖 Bot: Please provide a description for the priority task.\n", botStyle);
                    }
                } else if (userInput.equalsIgnoreCase("add task")) {
                    appendToChat("🤖 Bot: Please enter the task you want to add:\n", botStyle);
                    isAddingTask = true;
                } else if (userInput.equalsIgnoreCase("show list")) {
                    java.util.List<String> taskList = loadTasksFromDatabase();
                    showTaskList(taskList);
                } else if (userInput.toLowerCase().startsWith("mark") || userInput.toLowerCase().startsWith("complete")) {
                    appendToChat("🤖 Bot: Please provide the task number you want to mark as complete:\n", botStyle);
                    isMarkingTaskComplete = true;
                } else if (userInput.equalsIgnoreCase("exit")) {
                    exitApplication();
                } else if (userInput.equalsIgnoreCase("clear")) {
                    clearTasks();
                } else {
                    appendToChat("🤖 Bot: Sorry, I don't understand that command.\n", botStyle);
                }
            }
        }

        userInputField.setText("");
    }

    // Delete a task
    private void deleteTask(int taskIdToDelete) {
        try (Connection connection = getConnection()) {
            // Delete the task with the specified ID
            String deleteSql = "DELETE FROM TBDB WHERE id = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setInt(1, taskIdToDelete);
                int rowsDeleted = deleteStatement.executeUpdate();

                if (rowsDeleted > 0) {
                    appendToChat("🤖 Bot: Task #" + taskIdToDelete + " has been deleted!\n", botStyle);

                    // After deleting the task, update the task IDs in the database to remove any gaps
                    String updateSql = "UPDATE TBDB SET id = id - 1 WHERE id > ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                        updateStatement.setInt(1, taskIdToDelete);
                        int rowsUpdated = updateStatement.executeUpdate();
                        if (rowsUpdated > 0) {
                            appendToChat("🤖 Bot: Task IDs have been updated in the database.\n", botStyle);
                        }
                    }
                } else {
                    appendToChat("🤖 Bot: Task #" + taskIdToDelete + " not found.\n", botStyle);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Error occurred while deleting the task.\n", botStyle);
        }
    }

    // Set a due date for a task
    private void setDueDateForTask(int taskId, String dueDate) {
        try (Connection connection = getConnection()) {
            String sql = "UPDATE TBDB SET due_date = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                // Convert the due date string to a java.sql.Date
                java.sql.Date sqlDueDate = java.sql.Date.valueOf(dueDate);

                preparedStatement.setDate(1, sqlDueDate);
                preparedStatement.setInt(2, taskId);
                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated > 0) {
                    appendToChat("🤖 Bot: Due date set for Task #" + taskId + ": " + dueDate + "\n", botStyle);
                    // After setting the due date, refresh the task list if necessary
                    taskList = loadTasksFromDatabase();
                } else {
                    appendToChat("🤖 Bot: Task #" + taskId + " not found.\n", botStyle);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Error occurred while setting the due date for the task.\n", botStyle);
        }
    }

    // Add a priority task to the database
    private void addPriorityTaskToDatabase(String taskDescription) {
        try (Connection connection = getConnection()) {
            String sql = "INSERT INTO TBDB (task_description, priority) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, "[Priority] " + taskDescription);
                preparedStatement.setString(2, "High"); // Set priority to "High" (or preferred default)
                int rowsInserted = preparedStatement.executeUpdate();

                if (rowsInserted > 0) {
                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int taskId = generatedKeys.getInt(1);
                            appendToChat("🤖 Bot: Priority task (ID #" + taskId + ") '" + taskDescription + "' added to the database!\n", botStyle);
                        }
                    }
                } else {
                    appendToChat("🤖 Bot: Failed to save priority task to the database.\n", botStyle);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Failed to save priority task to the database.\n", botStyle);
        }
    }

    // Mark a task as complete
    private void markTaskAsComplete(int taskId) {
        try (Connection connection = getConnection()) {
            String sql = "UPDATE TBDB SET task_description = CONCAT('[Completed] ', task_description) WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, taskId);
                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated > 0) {
                    appendToChat("🤖 Bot: Task #" + taskId + " marked as complete!\n", botStyle);
                    // After marking the task as complete, refresh the task list
                    taskList = loadTasksFromDatabase();
                } else {
                    appendToChat("🤖 Bot: Failed to mark task as complete.\n", botStyle);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Error occurred while marking the task as complete.\n", botStyle);
        }
    }

    // Extract task number from user input
    private int extractTaskNumber(String userInput) {
        try {
            userInput = userInput.toLowerCase().trim();
            if (userInput.startsWith("task")) {
                userInput = userInput.substring("task".length()).trim();
            }
            return Integer.parseInt(userInput);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Show the list of tasks
    private void showTaskList(java.util.List<String> taskList) {
        if (taskList.isEmpty()) {
            appendToChat("🤖 Bot: The task list is empty.\n", botStyle);
        } else {
            appendToChat("🤖 Bot: Here is your task list:\n", botStyle);
            int taskNumber = 1;
            for (String task : taskList) {
                String taskDescription = task.replaceFirst("- Task #" + taskNumber + ": ", "");

                if (taskDescription.contains("(Due Date:") && taskDescription.contains("(Priority)")) {
                    // Task has both due date and priority
                    int dueDateStart = taskDescription.indexOf("(Due Date:") + 10;
                    int dueDateEnd = taskDescription.indexOf(")", dueDateStart);
                    String dueDate = taskDescription.substring(dueDateStart, dueDateEnd);

                    int priorityStart = taskDescription.indexOf("(Priority)") + 11;
                    String priority = taskDescription.substring(priorityStart);

                    taskDescription = taskDescription.replace("(Due Date:" + dueDate + ")", "");
                    String formattedTask = "- Task #" + taskNumber + ": " + taskDescription + " (Due Date: " + dueDate + ") " + "(" + priority + ")\n";
                    appendToChatWithBold(formattedTask);
                } else if (taskDescription.contains("(Due Date:")) {
                    // Task has a due date
                    int dueDateStart = taskDescription.indexOf("(Due Date:") + 10;
                    int dueDateEnd = taskDescription.indexOf(")", dueDateStart);
                    String dueDate = taskDescription.substring(dueDateStart, dueDateEnd);

                    taskDescription = taskDescription.replace("(Due Date:" + dueDate + ")", "");
                    String formattedTask = "- Task #" + taskNumber + ": " + taskDescription + " (Due Date: " + dueDate + ")\n";
                    appendToChatWithBold(formattedTask);
                } else if (taskDescription.contains("(Priority)")) {
                    // Task has priority
                    int priorityStart = taskDescription.indexOf("(Priority)") + 11;
                    String priority = taskDescription.substring(priorityStart);

                    taskDescription = taskDescription.replace("(Priority)", "");
                    String formattedTask = "- Task #" + taskNumber + ": " + taskDescription + " (" + priority + ")\n";
                    appendToChatWithBold(formattedTask);
                } else {
                    // Task has neither due date nor priority
                    String formattedTask = "- Task #" + taskNumber + ": " + taskDescription + "\n";
                    appendToChatWithBold(formattedTask);
                }

                taskNumber++;
            }
        }
    }

    private void appendToChatWithBold(String message) {
        StyledDocument doc = chatPane.getStyledDocument();
        Style boldStyle = doc.addStyle("BoldStyle", null);
        StyleConstants.setBold(boldStyle, true);

        try {
            doc.insertString(doc.getLength(), message, boldStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Exit the application
    private void exitApplication() {
        appendToChat("🤖 Bot: Goodbye!\n", botStyle);
        new Thread(() -> {
            try {
                Thread.sleep(2000);  // Delay for 2 seconds
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            appendToChat("🤖 Bot: Have a nice day!\n", botStyle);
            userInputField.setEditable(false);
            try {
                Thread.sleep(2000);  // Delay for 2 seconds before closing
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.exit(0);  // Close the application
        }).start();
    }

    private void displayUserCommands() {
        // Clear the chat pane
        chatPane.setText("");

        // Display user commands
        appendToChat("🤖 Bot: User Commands:\n", botStyle);
        appendToChat("1. Hi or Hello - For Greeting\n", regularStyle);
        appendToChat("2. Delete Task - To Delete a task\n", regularStyle);
        appendToChat("3. Set Due Date for Task  - To Set a due date for a task\n", regularStyle);
        appendToChat("4. Add Priority Task - To Add a priority task\n", regularStyle);
        appendToChat("5. Add Task - Start adding a task to Database\n", regularStyle);
        appendToChat("6. Show List - Show the task list in Database\n", regularStyle);
        appendToChat("7. Mark task as complete - To Mark a task as complete\n", regularStyle);
        appendToChat("8. Exit - To Exit the application\n", regularStyle);
    }

    private void clearTasks() {
        try (Connection connection = getConnection()) {
            // Clear all tasks from the database
            String clearSql = "DELETE FROM TBDB";
            try (PreparedStatement clearStatement = connection.prepareStatement(clearSql)) {
                clearStatement.executeUpdate();
            }

            // Reset the task IDs in the database to start from 1
            String resetSql = "ALTER TABLE TBDB AUTO_INCREMENT = 1";
            try (PreparedStatement resetStatement = connection.prepareStatement(resetSql)) {
                resetStatement.executeUpdate();
            }

            appendToChat("🤖 Bot: All tasks have been cleared, and task IDs have been reset.\n", botStyle);
        } catch (SQLException e) {
            e.printStackTrace();
            appendToChat("🤖 Bot: Error occurred while clearing tasks and resetting task IDs.\n", botStyle);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Task Manager Bot");
            TaskManagerBot toDoListGui = new TaskManagerBot();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(toDoListGui);

            // Set the preferred size of the JFrame (e.g., 800x600)
            frame.setPreferredSize(new Dimension(450, 300));

            frame.pack();

            // Get the screen dimensions
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            // Calculate the desired position for the bottom-right corner
            int x = (int) (screenSize.getWidth() - frame.getWidth());
            int y = (int) (screenSize.getHeight() - frame.getHeight());

            // Set the JFrame location
            frame.setLocation(x, y);

            frame.setVisible(true);
        });
    }
}