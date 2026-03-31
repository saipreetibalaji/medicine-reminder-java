package mini_project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class MedicineReminderFX extends Application {

    private final Map<String, ObservableList<Medicine>> profiles = new HashMap<>();
    private final ComboBox<String> profileBox = new ComboBox<>();
    private final ObservableList<String> profileNames = FXCollections.observableArrayList();
    private ObservableList<Medicine> currentMedicineList = FXCollections.observableArrayList();
    
    // tableView is defined before it's used
    private final TableView<Medicine> tableView = new TableView<>();
    
    // For showing upcoming reminders
    private final ListView<String> reminderListView = new ListView<>();
    
    // For missed doses tracking
    private final ListView<String> missedDosesListView = new ListView<>();

    private static final String FILE_PATH = "medicine_data_profiles.txt";
    private static final String READABLE_DATA_PATH = "medicine_data_readable.txt";
    
    // For theme handling
    private boolean isDarkTheme = false;
    private Scene mainScene;
    private BorderPane mainLayout;
    
    // For navigation
    private VBox dashboardContent;
    private VBox medicinesContent;
    private VBox remindersContent;
    private VBox settingsContent;
    private VBox currentContent;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Medicine Reminder App");

        loadMedicineData();

        // Create main BorderPane layout
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f5f7fa;");
        
        // Create header
        HBox header = createHeader();
        mainLayout.setTop(header);
        
        // Create side navigation
        VBox sideNav = createSideNav();
        mainLayout.setLeft(sideNav);
        
        // Create all content sections
        dashboardContent = createDashboardContent();
        medicinesContent = createMedicinesContent();
        remindersContent = createRemindersContent();
        settingsContent = createSettingsContent();
        
        // Set initial content to dashboard
        currentContent = dashboardContent;
        mainLayout.setCenter(currentContent);
        
        // Apply inline styles
        applyInlineStyles(header, sideNav, currentContent);
        
        mainScene = new Scene(mainLayout, 950, 700);
        primaryStage.setScene(mainScene);
        primaryStage.show();

        startReminderChecker();
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setSpacing(10);
        
        Label appTitle = new Label("Medicine Reminder");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        profileBox.setItems(profileNames);
        profileBox.setPromptText("Select Profile");
        profileBox.setPrefWidth(150);
        
        TextField profileNameField = new TextField();
        profileNameField.setPromptText("New Profile Name");
        profileNameField.setPrefWidth(150);
        
        Button createProfileBtn = new Button("Create Profile");
        styleButton(createProfileBtn, "#5D9CEC", "#4A89DC");
        
        createProfileBtn.setOnAction(e -> {
            String newProfile = profileNameField.getText().trim();
            if (!newProfile.isEmpty() && !profiles.containsKey(newProfile)) {
                profiles.put(newProfile, FXCollections.observableArrayList());
                profileNames.add(newProfile);
                profileBox.setValue(newProfile);
                currentMedicineList = profiles.get(newProfile);
                saveMedicineData();
                saveReadableMedicineData();
                showNotification("Profile Created", "New profile '" + newProfile + "' has been created.");
                updateReminderView(); // Update the reminder view when profile changes
            }
        });
        
        profileBox.setOnAction(e -> {
            String selected = profileBox.getValue();
            if (selected != null) {
                currentMedicineList = profiles.get(selected);
                tableView.setItems(currentMedicineList);
                updateReminderView(); // Update the reminder view when profile changes
                updateMissedDosesView(); // Update missed doses view
            }
        });
        
        header.getChildren().addAll(appTitle, spacer, profileNameField, createProfileBtn, profileBox);
        return header;
    }
    
    private VBox createSideNav() {
        VBox sideNav = new VBox();
        sideNav.setPadding(new Insets(20));
        sideNav.setSpacing(15);
        sideNav.setPrefWidth(200);
        
        Label navTitle = new Label("Dashboard");
        navTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        Button dashboardBtn = createNavButton("Dashboard", true);
        Button medicinesBtn = createNavButton("Medicines", false);
        Button remindersBtn = createNavButton("Reminders", false);
        Button settingsBtn = createNavButton("Settings", false);
        
        // Add navigation functionality
        dashboardBtn.setOnAction(e -> switchContent(dashboardContent));
        medicinesBtn.setOnAction(e -> switchContent(medicinesContent));
        remindersBtn.setOnAction(e -> {
            updateReminderView();
            updateMissedDosesView();
            switchContent(remindersContent);
        });
        settingsBtn.setOnAction(e -> switchContent(settingsContent));
        
        sideNav.getChildren().addAll(navTitle, dashboardBtn, medicinesBtn, remindersBtn, settingsBtn);
        
        return sideNav;
    }
    
    private void switchContent(VBox newContent) {
        // Get the parent BorderPane
        BorderPane parent = (BorderPane) currentContent.getParent();
        
        // Update styling
        currentContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        newContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // Replace the center content
        parent.setCenter(newContent);
        currentContent = newContent;
    }
    
    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setPrefWidth(160);
        btn.setPrefHeight(40);
        
        if (isActive) {
            btn.setStyle(isDarkTheme ? 
                "-fx-background-color: #4A5568; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 14px; -fx-font-weight: bold;" :
                "-fx-background-color: #5D9CEC; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 14px; -fx-font-weight: bold;");
        } else {
            btn.setStyle(isDarkTheme ?
                "-fx-background-color: transparent; -fx-text-fill: #CBD5E0; " +
                "-fx-background-radius: 5; -fx-font-size: 14px;" :
                "-fx-background-color: transparent; -fx-text-fill: #656D78; " +
                "-fx-background-radius: 5; -fx-font-size: 14px;");
            
            btn.setOnMouseEntered(e -> 
                btn.setStyle(isDarkTheme ?
                    "-fx-background-color: #4A5568; -fx-text-fill: #CBD5E0; " +
                    "-fx-background-radius: 5; -fx-font-size: 14px;" :
                    "-fx-background-color: #E6E9ED; -fx-text-fill: #656D78; " +
                    "-fx-background-radius: 5; -fx-font-size: 14px;"));
            
            btn.setOnMouseExited(e -> 
                btn.setStyle(isDarkTheme ?
                    "-fx-background-color: transparent; -fx-text-fill: #CBD5E0; " +
                    "-fx-background-radius: 5; -fx-font-size: 14px;" :
                    "-fx-background-color: transparent; -fx-text-fill: #656D78; " +
                    "-fx-background-radius: 5; -fx-font-size: 14px;"));
        }
        
        return btn;
    }
    
    private VBox createDashboardContent() {
        VBox contentArea = new VBox();
        contentArea.setPadding(new Insets(25));
        contentArea.setSpacing(20);
        
        // Dashboard welcome message
        Label welcomeLabel = new Label("Welcome to Medicine Reminder");
        welcomeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        
        // Dashboard summary section
        HBox summaryBox = new HBox(30);
        summaryBox.setPadding(new Insets(20));
        summaryBox.setStyle(isDarkTheme ?
            "-fx-background-color: #1A202C; -fx-background-radius: 5; -fx-border-color: #4A5568; -fx-border-radius: 5;" :
            "-fx-background-color: #F8F9FA; -fx-background-radius: 5; -fx-border-color: #E6E9ED; -fx-border-radius: 5;");
        
        VBox totalMedBox = createSummaryCard("Total Medications", "0");
        VBox upcomingBox = createSummaryCard("Upcoming Doses", "0");
        VBox missedBox = createSummaryCard("Missed Doses", "0");
        
        summaryBox.getChildren().addAll(totalMedBox, upcomingBox, missedBox);
        
        // Today's schedule
        Label scheduleLabel = new Label("Today's Schedule");
        scheduleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        ListView<String> todayScheduleList = new ListView<>();
        updateTodaySchedule(todayScheduleList);
        
        Button refreshDashboardBtn = new Button("Refresh Dashboard");
        styleButton(refreshDashboardBtn, "#5D9CEC", "#4A89DC");
        refreshDashboardBtn.setOnAction(e -> {
            updateTodaySchedule(todayScheduleList);
            updateDashboardSummary(totalMedBox, upcomingBox, missedBox);
        });
        
        contentArea.getChildren().addAll(welcomeLabel, summaryBox, scheduleLabel, 
                todayScheduleList, refreshDashboardBtn);
        
        return contentArea;
    }
    
    private VBox createSummaryCard(String title, String value) {
        VBox card = new VBox(5);
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", 14));
        titleLabel.setStyle(isDarkTheme ? "-fx-text-fill: #A0AEC0;" : "-fx-text-fill: #656D78;");
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        valueLabel.setStyle(isDarkTheme ? "-fx-text-fill: #90CDF4;" : "-fx-text-fill: #5D9CEC;");
        
        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }
    
    private void updateTodaySchedule(ListView<String> scheduleList) {
        ObservableList<String> scheduleItems = FXCollections.observableArrayList();
        LocalDate today = LocalDate.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (String profile : profiles.keySet()) {
            for (Medicine med : profiles.get(profile)) {
                try {
                    if (med.morning != null && !med.morning.equalsIgnoreCase("null")) {
                        LocalTime morningTime = LocalTime.parse(med.morning);
                        scheduleItems.add(String.format("%s - %s - Morning dose at %s %s", 
                            profile, med.name, morningTime.format(timeFormatter), med.mealTiming));
                    }
                    
                    if (med.afternoon != null && !med.afternoon.equalsIgnoreCase("null")) {
                        LocalTime afternoonTime = LocalTime.parse(med.afternoon);
                        scheduleItems.add(String.format("%s - %s - Afternoon dose at %s %s", 
                            profile, med.name, afternoonTime.format(timeFormatter), med.mealTiming));
                    }
                    
                    if (med.night != null && !med.night.equalsIgnoreCase("null")) {
                        LocalTime nightTime = LocalTime.parse(med.night);
                        scheduleItems.add(String.format("%s - %s - Night dose at %s %s", 
                            profile, med.name, nightTime.format(timeFormatter), med.mealTiming));
                    }
                } catch (Exception e) {
                    // Skip invalid time entries
                }
            }
        }
        
        scheduleList.setItems(scheduleItems);
    }
    
    private void updateDashboardSummary(VBox totalMedBox, VBox upcomingBox, VBox missedBox) {
        // Count total meds
        int totalMeds = 0;
        int upcomingDoses = 0;
        int missedDoses = 0;
        
        for (String profile : profiles.keySet()) {
            totalMeds += profiles.get(profile).size();
            
            for (Medicine med : profiles.get(profile)) {
                // Count upcoming doses
                if (med.morning != null && !med.morning.equalsIgnoreCase("null")) upcomingDoses++;
                if (med.afternoon != null && !med.afternoon.equalsIgnoreCase("null")) upcomingDoses++;
                if (med.night != null && !med.night.equalsIgnoreCase("null")) upcomingDoses++;
                
                // Count missed doses for today
                for (String missedDose : med.missedDoses.keySet()) {
                    if (missedDose.contains(LocalDate.now().toString())) {
                        missedDoses++;
                    }
                }
            }
        }
        
        // Update labels
        ((Label)totalMedBox.getChildren().get(0)).setText(String.valueOf(totalMeds));
        ((Label)upcomingBox.getChildren().get(0)).setText(String.valueOf(upcomingDoses));
        ((Label)missedBox.getChildren().get(0)).setText(String.valueOf(missedDoses));
    }
    
    private VBox createMedicinesContent() {
        VBox contentArea = new VBox();
        contentArea.setPadding(new Insets(25));
        contentArea.setSpacing(20);
        
        // Add New Medicine Section
        TitledPane addMedicinePane = createAddMedicinePane();
        addMedicinePane.setExpanded(false);
        
        // Medicine Table Section
        Label tableTitle = new Label("Your Medications");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        configureTableView();
        
        // Add content to main area
        contentArea.getChildren().addAll(addMedicinePane, tableTitle, tableView);
        
        return contentArea;
    }
    
    private VBox createRemindersContent() {
        VBox contentArea = new VBox();
        contentArea.setPadding(new Insets(25));
        contentArea.setSpacing(20);
        
        // Upcoming Reminders Section
        Label upcomingTitle = new Label("Upcoming Reminders");
        upcomingTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        // Refresh button
        Button refreshBtn = new Button("Refresh Reminders");
        styleButton(refreshBtn, "#5D9CEC", "#4A89DC");
        refreshBtn.setOnAction(e -> {
            updateReminderView();
            updateMissedDosesView();
        });
        
        // Missed Doses Section
        Label missedTitle = new Label("Missed Doses");
        missedTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        
        // Clear missed doses button
        Button clearMissedBtn = new Button("Clear Missed Doses History");
        styleButton(clearMissedBtn, "#ED5565", "#DA4453");
        clearMissedBtn.setOnAction(e -> {
            String profile = profileBox.getValue();
            if (profile != null) {
                for (Medicine med : profiles.get(profile)) {
                    med.missedDoses.clear();
                }
                updateMissedDosesView();
                saveMedicineData();
                saveReadableMedicineData();
                showNotification("History Cleared", "Missed dose history has been cleared.");
            }
        });
        
        // Add to layout
        contentArea.getChildren().addAll(upcomingTitle, reminderListView, refreshBtn, 
                                        missedTitle, missedDosesListView, clearMissedBtn);
        
        return contentArea;
    }
    
    private VBox createSettingsContent() {
        VBox contentArea = new VBox();
        contentArea.setPadding(new Insets(25));
        contentArea.setSpacing(20);
        
        Label settingsTitle = new Label("Settings");
        settingsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        
        // Theme Setting
        Label themeLabel = new Label("Theme Settings");
        themeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        // Theme toggle button
        Button toggleThemeBtn = new Button(isDarkTheme ? "Switch to Light Theme" : "Switch to Dark Theme");
        styleButton(toggleThemeBtn, "#5D9CEC", "#4A89DC");
        toggleThemeBtn.setOnAction(e -> {
            isDarkTheme = !isDarkTheme;
            applyTheme();
            toggleThemeBtn.setText(isDarkTheme ? "Switch to Light Theme" : "Switch to Dark Theme");
        });
        
        // Data Management
        Label dataLabel = new Label("Data Management");
        dataLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        Button exportBtn = new Button("Export Medicine Data");
        styleButton(exportBtn, "#5D9CEC", "#4A89DC");
        exportBtn.setOnAction(e -> {
            saveMedicineData();
            saveReadableMedicineData();
            showNotification("Data Exported", "Medicine data has been exported to: " + READABLE_DATA_PATH);
        });
        
        Button importBtn = new Button("Reload Medicine Data");
        styleButton(importBtn, "#5D9CEC", "#4A89DC");
        importBtn.setOnAction(e -> {
            profiles.clear();
            profileNames.clear();
            loadMedicineData();
            showNotification("Data Reloaded", "Medicine data has been reloaded from: " + FILE_PATH);
        });
        
        // Notification Settings
        Label notificationLabel = new Label("Notification Settings");
        notificationLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        
        CheckBox enableNotifications = new CheckBox("Enable Notifications");
        enableNotifications.setSelected(true);
        
        contentArea.getChildren().addAll(
            settingsTitle, 
            themeLabel, toggleThemeBtn,
            dataLabel, exportBtn, importBtn,
            notificationLabel, enableNotifications
        );
        
        return contentArea;
    }
    
    private void applyTheme() {
        // Apply theme to main components
        mainLayout.setStyle(isDarkTheme ? "-fx-background-color: #1A202C;" : "-fx-background-color: #f5f7fa;");
        
        // Apply theme to headers, sidebars
        HBox header = (HBox) mainLayout.getTop();
        VBox sideNav = (VBox) mainLayout.getLeft();
        
        header.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-border-color: #4A5568; -fx-border-width: 0 0 1 0;" :
            "-fx-background-color: white; -fx-border-color: #E6E9ED; -fx-border-width: 0 0 1 0;");
        
        sideNav.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-border-color: #4A5568; -fx-border-width: 0 1 0 0;" :
            "-fx-background-color: white; -fx-border-color: #E6E9ED; -fx-border-width: 0 1 0 0;");
        
        // Apply theme to current content
        currentContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // Update theme for all content areas
        dashboardContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            
        medicinesContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            
        remindersContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            
        settingsContent.setStyle(isDarkTheme ? 
            "-fx-background-color: #2D3748; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);" :
            "-fx-background-color: white; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // Update text colors for all labels in the application
        updateLabelsForTheme(mainLayout);
    }
    
    private void updateLabelsForTheme(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                // Don't change special labels (like those in summary cards)
                if (!label.getStyle().contains("-fx-text-fill")) {
                    label.setTextFill(isDarkTheme ? Color.web("#E2E8F0") : Color.web("#333333"));
                }
            } else if (node instanceof Parent) {
                updateLabelsForTheme((Parent) node);
            }
        }
    }
    
    private TitledPane createAddMedicinePane() {
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Add New Medication");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        // Input Fields
        TextField nameField = new TextField();
        nameField.setPromptText("Medicine Name");
        styleTextField(nameField);
        
        TextField dosageField = new TextField();
        dosageField.setPromptText("Dosage (e.g., 25mg)");
        styleTextField(dosageField);
        
        TextField ageField = new TextField();
        ageField.setPromptText("Patient Age");
        styleTextField(ageField);
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Total Quantity");
        styleTextField(quantityField);
        
        TextField morningTimeField = new TextField();
        morningTimeField.setPromptText("Morning Time (HH:MM or null)");
        styleTextField(morningTimeField);
        
        TextField afternoonTimeField = new TextField();
        afternoonTimeField.setPromptText("Afternoon Time (HH:MM or null)");
        styleTextField(afternoonTimeField);
        
        TextField nightTimeField = new TextField();
        nightTimeField.setPromptText("Night Time (HH:MM or null)");
        styleTextField(nightTimeField);
        
        ComboBox<String> mealTimingBox = new ComboBox<>();
        mealTimingBox.getItems().addAll("Before Meals", "After Meals");
        mealTimingBox.setPromptText("Before or After Meals");
        mealTimingBox.setPrefWidth(200);
        
        Button submitButton = new Button("Add Medicine");
        styleButton(submitButton, "#5D9CEC", "#4A89DC");
        submitButton.setPrefWidth(Double.MAX_VALUE);
        
        Label outputLabel = new Label();
        outputLabel.setStyle("-fx-text-fill: #4A89DC;");
        
        submitButton.setOnAction(e -> {
            String profile = profileBox.getValue();
            if (profile == null) {
                outputLabel.setText("Please select a profile.");
                outputLabel.setStyle("-fx-text-fill: #ED5565;");
                return;
            }

            try {
                String name = nameField.getText();
                String dosage = dosageField.getText();
                String age = ageField.getText();
                int quantity = Integer.parseInt(quantityField.getText());
                String morning = morningTimeField.getText();
                String afternoon = afternoonTimeField.getText();
                String night = nightTimeField.getText();
                String meal = mealTimingBox.getValue();

                if (name.isEmpty() || dosage.isEmpty() || age.isEmpty() || meal == null)
                    throw new Exception();

                Medicine med = new Medicine(name, dosage, age, quantity, morning, afternoon, night, meal);
                currentMedicineList.add(med);
                outputLabel.setText("Medicine Added: " + name);
                outputLabel.setStyle("-fx-text-fill: #8CC152;");
                saveMedicineData();
                saveReadableMedicineData();
                
                // Clear fields
                nameField.clear();
                dosageField.clear();
                quantityField.clear();
                morningTimeField.clear();
                afternoonTimeField.clear();
                nightTimeField.clear();
                mealTimingBox.setValue(null);
                
                // Update views
                updateReminderView();
                
                showNotification("Medicine Added", name + " has been added to your medicines.");
            } catch (Exception ex) {
                outputLabel.setText("Fill all fields correctly.");
                outputLabel.setStyle("-fx-text-fill: #ED5565;");
            }
        });
        
        // Add components to grid
        grid.add(new Label("Medicine Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Dosage:"), 0, 1);
        grid.add(dosageField, 1, 1);
        grid.add(new Label("Patient Age:"), 0, 2);
        grid.add(ageField, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(quantityField, 1, 3);
        
        grid.add(new Label("Morning Time:"), 2, 0);
        grid.add(morningTimeField, 3, 0);
        grid.add(new Label("Afternoon Time:"), 2, 1);
        grid.add(afternoonTimeField, 3, 1);
        grid.add(new Label("Night Time:"), 2, 2);
        grid.add(nightTimeField, 3, 2);
        grid.add(new Label("Meal Timing:"), 2, 3);
        grid.add(mealTimingBox, 3, 3);

        grid.add(submitButton, 0, 4, 4, 1);
        grid.add(outputLabel, 0, 5, 4, 1);

        titledPane.setContent(grid);
        return titledPane;
        }

        private void configureTableView() {
            // Define table columns
            TableColumn<Medicine, String> nameColumn = new TableColumn<>("Medicine Name");
            nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name));
            nameColumn.setPrefWidth(150);

            TableColumn<Medicine, String> dosageColumn = new TableColumn<>("Dosage");
            dosageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().dosage));
            dosageColumn.setPrefWidth(100);

            TableColumn<Medicine, String> morningColumn = new TableColumn<>("Morning");
            morningColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().morning != null && !cellData.getValue().morning.equalsIgnoreCase("null") ?
                    cellData.getValue().morning : "—"));
            morningColumn.setPrefWidth(100);

            TableColumn<Medicine, String> afternoonColumn = new TableColumn<>("Afternoon");
            afternoonColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().afternoon != null && !cellData.getValue().afternoon.equalsIgnoreCase("null") ?
                    cellData.getValue().afternoon : "—"));
            afternoonColumn.setPrefWidth(100);

            TableColumn<Medicine, String> nightColumn = new TableColumn<>("Night");
            nightColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                    cellData.getValue().night != null && !cellData.getValue().night.equalsIgnoreCase("null") ?
                    cellData.getValue().night : "—"));
            nightColumn.setPrefWidth(100);

            TableColumn<Medicine, String> mealColumn = new TableColumn<>("Meal Timing");
            mealColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().mealTiming));
            mealColumn.setPrefWidth(120);

            TableColumn<Medicine, String> qtyColumn = new TableColumn<>("Quantity");
            qtyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().quantity)));
            qtyColumn.setPrefWidth(80);

            // Delete action column
            TableColumn<Medicine, Void> actionColumn = new TableColumn<>("Actions");
            actionColumn.setPrefWidth(100);
            actionColumn.setCellFactory(param -> new TableCell<>() {
                private final Button deleteButton = new Button("Delete");

                {
                    styleButton(deleteButton, "#ED5565", "#DA4453");
                    deleteButton.setOnAction(event -> {
                        Medicine medicine = getTableView().getItems().get(getIndex());
                        currentMedicineList.remove(medicine);
                        saveMedicineData();
                        saveReadableMedicineData();
                        updateReminderView();
                        showNotification("Medicine Deleted", medicine.name + " has been removed from your medicines.");
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteButton);
                    }
                }
            });

            tableView.getColumns().addAll(nameColumn, dosageColumn, morningColumn,
                    afternoonColumn, nightColumn, mealColumn, qtyColumn, actionColumn);
            tableView.setItems(currentMedicineList);
        }

        private void updateReminderView() {
            String selectedProfile = profileBox.getValue();
            if (selectedProfile == null) return;

            ObservableList<String> reminderItems = FXCollections.observableArrayList();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            for (Medicine med : profiles.get(selectedProfile)) {
                try {
                    if (med.morning != null && !med.morning.equalsIgnoreCase("null")) {
                        LocalTime morningTime = LocalTime.parse(med.morning);
                        LocalDateTime morningDateTime = LocalDateTime.of(now.toLocalDate(), morningTime);
                        if (morningDateTime.isAfter(now)) {
                            reminderItems.add(String.format("%s - Morning dose at %s %s", 
                                med.name, morningTime.format(timeFormatter), med.mealTiming));
                        }
                    }
                    
                    if (med.afternoon != null && !med.afternoon.equalsIgnoreCase("null")) {
                        LocalTime afternoonTime = LocalTime.parse(med.afternoon);
                        LocalDateTime afternoonDateTime = LocalDateTime.of(now.toLocalDate(), afternoonTime);
                        if (afternoonDateTime.isAfter(now)) {
                            reminderItems.add(String.format("%s - Afternoon dose at %s %s", 
                                med.name, afternoonTime.format(timeFormatter), med.mealTiming));
                        }
                    }
                    
                    if (med.night != null && !med.night.equalsIgnoreCase("null")) {
                        LocalTime nightTime = LocalTime.parse(med.night);
                        LocalDateTime nightDateTime = LocalDateTime.of(now.toLocalDate(), nightTime);
                        if (nightDateTime.isAfter(now)) {
                            reminderItems.add(String.format("%s - Night dose at %s %s", 
                                med.name, nightTime.format(timeFormatter), med.mealTiming));
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid time entries
                }
            }

            reminderListView.setItems(reminderItems);
        }

        private void updateMissedDosesView() {
            String selectedProfile = profileBox.getValue();
            if (selectedProfile == null) return;

            ObservableList<String> missedItems = FXCollections.observableArrayList();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Medicine med : profiles.get(selectedProfile)) {
                for (Map.Entry<String, String> entry : med.missedDoses.entrySet()) {
                    missedItems.add(String.format("%s - %s - Missed at %s", 
                        med.name, entry.getValue(), entry.getKey()));
                }
            }

            missedDosesListView.setItems(missedItems);
        }

        private void startReminderChecker() {
            scheduler.scheduleAtFixedRate(() -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDate today = now.toLocalDate();
                LocalTime currentTime = now.toLocalTime();

                for (String profileName : profiles.keySet()) {
                    for (Medicine med : profiles.get(profileName)) {
                        checkMedicineTime(med, "Morning", med.morning, currentTime, today, profileName);
                        checkMedicineTime(med, "Afternoon", med.afternoon, currentTime, today, profileName);
                        checkMedicineTime(med, "Night", med.night, currentTime, today, profileName);
                    }
                }
            }, 0, 1, TimeUnit.MINUTES);
        }

        private void checkMedicineTime(Medicine med, String timeOfDay, String timeStr, 
                                     LocalTime currentTime, LocalDate today, String profileName) {
            if (timeStr == null || timeStr.equalsIgnoreCase("null")) return;
            
            try {
                LocalTime medicineTime = LocalTime.parse(timeStr);
                
                // Check if it's time for reminder (within 1 minute)
                if (Math.abs(currentTime.getHour() - medicineTime.getHour()) == 0 &&
                    Math.abs(currentTime.getMinute() - medicineTime.getMinute()) <= 1) {
                    
                    Platform.runLater(() -> {
                        showReminderNotification(med, timeOfDay, profileName);
                    });
                }
                
                // Check for missed doses (more than 30 minutes past the scheduled time)
                if ((currentTime.getHour() > medicineTime.getHour() || 
                    (currentTime.getHour() == medicineTime.getHour() && 
                     currentTime.getMinute() > medicineTime.getMinute() + 30))) {
                    
                    // Add to missed doses if not already there
                    String timeKey = today.toString() + " " + timeStr;
                    if (!med.missedDoses.containsKey(timeKey)) {
                        med.missedDoses.put(timeKey, timeOfDay);
                        saveMedicineData();
                        saveReadableMedicineData();
                        
                        Platform.runLater(() -> {
                            updateMissedDosesView();
                        });
                    }
                }
            } catch (Exception e) {
                // Skip invalid time
            }
        }

        private void showReminderNotification(Medicine med, String timeOfDay, String profileName) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Medicine Reminder");
            alert.setHeaderText("Time to take medicine");
            alert.setContentText(String.format("Profile: %s\nMedicine: %s\nDosage: %s\nTiming: %s %s", 
                profileName, med.name, med.dosage, timeOfDay, med.mealTiming));
            alert.show();
        }

        private void loadMedicineData() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
                Map<String, List<Medicine>> loadedProfiles = (Map<String, List<Medicine>>) ois.readObject();
                
                for (Map.Entry<String, List<Medicine>> entry : loadedProfiles.entrySet()) {
                    profileNames.add(entry.getKey());
                    profiles.put(entry.getKey(), FXCollections.observableArrayList(entry.getValue()));
                }
                
                if (!profileNames.isEmpty()) {
                    profileBox.setValue(profileNames.get(0));
                    currentMedicineList = profiles.get(profileNames.get(0));
                    tableView.setItems(currentMedicineList);
                }
            } catch (FileNotFoundException e) {
                // File doesn't exist yet, that's OK
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("Load Error", "Error loading medicine data.");
            }
        }

        private void saveMedicineData() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
                // Convert ObservableList to regular Lists for serialization
                Map<String, List<Medicine>> serializableProfiles = new HashMap<>();
                for (Map.Entry<String, ObservableList<Medicine>> entry : profiles.entrySet()) {
                    serializableProfiles.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
                
                oos.writeObject(serializableProfiles);
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("Save Error", "Error saving medicine data.");
            }
        }

        private void saveReadableMedicineData() {
            try (PrintWriter writer = new PrintWriter(new FileWriter(READABLE_DATA_PATH))) {
                writer.println("MEDICINE REMINDER DATA");
                writer.println("======================");
                writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println();
                
                for (String profileName : profiles.keySet()) {
                    writer.println("PROFILE: " + profileName);
                    writer.println("---------------------");
                    
                    for (Medicine med : profiles.get(profileName)) {
                        writer.println("Medicine: " + med.name);
                        writer.println("Dosage: " + med.dosage);
                        writer.println("Patient Age: " + med.age);
                        writer.println("Quantity: " + med.quantity);
                        writer.println("Timing:");
                        writer.println("  Morning: " + (med.morning == null ? "N/A" : med.morning));
                        writer.println("  Afternoon: " + (med.afternoon == null ? "N/A" : med.afternoon));
                        writer.println("  Night: " + (med.night == null ? "N/A" : med.night));
                        writer.println("Meal Timing: " + med.mealTiming);
                        
                        if (!med.missedDoses.isEmpty()) {
                            writer.println("Missed Doses:");
                            for (Map.Entry<String, String> entry : med.missedDoses.entrySet()) {
                                writer.println("  - " + entry.getKey() + " (" + entry.getValue() + ")");
                            }
                        }
                        
                        writer.println("---------------------");
                    }
                    writer.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showNotification("Export Error", "Error exporting readable medicine data.");
            }
        }

        private void showNotification(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        }

        private void styleTextField(TextField textField) {
            textField.setPrefHeight(35);
            textField.setPrefWidth(200);
        }

        private void styleButton(Button button, String baseColor, String hoverColor) {
            button.setStyle(
                "-fx-background-color: " + baseColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 3px;" +
                "-fx-padding: 8px 15px;" +
                "-fx-font-weight: bold;"
            );
            
            button.setOnMouseEntered(e -> 
                button.setStyle(
                    "-fx-background-color: " + hoverColor + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 3px;" +
                    "-fx-padding: 8px 15px;" +
                    "-fx-font-weight: bold;"
                )
            );
            
            button.setOnMouseExited(e -> 
                button.setStyle(
                    "-fx-background-color: " + baseColor + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 3px;" +
                    "-fx-padding: 8px 15px;" +
                    "-fx-font-weight: bold;"
                )
            );
        }

        private void applyInlineStyles(HBox header, VBox sideNav, VBox contentArea) {
            // Style for header
            header.setStyle("-fx-background-color: white; -fx-border-color: #E6E9ED; -fx-border-width: 0 0 1 0;");
            
            // Style for sidebar
            sideNav.setStyle("-fx-background-color: white; -fx-border-color: #E6E9ED; -fx-border-width: 0 1 0 0;");
            
            // Style for content area
            contentArea.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        }

        @Override
        public void stop() {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            saveMedicineData();
            saveReadableMedicineData();
        }

        public static void main(String[] args) {
            launch(args);
        }

        static class Medicine implements Serializable {
            private static final long serialVersionUID = 1L;
            
            String name;
            String dosage;
            String age;
            int quantity;
            String morning;
            String afternoon;
            String night;
            String mealTiming;
            Map<String, String> missedDoses; // key = time, value = timeOfDay (Morning/Afternoon/Night)

            public Medicine(String name, String dosage, String age, int quantity, 
                          String morning, String afternoon, String night, String mealTiming) {
                this.name = name;
                this.dosage = dosage;
                this.age = age;
                this.quantity = quantity;
                this.morning = morning;
                this.afternoon = afternoon;
                this.night = night;
                this.mealTiming = mealTiming;
                this.missedDoses = new HashMap<>();
            }
        }
        
}
