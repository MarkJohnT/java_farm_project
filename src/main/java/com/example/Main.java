package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import com.example.models.Farmer;
import com.example.models.Product;
import com.example.models.ProductCategory;
import com.example.models.ProductReview;
import com.example.models.Customer;
import com.example.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.example.models.Customer;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.example.EmailUtil;
import jakarta.mail.MessagingException;
import java.util.Random;

// Payment system imports
import com.example.models.PaymentMethod;
import com.example.models.Transaction;
import com.example.services.PaymentService;
import com.example.ui.AnalyticsDashboard;

public class Main extends Application {

    private BorderPane root;
    private StackPane leftContentPane;
    private Scene scene;
    private Stage primaryStage;
    private ObservableList<Product> productsList;
    private TableView<Product> productsTable;
    private Farmer demoFarmer;

    private static final String COLOR_PRIMARY_GREEN = "#22c55e"; // fresh green accent
    private static final String COLOR_BLACK = "#000000";
    private static final String COLOR_WHITE = "#ffffff";
    private static final String COLOR_GRAY_TEXT = "#6b7280";

    private List<CartItem> cartItems = new ArrayList<>();
    private Label cartBadge;
    private Customer currentCustomer;

    private static Connection dbConnection;

    private static class CartItem {
        private String name;
        private double price;
        private String unit;
        private int quantity;

        public CartItem(String name, double price, String unit, int quantity) {
            this.name = name;
            this.price = price;
            this.unit = unit;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public String getUnit() {
            return unit;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    private void updateCartBadge() {
        if (cartBadge != null) {
            int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
            cartBadge.setText(String.valueOf(totalItems));
            cartBadge.setVisible(totalItems > 0);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.productsList = FXCollections.observableArrayList();

        // Initialize H2 database connection
        try {
            dbConnection = DriverManager.getConnection("jdbc:h2:~/farmers_customers_db;MODE=MySQL", "sa", "");
            System.out.println("H2 database connected successfully.");

            // Drop existing tables and recreate them to ensure correct schema
            String dropFarmerTable = "DROP TABLE IF EXISTS Farmer";
            String dropCustomerTable = "DROP TABLE IF EXISTS Customer";
            String dropProductTable = "DROP TABLE IF EXISTS Product";
            
            Statement stmt = dbConnection.createStatement();
            stmt.execute(dropFarmerTable);
            stmt.execute(dropCustomerTable);
            stmt.execute(dropProductTable);

            // Create tables with correct schema
            String createFarmerTable = "CREATE TABLE IF NOT EXISTS Farmer (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(50), " +
                    "farmName VARCHAR(255), " +
                    "farmLocation VARCHAR(255), " +
                    "passwordHash VARCHAR(255), " +
                    "passwordSalt VARCHAR(255), " +
                    "verified BOOLEAN DEFAULT FALSE, " +
                    "deactivated BOOLEAN DEFAULT FALSE" +
                    ")";
            String createCustomerTable = "CREATE TABLE IF NOT EXISTS Customer (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(50), " +
                    "address VARCHAR(255), " +
                    "passwordHash VARCHAR(255), " +
                    "passwordSalt VARCHAR(255), " +
                    "verified BOOLEAN DEFAULT FALSE, " +
                    "deactivated BOOLEAN DEFAULT FALSE, " +
                    "isAdmin BOOLEAN DEFAULT FALSE" +
                    ")";
            String createProductTable = "CREATE TABLE IF NOT EXISTS Product (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "price DOUBLE, " +
                    "description VARCHAR(1024), " +
                    "unit VARCHAR(50), " +
                    "quantity INT, " +
                    "farmerId BIGINT, " +
                    "imagePath VARCHAR(255), " +
                    "FOREIGN KEY (farmerId) REFERENCES Farmer(id)" +
                    ")";
            String createOrderTable = "CREATE TABLE IF NOT EXISTS Orders (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customerId BIGINT, " +
                    "productId BIGINT, " +
                    "quantity INT, " +
                    "orderDate TIMESTAMP, " +
                    "status VARCHAR(50), " +
                    "FOREIGN KEY (customerId) REFERENCES Customer(id), " +
                    "FOREIGN KEY (productId) REFERENCES Product(id)" +
                    ")";
            String createReportsTable = "CREATE TABLE IF NOT EXISTS Reports (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "reporterUsername VARCHAR(255), " +
                "reportedUsername VARCHAR(255), " +
                "role VARCHAR(50), " +
                "reason VARCHAR(1024), " +
                "status VARCHAR(50) DEFAULT 'Pending', " +
                "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            stmt.execute(createFarmerTable);
            stmt.execute(createCustomerTable);
            stmt.execute(createProductTable);
            stmt.execute(createOrderTable);
            stmt.execute(createReportsTable);
            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to H2 database or create tables: " + e.getMessage());
            return;
        }

        // Create demo farmer
        this.demoFarmer = new Farmer(
                "John Smith",
                "johnsmith",
                "john@organicfarm.com",
                "555-0123",
                "Green Valley Organic Farm",
                "123 Farm Road, Green Valley, CA 90210",
                "password");

        // Add a demo product
        Product demoProduct = new Product(
                "Organic Tomatoes",
                4.99,
                "Fresh organic tomatoes grown with care. Perfect for salads and cooking.",
                "kg",
                50,
                demoFarmer.getId());
        demoProduct.setImagePath("/com/example/images/download.jpeg");
        productsList.add(demoProduct);

        root = new BorderPane();

        // --- Splash Animation Pane ---
        StackPane splashPane = new StackPane();
        splashPane.setStyle("-fx-background-color: white;");
        ImageView splashLogo = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            splashLogo = new ImageView(logo);
            splashLogo.setFitHeight(160);
            splashLogo.setFitWidth(160);
            splashLogo.setPreserveRatio(true);
            splashLogo.setSmooth(true);
            splashLogo.setCache(true);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (splashLogo != null)
            splashPane.getChildren().add(splashLogo);
        splashPane.setAlignment(Pos.CENTER);

        Scene splashScene = new Scene(splashPane, 1060, 600);
        try {
            splashScene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }
        primaryStage.setScene(splashScene);
        primaryStage.setTitle("Farmers & Customers Interaction App");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(560);
        primaryStage.show();

        // Animate splash (fade in, then fade out, then show login)
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600),
                splashPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);
        fadeIn.setOnFinished(ev -> {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2.4));
            pause.setOnFinished(ev2 -> {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(600), splashPane);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setCycleCount(1);
                fadeOut.setOnFinished(ev3 -> {
                    // After animation, show main login as pane
                    leftContentPane = new StackPane();
                    leftContentPane.setPadding(new Insets(32));
                    leftContentPane.setMaxWidth(520);
                    leftContentPane.getStyleClass().add("container-box");
                    leftContentPane.getChildren().add(createLoginChoicePane());

                    VBox leftPane = new VBox(leftContentPane);
                    leftPane.setPadding(new Insets(40));
                    leftPane.setAlignment(Pos.CENTER);
                    leftPane.setPrefWidth(540);
                    leftPane.setStyle("-fx-background-color: " + COLOR_WHITE + ";");
                    BorderPane.setAlignment(leftPane, Pos.CENTER);
                    root.setLeft(leftPane);

                    // Load image from resources
                    try {
                        Image localImage = new Image(
                                getClass().getResourceAsStream("/com/example/images/greenfield.jpeg"));
                        ImageView imageView = new ImageView(localImage);
                        imageView.setSmooth(true);
                        imageView.setCache(true);

                        StackPane rightPane = new StackPane(imageView);
                        rightPane.setStyle("-fx-background-color: #e6f7ff;"); // subtle soft background for contrast
                        rightPane.setPrefWidth(640);
                        rightPane.setAlignment(Pos.CENTER);
                        rightPane.setPadding(new Insets(40));
                        imageView.fitWidthProperty().bind(rightPane.widthProperty().subtract(100));
                        imageView.fitHeightProperty().bind(rightPane.heightProperty().subtract(80));
                        imageView.setPreserveRatio(false);
                        BorderPane.setAlignment(rightPane, Pos.CENTER);
                        root.setRight(rightPane);
                    } catch (Exception e) {
                        // If image loading fails, create a simple colored background
                        StackPane rightPane = new StackPane();
                        rightPane.setStyle("-fx-background-color: #e6f7ff;");
                        rightPane.setPrefWidth(640);
                        BorderPane.setAlignment(rightPane, Pos.CENTER);
                        root.setRight(rightPane);
                    }

                    scene = new Scene(root, 1060, 600);
                    try {
                        scene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
                    } catch (Exception e) {
                        System.err.println("Could not load styles.css: " + e.getMessage());
                    }

                    primaryStage.setScene(scene);
                });
                fadeOut.play();
            });
            pause.play();
        });
        fadeIn.play();
    }

    private VBox createLoginChoicePane() {
        VBox container = new VBox(28); // more spacious spacing
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.setAlignment(Pos.CENTER);

        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(128, 128);
        logoCircle.setMaxSize(128, 128);
        logoCircle.setMinSize(128, 128);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 64;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(110);
            logoView.setFitWidth(110);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 18, 0));

        Label title = new Label("Login as");
        title.getStyleClass().add("title-label");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 22px;");

        Button customerBtn = new Button("Customer");
        Button farmerBtn = new Button("Farmer");
        customerBtn.getStyleClass().add("button-primary");
        farmerBtn.getStyleClass().add("button-primary");
        customerBtn.setMinWidth(90);
        farmerBtn.setMinWidth(90);
        customerBtn.setMaxWidth(Double.MAX_VALUE);
        farmerBtn.setMaxWidth(Double.MAX_VALUE);
        customerBtn.setAlignment(Pos.CENTER);
        farmerBtn.setAlignment(Pos.CENTER);
        customerBtn.setPrefHeight(40);
        farmerBtn.setPrefHeight(40);
        customerBtn.setStyle("-fx-font-size: 16px;");
        farmerBtn.setStyle("-fx-font-size: 16px;");

        VBox buttonBox = new VBox(16, customerBtn, farmerBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(Double.MAX_VALUE);

        Label signUpLink = new Label("Don't have an account? Sign up");
        signUpLink.getStyleClass().add("link-label");
        signUpLink.setAlignment(Pos.CENTER);
        signUpLink.setMaxWidth(Double.MAX_VALUE);
        signUpLink.setStyle("-fx-font-size: 15px;");

        customerBtn.setOnAction(e -> switchToLoginForm("Customer"));
        farmerBtn.setOnAction(e -> switchToLoginForm("Farmer"));
        signUpLink.setOnMouseClicked(e -> switchToSignUpForm());

        container.getChildren().addAll(
                logoCircle,
                title,
                buttonBox,
                signUpLink);

        // No ScrollPane, just a large, centered VBox
        VBox outer = new VBox(container);
        outer.setAlignment(Pos.CENTER);
        outer.setFillWidth(true);
        VBox.setVgrow(container, Priority.ALWAYS);
        return outer;
    }

    private void switchToLoginForm(String role) {
        VBox form = createLoginForm(role);
        swapLeftContent(form);
    }

    private void switchToSignUpForm() {
        VBox form = createSignUpForm();
        swapLeftContent(form);
    }

    private void showForgotPasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Enter your username and select your role. We'll send a reset code to your email.");
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton customerRadio = new RadioButton("Customer");
        RadioButton farmerRadio = new RadioButton("Farmer");
        customerRadio.setToggleGroup(roleGroup);
        farmerRadio.setToggleGroup(roleGroup);
        customerRadio.setSelected(true);
        
        // Style radio buttons for better visibility
        customerRadio.setStyle("-fx-text-fill: #333333;");
        farmerRadio.setStyle("-fx-text-fill: #333333;");
        
        HBox roleBox = new HBox(16, customerRadio, farmerRadio);
        Label roleLabel = new Label("Role:");
        roleLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        VBox content = new VBox(10, userLabel, usernameField, roleLabel, roleBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResizable(true);
        dialog.setWidth(350);
        dialog.setHeight(220);
        var result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        String username = usernameField.getText().trim();
        boolean isFarmer = farmerRadio.isSelected();
        String email = null;
        try {
            var stmt = dbConnection.prepareStatement(
                isFarmer ? "SELECT * FROM Farmer WHERE username = ?" : "SELECT * FROM Customer WHERE username = ?");
            stmt.setString(1, username);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                email = rs.getString("email");
            } else {
                showAlert("Not Found", "No user found with that username.");
                return;
            }
        } catch (Exception ex) {
            showAlert("Error", "Database error: " + ex.getMessage());
            return;
        }
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        try {
            EmailUtil.sendVerificationEmail(email, code);
        } catch (Exception ex) {
            showAlert("Error", "Failed to send email: " + ex.getMessage());
            return;
        }
        TextInputDialog codeDialog = new TextInputDialog();
        codeDialog.setTitle("Enter Reset Code");
        codeDialog.setHeaderText("A reset code has been sent to your email. Enter it below:");
        codeDialog.setContentText("Reset Code:");
        var codeResult = codeDialog.showAndWait();
        if (codeResult.isEmpty() || !codeResult.get().trim().equals(code)) {
            showAlert("Reset Failed", "Incorrect code. Password reset cancelled.");
            return;
        }
        Dialog<ButtonType> passDialog = new Dialog<>();
        passDialog.setTitle("Set New Password");
        passDialog.setHeaderText("Enter your new password.");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm Password");
        
        Label newPassLabel = new Label("New Password:");
        newPassLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label confirmPassLabel = new Label("Confirm Password:");
        confirmPassLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        VBox passBox = new VBox(10, newPassLabel, newPass, confirmPassLabel, confirmPass);
        passDialog.getDialogPane().setContent(passBox);
        passDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        var passResult = passDialog.showAndWait();
        if (passResult.isEmpty() || passResult.get() != ButtonType.OK) return;
        if (!newPass.getText().equals(confirmPass.getText()) || newPass.getText().isEmpty()) {
            showAlert("Error", "Passwords do not match or are empty.");
            return;
        }
        try {
            String updateSql = isFarmer ?
                "UPDATE Farmer SET passwordHash = ?, passwordSalt = ? WHERE username = ?" :
                "UPDATE Customer SET passwordHash = ?, passwordSalt = ? WHERE username = ?";
            var stmt = dbConnection.prepareStatement(updateSql);
            String salt, hash;
            if (isFarmer) {
                com.example.models.Farmer f = new com.example.models.Farmer("", username, email, "", "", "", "");
                f.setPassword(newPass.getText());
                hash = f.getPasswordHash();
                salt = f.getPasswordSalt();
            } else {
                com.example.models.Customer c = new com.example.models.Customer("", username, email, "", "");
                c.setPassword(newPass.getText());
                hash = c.getPasswordHash();
                salt = c.getPasswordSalt();
            }
            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.setString(3, username);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                showAlert("Success", "Password reset successfully!");
            } else {
                showAlert("Error", "Failed to update password.");
            }
        } catch (Exception ex) {
            showAlert("Error", "Failed to update password: " + ex.getMessage());
        }
    }

    private void swapLeftContent(VBox newContent) {
        leftContentPane.getChildren().clear();
        leftContentPane.getChildren().add(newContent);
    }

    private VBox createLoginForm(String role) {
        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(128, 128);
        logoCircle.setMaxSize(128, 128);
        logoCircle.setMinSize(128, 128);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 64;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(110);
            logoView.setFitWidth(110);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 18, 0));

        // --- Minimized login form ---
        VBox container = new VBox(10); // reduced spacing
        container.setFillWidth(true);
        container.setMaxWidth(320);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("container-box");

        Label header = new Label(role + " Login");
        header.getStyleClass().add("title-label");
        VBox.setMargin(header, new Insets(0, 0, 6, 0));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setPrefHeight(32);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setPrefHeight(32);

        final TextField farmNameField = "Farmer".equals(role) ? new TextField() : null;
        if (farmNameField != null) {
            farmNameField.setPromptText("Farm Name");
            farmNameField.setMaxWidth(Double.MAX_VALUE);
            farmNameField.setPrefHeight(32);
            container.getChildren().addAll(header, usernameField, farmNameField, passwordField);
        } else {
            container.getChildren().addAll(header, usernameField, passwordField);
        }

        // Add 'Forgot Password?' link
        Label forgotPasswordLink = new Label("Forgot Password?");
        forgotPasswordLink.getStyleClass().add("link-label");
        forgotPasswordLink.setStyle("-fx-font-size: 14px; -fx-text-fill: #22c55e; -fx-underline: true; -fx-cursor: hand;");
        forgotPasswordLink.setOnMouseClicked(e -> showForgotPasswordDialog());
        forgotPasswordLink.setMaxWidth(Double.MAX_VALUE);
        forgotPasswordLink.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(forgotPasswordLink, new Insets(0, 0, 0, 0));
        container.getChildren().add(forgotPasswordLink);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("button-black");
        loginBtn.setMaxWidth(160);
        loginBtn.setMinWidth(120);
        loginBtn.setPrefHeight(34);
        VBox.setMargin(loginBtn, new Insets(10, 0, 0, 0));

        // Add login button action
        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Login Failed", "Please enter both username and password.");
                return;
            }
            
            authenticateUser(role, username, password);
        });

        Label backLink = new Label("← Back");
        backLink.getStyleClass().add("link-label");
        backLink.setOnMouseClicked(e -> swapLeftContent(createLoginChoicePane()));
        backLink.setMaxWidth(Double.MAX_VALUE);
        backLink.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(backLink, new Insets(8, 0, 0, 0));

        container.getChildren().addAll(loginBtn, backLink);

        VBox outer = new VBox(logoCircle, container);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setSpacing(8);
        return outer;
    }

    /**
     * Enhanced authentication method with Two-Factor Authentication support
     */
    private void authenticateUser(String role, String username, String password) {
        try {
            if ("Farmer".equals(role)) {
                var stmt = dbConnection.prepareStatement("SELECT * FROM Farmer WHERE username = ?");
                stmt.setString(1, username);
                var rs = stmt.executeQuery();
                
                if (rs.next()) {
                    Farmer farmer = new Farmer(
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("farmName"),
                        rs.getString("farmLocation"),
                        "");
                    farmer.setPasswordHash(rs.getString("passwordHash"));
                    farmer.setPasswordSalt(rs.getString("passwordSalt"));
                    // Set the database ID to the farmer object
                    farmer.setId(String.valueOf(rs.getLong("id")));
                    
                    // Check if account is locked
                    if (farmer.isAccountLocked()) {
                        showAlert("Account Locked", 
                            "Your account has been locked due to multiple failed login attempts. " +
                            "Please try again later or contact support.");
                        return;
                    }
                    
                    if (farmer.checkPassword(password)) {
                        // Reset failed login attempts on successful password verification
                        farmer.resetFailedLoginAttempts();
                        
                        // Check if Two-Factor Authentication is enabled
                        if (farmer.isTwoFactorEnabled()) {
                            showTwoFactorDialog(farmer, () -> {
                                farmer.setLastLoginDate(java.time.LocalDateTime.now());
                                showFarmerDashboard(farmer);
                            });
                        } else {
                            farmer.setLastLoginDate(java.time.LocalDateTime.now());
                            showFarmerDashboard(farmer);
                        }
                    } else {
                        farmer.incrementFailedLoginAttempts();
                        String lockoutMessage = farmer.isAccountLocked() ? 
                            " Account has been locked for 30 minutes due to multiple failed attempts." : "";
                        showAlert("Login Failed", "Incorrect password." + lockoutMessage);
                    }
                } else {
                    showAlert("Login Failed", "No farmer found with that username.");
                }
            } else {
                var stmt = dbConnection.prepareStatement("SELECT * FROM Customer WHERE username = ?");
                stmt.setString(1, username);
                var rs = stmt.executeQuery();
                
                if (rs.next()) {
                    Customer customer = new Customer(
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"));
                    customer.setPasswordHash(rs.getString("passwordHash"));
                    customer.setPasswordSalt(rs.getString("passwordSalt"));
                    customer.setAdmin(rs.getBoolean("isAdmin"));
                    // Set the database ID to the customer object
                    customer.setId(String.valueOf(rs.getLong("id")));
                    
                    // Check if account is locked
                    if (customer.isAccountLocked()) {
                        showAlert("Account Locked", 
                            "Your account has been locked due to multiple failed login attempts. " +
                            "Please try again later or contact support.");
                        return;
                    }
                    
                    if (customer.checkPassword(password)) {
                        // Reset failed login attempts on successful password verification
                        customer.resetFailedLoginAttempts();
                        
                        // Check if Two-Factor Authentication is enabled
                        if (customer.isTwoFactorEnabled()) {
                            showTwoFactorDialog(customer, () -> {
                                customer.setLastLoginDate(java.time.LocalDateTime.now());
                                if (customer.isAdmin()) {
                                    showAdminDashboard(customer);
                                } else {
                                    showCustomerDashboard(customer);
                                }
                            });
                        } else {
                            customer.setLastLoginDate(java.time.LocalDateTime.now());
                            if (customer.isAdmin()) {
                                showAdminDashboard(customer);
                            } else {
                                showCustomerDashboard(customer);
                            }
                        }
                    } else {
                        customer.incrementFailedLoginAttempts();
                        String lockoutMessage = customer.isAccountLocked() ? 
                            " Account has been locked for 30 minutes due to multiple failed attempts." : "";
                        showAlert("Login Failed", "Incorrect password." + lockoutMessage);
                    }
                } else {
                    showAlert("Login Failed", "No customer found with that username.");
                }
            }
        } catch (Exception ex) {
            showAlert("Error", "Login error: " + ex.getMessage());
        }
    }

    /**
     * Show Two-Factor Authentication dialog
     */
    private void showTwoFactorDialog(User user, Runnable onSuccess) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Two-Factor Authentication");
        dialog.setHeaderText("Enter your 6-digit authentication code");
        
        // Set the button types
        ButtonType loginButtonType = new ButtonType("Verify", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // Create the 2FA code input field
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setMaxWidth(120);
        codeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                codeField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 6) {
                codeField.setText(oldValue);
            }
        });
        
        Label infoLabel = new Label("Open your authenticator app and enter the 6-digit code:");
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(300);
        
        Label backupLabel = new Label("Lost your device? Contact support for backup codes.");
        backupLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        backupLabel.setWrapText(true);
        backupLabel.setMaxWidth(300);
        
        grid.add(infoLabel, 0, 0, 2, 1);
        grid.add(new Label("Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(backupLabel, 0, 2, 2, 1);
        
        Node verifyButton = dialog.getDialogPane().lookupButton(loginButtonType);
        verifyButton.setDisable(true);
        
        // Enable/Disable verify button based on code length
        codeField.textProperty().addListener((observable, oldValue, newValue) -> {
            verifyButton.setDisable(newValue.trim().length() != 6);
        });
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the code field by default
        Platform.runLater(() -> codeField.requestFocus());
        
        // Convert the result to a string when the verify button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return codeField.getText();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(code -> {
            // In a real implementation, you would verify the TOTP code here
            // For demonstration, we'll accept any 6-digit code
            if (code.length() == 6 && code.matches("\\d{6}")) {
                // Log successful 2FA login
                System.out.println("Two-factor authentication successful for user: " + user.getUsername());
                
                onSuccess.run();
            } else {
                showAlert("Invalid Code", "Please enter a valid 6-digit authentication code.");
                // Show dialog again for retry
                showTwoFactorDialog(user, onSuccess);
            }
        });
    }

    private void showAdminDashboard(Customer admin) {
        Stage adminStage = new Stage();
        adminStage.setTitle("Admin Dashboard");
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        TabPane tabPane = new TabPane();
        Tab customersTab = new Tab("Customers");
        Tab farmersTab = new Tab("Farmers");
        Tab reportsTab = new Tab("Reports");
        tabPane.getTabs().addAll(customersTab, farmersTab, reportsTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Customers Table
        TableView<Customer> customerTable = new TableView<>();
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Customer, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<Customer, String> cUsername = new TableColumn<>("Username");
        cUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<Customer, String> cEmail = new TableColumn<>("Email");
        cEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        TableColumn<Customer, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isAdmin() ? "Admin" : (isCustomerDeactivated(data.getValue()) ? "Deactivated" : "Active")));
        customerTable.getColumns().addAll(cName, cUsername, cEmail, cStatus);
        customerTable.setItems(getAllCustomers());

        Button banCustomerBtn = new Button("Ban/Unban");
        Button deleteCustomerBtn = new Button("Delete");
        HBox customerActions = new HBox(10, banCustomerBtn, deleteCustomerBtn);
        customerActions.setAlignment(Pos.CENTER);
        customersTab.setContent(new VBox(10, customerTable, customerActions));

        // Farmers Table
        TableView<Farmer> farmerTable = new TableView<>();
        farmerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Farmer, String> fName = new TableColumn<>("Name");
        fName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        TableColumn<Farmer, String> fUsername = new TableColumn<>("Username");
        fUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        TableColumn<Farmer, String> fEmail = new TableColumn<>("Email");
        fEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        TableColumn<Farmer, String> fStatus = new TableColumn<>("Status");
        fStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            isFarmerDeactivated(data.getValue()) ? "Deactivated" : "Active"));
        farmerTable.getColumns().addAll(fName, fUsername, fEmail, fStatus);
        farmerTable.setItems(getAllFarmers());

        Button banFarmerBtn = new Button("Ban/Unban");
        Button deleteFarmerBtn = new Button("Delete");
        HBox farmerActions = new HBox(10, banFarmerBtn, deleteFarmerBtn);
        farmerActions.setAlignment(Pos.CENTER);
        farmersTab.setContent(new VBox(10, farmerTable, farmerActions));

        // Ban/Unban Customer
        banCustomerBtn.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select User", "Please select a customer.");
                return;
            }
            if (selected.isAdmin()) {
                showAlert("Error", "Cannot ban/unban an admin user.");
                return;
            }
            boolean currentlyDeactivated = isCustomerDeactivated(selected);
            String action = currentlyDeactivated ? "Unban" : "Ban";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to " + action + " this customer?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle(action + " Customer");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    var stmt = dbConnection.prepareStatement("UPDATE Customer SET deactivated=? WHERE username=?");
                    stmt.setBoolean(1, !currentlyDeactivated);
                    stmt.setString(2, selected.getUsername());
                    stmt.executeUpdate();
                    customerTable.setItems(getAllCustomers());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update customer: " + ex.getMessage());
                }
            }
        });
        // Delete Customer
        deleteCustomerBtn.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select User", "Please select a customer.");
                return;
            }
            if (selected.isAdmin()) {
                showAlert("Error", "Cannot delete an admin user.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this customer? This cannot be undone.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Delete Customer");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    var stmt = dbConnection.prepareStatement("DELETE FROM Customer WHERE username=?");
                    stmt.setString(1, selected.getUsername());
                    stmt.executeUpdate();
                    customerTable.setItems(getAllCustomers());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to delete customer: " + ex.getMessage());
                }
            }
        });
        // Ban/Unban Farmer
        banFarmerBtn.setOnAction(e -> {
            Farmer selected = farmerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select User", "Please select a farmer.");
                return;
            }
            boolean currentlyDeactivated = isFarmerDeactivated(selected);
            String action = currentlyDeactivated ? "Unban" : "Ban";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to " + action + " this farmer?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle(action + " Farmer");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    var stmt = dbConnection.prepareStatement("UPDATE Farmer SET deactivated=? WHERE username=?");
                    stmt.setBoolean(1, !currentlyDeactivated);
                    stmt.setString(2, selected.getUsername());
                    stmt.executeUpdate();
                    farmerTable.setItems(getAllFarmers());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update farmer: " + ex.getMessage());
                }
            }
        });
        // Delete Farmer
        deleteFarmerBtn.setOnAction(e -> {
            Farmer selected = farmerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select User", "Please select a farmer.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this farmer? This cannot be undone.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Delete Farmer");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    var stmt = dbConnection.prepareStatement("DELETE FROM Farmer WHERE username=?");
                    stmt.setString(1, selected.getUsername());
                    stmt.executeUpdate();
                    farmerTable.setItems(getAllFarmers());
                } catch (Exception ex) {
                    showAlert("Error", "Failed to delete farmer: " + ex.getMessage());
                }
            }
        });

        // Reports Table
        TableView<Report> reportsTable = new TableView<>();
        reportsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Report, String> repReporter = new TableColumn<>("Reporter");
        repReporter.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().reporterUsername));
        TableColumn<Report, String> repReported = new TableColumn<>("Reported User");
        repReported.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().reportedUsername));
        TableColumn<Report, String> repRole = new TableColumn<>("Role");
        repRole.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().role));
        TableColumn<Report, String> repReason = new TableColumn<>("Reason");
        repReason.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().reason));
        TableColumn<Report, String> repStatus = new TableColumn<>("Status");
        repStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status));
        reportsTable.getColumns().addAll(repReporter, repReported, repRole, repReason, repStatus);
        reportsTable.setItems(getAllReports());

        Button banBtn = new Button("Ban/Deactivate User");
        Button ignoreBtn = new Button("Ignore Report");
        HBox reportActions = new HBox(10, banBtn, ignoreBtn);
        reportActions.setAlignment(Pos.CENTER);
        reportsTab.setContent(new VBox(10, reportsTable, reportActions));

        banBtn.setOnAction(e -> {
            Report selected = reportsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select Report", "Please select a report.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Ban/deactivate user '" + selected.reportedUsername + "'?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Ban/Deactivate User");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    if (selected.role.equals("Customer")) {
                        var stmt = dbConnection.prepareStatement("UPDATE Customer SET deactivated=TRUE WHERE username=?");
                        stmt.setString(1, selected.reportedUsername);
                        stmt.executeUpdate();
                    } else if (selected.role.equals("Farmer")) {
                        var stmt = dbConnection.prepareStatement("UPDATE Farmer SET deactivated=TRUE WHERE username=?");
                        stmt.setString(1, selected.reportedUsername);
                        stmt.executeUpdate();
                    }
                    var stmt2 = dbConnection.prepareStatement("UPDATE Reports SET status='Resolved' WHERE id=?");
                    stmt2.setLong(1, selected.id);
                    stmt2.executeUpdate();
                    reportsTable.setItems(getAllReports());
                    showAlert("User Banned", "User has been deactivated and report marked as resolved.");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to ban user: " + ex.getMessage());
                }
            }
        });
        ignoreBtn.setOnAction(e -> {
            Report selected = reportsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select Report", "Please select a report.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Ignore this report?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Ignore Report");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    var stmt = dbConnection.prepareStatement("UPDATE Reports SET status='Ignored' WHERE id=?");
                    stmt.setLong(1, selected.id);
                    stmt.executeUpdate();
                    reportsTable.setItems(getAllReports());
                    showAlert("Report Ignored", "Report has been marked as ignored.");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to ignore report: " + ex.getMessage());
                }
            }
        });

        root.getChildren().addAll(title, tabPane);
        adminStage.setScene(new Scene(root, 800, 600));
        adminStage.show();
    }

    // Helper methods for admin dashboard
    private javafx.collections.ObservableList<Customer> getAllCustomers() {
        javafx.collections.ObservableList<Customer> list = javafx.collections.FXCollections.observableArrayList();
        try {
            var stmt = dbConnection.createStatement();
            var rs = stmt.executeQuery("SELECT * FROM Customer");
            while (rs.next()) {
                Customer c = new Customer(
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("address"));
                c.setPasswordHash(rs.getString("passwordHash"));
                c.setPasswordSalt(rs.getString("passwordSalt"));
                c.setAdmin(rs.getBoolean("isAdmin"));
                list.add(c);
            }
        } catch (Exception ex) {
            showAlert("Error", "Failed to fetch customers: " + ex.getMessage());
        }
        return list;
    }
    private boolean isCustomerDeactivated(Customer c) {
        try {
            var stmt = dbConnection.prepareStatement("SELECT deactivated FROM Customer WHERE username=?");
            stmt.setString(1, c.getUsername());
            var rs = stmt.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
        } catch (Exception ex) {}
        return false;
    }
    private javafx.collections.ObservableList<Farmer> getAllFarmers() {
        javafx.collections.ObservableList<Farmer> list = javafx.collections.FXCollections.observableArrayList();
        try {
            var stmt = dbConnection.createStatement();
            var rs = stmt.executeQuery("SELECT * FROM Farmer");
            while (rs.next()) {
                Farmer f = new Farmer(
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("farmName"),
                    rs.getString("farmLocation"),
                    "");
                f.setPasswordHash(rs.getString("passwordHash"));
                f.setPasswordSalt(rs.getString("passwordSalt"));
                list.add(f);
            }
        } catch (Exception ex) {
            showAlert("Error", "Failed to fetch farmers: " + ex.getMessage());
        }
        return list;
    }
    private boolean isFarmerDeactivated(Farmer f) {
        try {
            var stmt = dbConnection.prepareStatement("SELECT deactivated FROM Farmer WHERE username=?");
            stmt.setString(1, f.getUsername());
            var rs = stmt.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
        } catch (Exception ex) {}
        return false;
    }

    private VBox createSignUpForm() {
        VBox container = new VBox(16);
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.getStyleClass().add("container-box");

        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(120, 120);
        logoCircle.setMaxSize(120, 120);
        logoCircle.setMinSize(120, 120);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 60;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setFitWidth(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(48, 48, 48);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 24, 0));

        Label header = new Label("Create an Account");
        header.getStyleClass().add("title-label");

        Label backLink = new Label("← Back");
        backLink.getStyleClass().add("link-label");
        backLink.setOnMouseClicked(e -> swapLeftContent(createLoginChoicePane()));
        VBox.setMargin(backLink, new Insets(0, 0, 10, 0));

        // Common fields (always visible)
        TextField fullName = new TextField();
        fullName.setPromptText("Full Name");
        fullName.setMaxWidth(Double.MAX_VALUE);

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setMaxWidth(Double.MAX_VALUE);

        TextField email = new TextField();
        email.setPromptText("Email");
        email.setMaxWidth(Double.MAX_VALUE);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setMaxWidth(Double.MAX_VALUE);

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm Password");
        confirmPassword.setMaxWidth(Double.MAX_VALUE);

        // Role selection
        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton customerRadio = new RadioButton("Customer");
        customerRadio.setStyle("-fx-text-fill: #333333;");
        RadioButton farmerRadio = new RadioButton("Farmer");
        farmerRadio.setStyle("-fx-text-fill: #333333;");
        customerRadio.setToggleGroup(roleGroup);
        farmerRadio.setToggleGroup(roleGroup);
        customerRadio.setSelected(true);

        HBox roleBox = new HBox(32, customerRadio, farmerRadio);
        roleBox.setAlignment(Pos.CENTER_LEFT);

        // Customer-specific fields (optional phone and address)
        TextField customerPhone = new TextField();
        customerPhone.setPromptText("Phone Number (Optional)");
        customerPhone.setMaxWidth(Double.MAX_VALUE);

        TextField customerAddress = new TextField();
        customerAddress.setPromptText("Address (Optional)");
        customerAddress.setMaxWidth(Double.MAX_VALUE);

        // Farmer-specific fields (required)
        TextField phoneNumber = new TextField();
        phoneNumber.setPromptText("Phone Number");
        phoneNumber.setMaxWidth(Double.MAX_VALUE);

        TextField farmName = new TextField();
        farmName.setPromptText("Farm Name");
        farmName.setMaxWidth(Double.MAX_VALUE);

        TextField farmLocation = new TextField();
        farmLocation.setPromptText("Farm Location");
        farmLocation.setMaxWidth(Double.MAX_VALUE);

        // VBox to hold dynamic fields based on role selection
        VBox dynamicFieldsContainer = new VBox(16);
        
        // Initially show customer fields (since customer is selected by default)
        dynamicFieldsContainer.getChildren().addAll(customerPhone, customerAddress);

        // Add listeners to radio buttons to show/hide fields
        customerRadio.setOnAction(e -> {
            dynamicFieldsContainer.getChildren().clear();
            dynamicFieldsContainer.getChildren().addAll(customerPhone, customerAddress);
        });

        farmerRadio.setOnAction(e -> {
            dynamicFieldsContainer.getChildren().clear();
            dynamicFieldsContainer.getChildren().addAll(phoneNumber, farmName, farmLocation);
        });

        Button signUpBtn = new Button("Sign Up");
        signUpBtn.getStyleClass().add("button-black");
        signUpBtn.setMaxWidth(205);
        signUpBtn.setMinWidth(205);
        VBox.setMargin(signUpBtn, new Insets(16, 0, 0, 0));

        // Center align the button
        HBox signUpButtonContainer = new HBox(signUpBtn);
        signUpButtonContainer.setAlignment(Pos.CENTER);
        signUpButtonContainer.setMaxWidth(Double.MAX_VALUE);

        signUpBtn.setOnAction(e -> {
            if (customerRadio.isSelected()) {
                // Validate customer form (only basic fields required)
                if (validateCustomerSignUpForm(fullName, username, email, password, confirmPassword)) {
                    String emailToVerify = email.getText();
                    String verificationCode = String.format("%06d", new Random().nextInt(999999));
                    try {
                        EmailUtil.sendVerificationEmail(emailToVerify, verificationCode);
                    } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
                        showAlert("Error", "Failed to send verification email: " + ex.getMessage());
                        return;
                    }
                    TextInputDialog codeDialog = new TextInputDialog();
                    codeDialog.setTitle("Email Verification");
                    codeDialog.setHeaderText("A verification code has been sent to your email. Please enter it below:\n(Temporary: Code is " + verificationCode + " - check spam folder)");
                    codeDialog.setContentText("Verification Code:");
                    var result = codeDialog.showAndWait();
                    if (result.isEmpty() || !result.get().trim().equals(verificationCode)) {
                        showAlert("Verification Failed", "Incorrect verification code. Registration cancelled.");
                        return;
                    }
                    
                    Customer customer = new Customer(
                        fullName.getText(),
                        username.getText(),
                        emailToVerify,
                        customerPhone.getText().isEmpty() ? "" : customerPhone.getText(),
                        customerAddress.getText().isEmpty() ? "" : customerAddress.getText());
                    customer.setPassword(password.getText());
                    try {
                        var stmt = dbConnection.prepareStatement(
                            "INSERT INTO Customer (name, username, email, phone, address, passwordHash, passwordSalt, verified, isAdmin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" );
                        stmt.setString(1, customer.getFullName());
                        stmt.setString(2, customer.getUsername());
                        stmt.setString(3, customer.getEmail());
                        stmt.setString(4, customer.getPhoneNumber());
                        stmt.setString(5, customer.getLocation());
                        stmt.setString(6, customer.getPasswordHash());
                        stmt.setString(7, customer.getPasswordSalt());
                        stmt.setBoolean(8, true);
                        stmt.setBoolean(9, false);
                        stmt.executeUpdate();
                    } catch (Exception ex) {
                        showAlert("Error", "Failed to create customer: " + ex.getMessage());
                        return;
                    }
                    showAlert("Success", "Customer account created and verified!");
                    swapLeftContent(createLoginForm("Customer"));
                }
            } else {
                // Validate farmer form (all fields required)
                if (validateFarmerSignUpForm(fullName, username, email, phoneNumber, farmName, farmLocation,
                        password, confirmPassword)) {
                    String emailToVerify = email.getText();
                    String verificationCode = String.format("%06d", new Random().nextInt(999999));
                    try {
                        EmailUtil.sendVerificationEmail(emailToVerify, verificationCode);
                    } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
                        showAlert("Error", "Failed to send verification email: " + ex.getMessage());
                        return;
                    }
                    TextInputDialog codeDialog = new TextInputDialog();
                    codeDialog.setTitle("Email Verification");
                    codeDialog.setHeaderText("A verification code has been sent to your email. Please enter it below:\n(Temporary: Code is " + verificationCode + " - check spam folder)");
                    codeDialog.setContentText("Verification Code:");
                    var result = codeDialog.showAndWait();
                    if (result.isEmpty() || !result.get().trim().equals(verificationCode)) {
                        showAlert("Verification Failed", "Incorrect verification code. Registration cancelled.");
                        return;
                    }
                    
                    Farmer farmer = new Farmer(
                            fullName.getText(),
                            username.getText(),
                            emailToVerify,
                            phoneNumber.getText(),
                            farmName.getText(),
                            farmLocation.getText(),
                            password.getText());
                    try {
                        var stmt = dbConnection.prepareStatement(
                            "INSERT INTO Farmer (name, username, email, phone, farmName, farmLocation, passwordHash, passwordSalt, verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" );
                        stmt.setString(1, farmer.getFullName());
                        stmt.setString(2, farmer.getUsername());
                        stmt.setString(3, farmer.getEmail());
                        stmt.setString(4, farmer.getPhoneNumber());
                        stmt.setString(5, farmer.getFarmName());
                        stmt.setString(6, farmer.getFarmLocation());
                        stmt.setString(7, farmer.getPasswordHash());
                        stmt.setString(8, farmer.getPasswordSalt());
                        stmt.setBoolean(9, true);
                        stmt.executeUpdate();
                    } catch (Exception ex) {
                        showAlert("Error", "Failed to create farmer: " + ex.getMessage());
                        return;
                    }
                    showAlert("Success", "Farmer account created and verified!");
                    swapLeftContent(createLoginForm("Farmer"));
                }
            }
        });

        container.getChildren().addAll(
                backLink,
                header,
                fullName,
                username,
                email,
                password,
                confirmPassword,
                new Label("Sign up as:"),
                roleBox,
                dynamicFieldsContainer,
                signUpButtonContainer);

        // Wrap the container in a modern ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStyleClass().add("modern-scroll-pane"); // <-- Add this style class

        VBox outer = new VBox(logoCircle, scrollPane);
        outer.setAlignment(Pos.TOP_CENTER);
        return outer;
        // Reminder: Add modern scrollbar styles to styles.css for .modern-scroll-pane
    }

    private boolean validateCustomerSignUpForm(TextField fullName, TextField username, TextField email,
            PasswordField password, PasswordField confirmPassword) {
        if (fullName.getText().isEmpty() || username.getText().isEmpty() ||
                email.getText().isEmpty() || password.getText().isEmpty() || 
                confirmPassword.getText().isEmpty()) {
            showAlert("Error", "Please fill in all required fields (Name, Username, Email, Password)");
            return false;
        }

        if (!password.getText().equals(confirmPassword.getText())) {
            showAlert("Error", "Passwords do not match");
            return false;
        }

        if (!isValidEmail(email.getText())) {
            showAlert("Error", "Please enter a valid email address");
            return false;
        }

        return true;
    }

    private boolean validateFarmerSignUpForm(TextField fullName, TextField username, TextField email,
            TextField phoneNumber, TextField farmName, TextField farmLocation,
            PasswordField password, PasswordField confirmPassword) {
        if (fullName.getText().isEmpty() || username.getText().isEmpty() ||
                email.getText().isEmpty() || phoneNumber.getText().isEmpty() ||
                farmName.getText().isEmpty() || farmLocation.getText().isEmpty() ||
                password.getText().isEmpty() || confirmPassword.getText().isEmpty()) {
            showAlert("Error", "Please fill in all fields");
            return false;
        }

        if (!password.getText().equals(confirmPassword.getText())) {
            showAlert("Error", "Passwords do not match");
            return false;
        }

        if (!isValidEmail(email.getText())) {
            showAlert("Error", "Please enter a valid email address");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showFarmerDashboard(Farmer farmer) {
        BorderPane dashboardRoot = new BorderPane();
        dashboardRoot.setStyle("-fx-background-color: #f5f5f5;");

        // Top Navigation Bar
        HBox topBar = createTopBar(farmer);
        dashboardRoot.setTop(topBar);

        // Left Sidebar
        VBox sidebar = createSidebar();
        dashboardRoot.setLeft(sidebar);

        // Main Content Area with ScrollPane
        ScrollPane scrollPane = new ScrollPane(createMainContent(farmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dashboardRoot.setCenter(scrollPane);

        Scene dashboardScene = new Scene(dashboardRoot, 1200, 800);
        dashboardScene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());

        primaryStage.setTitle("Farmer Dashboard - " + farmer.getFarmName());
        primaryStage.setScene(dashboardScene);
    }

    private HBox createTopBar(Farmer farmer) {
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        topBar.setPadding(new Insets(15));
        topBar.setSpacing(20);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // --- Add logo at the left ---
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            HBox.setMargin(logoView, new Insets(0, 18, 0, 0));
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }

        Label welcomeLabel = new Label("Welcome, " + farmer.getFullName());
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create profile picture container
        StackPane profileContainer = new StackPane();
        profileContainer.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 20;");
        profileContainer.setPrefSize(40, 40);

        // Create profile picture
        ImageView profilePicture = new ImageView();
        try {
            Image profileImage = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profilePicture.setImage(profileImage);
        } catch (Exception e) {
            // If image loading fails, show initials
            Label initials = new Label(farmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 20));
            initials.setTextFill(Color.WHITE);
            profileContainer.getChildren().add(initials);
        }

        profilePicture.setFitWidth(40);
        profilePicture.setFitHeight(40);
        profilePicture.setPreserveRatio(true);
        profilePicture.setSmooth(true);
        profileContainer.getChildren().add(profilePicture);

        // Add hover effect
        profileContainer.setOnMouseEntered(e -> {
            profileContainer.setStyle("-fx-background-color: #d1d5db; -fx-background-radius: 20; -fx-cursor: hand;");
        });
        profileContainer.setOnMouseExited(e -> {
            profileContainer.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 20;");
        });

        // Add click handler to show profile menu
        profileContainer.setOnMouseClicked(e -> showProfileMenu(profileContainer, farmer));

        if (logoView != null) {
            topBar.getChildren().add(logoView);
        }
        topBar.getChildren().addAll(welcomeLabel, spacer, profileContainer);
        return topBar;
    }

    private void showProfileMenu(StackPane profileContainer, Farmer farmer) {
        ContextMenu profileMenu = new ContextMenu();

        MenuItem viewProfile = new MenuItem("View Profile");
        MenuItem editProfile = new MenuItem("Edit Profile");
        MenuItem analytics = new MenuItem("Analytics Dashboard");
        MenuItem settings = new MenuItem("Settings");
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("Logout");

        // Style menu items
        String menuItemStyle = "-fx-font-family: 'Roboto'; -fx-font-size: 14px; -fx-padding: 8 16;";
        viewProfile.setStyle(menuItemStyle);
        editProfile.setStyle(menuItemStyle);
        analytics.setStyle(menuItemStyle);
        settings.setStyle(menuItemStyle);
        logout.setStyle(menuItemStyle);

        // Add action handlers
        viewProfile.setOnAction(e -> showProfileDetails(farmer));
        editProfile.setOnAction(e -> showEditProfileDialog(farmer));
        analytics.setOnAction(e -> showAnalyticsDashboard());
        settings.setOnAction(e -> showSettingsDialog());
        logout.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        profileMenu.getItems().addAll(viewProfile, editProfile, analytics, settings, separator, logout);
        profileMenu.show(profileContainer, Side.BOTTOM, 0, 0);
    }

    private void showProfileDetails(Farmer farmer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Profile Details");
        dialog.setHeaderText(farmer.getFullName() + "'s Profile");
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        // Profile picture
        StackPane profilePicture = new StackPane();
        profilePicture.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 50;");
        profilePicture.setPrefSize(100, 100);

        ImageView profileImage = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profileImage.setImage(image);
        } catch (Exception e) {
            Label initials = new Label(farmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 40));
            initials.setTextFill(Color.WHITE);
            profilePicture.getChildren().add(initials);
        }

        profileImage.setFitWidth(100);
        profileImage.setFitHeight(100);
        profileImage.setPreserveRatio(true);
        profileImage.setSmooth(true);
        profilePicture.getChildren().add(profileImage);

        // Profile details
        VBox details = new VBox(10);
        details.setStyle("-fx-font-family: 'Roboto';");

        Label nameLabel = new Label("Name: " + farmer.getFullName());
        Label farmLabel = new Label("Farm: " + farmer.getFarmName());
        Label emailLabel = new Label("Email: " + farmer.getEmail());
        Label phoneLabel = new Label("Phone: " + farmer.getPhoneNumber());
        Label locationLabel = new Label("Location: " + farmer.getFarmLocation());

        nameLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        farmLabel.setFont(Font.font("Roboto", 14));
        emailLabel.setFont(Font.font("Roboto", 14));
        phoneLabel.setFont(Font.font("Roboto", 14));
        locationLabel.setFont(Font.font("Roboto", 14));

        details.getChildren().addAll(nameLabel, farmLabel, emailLabel, phoneLabel, locationLabel);

        content.getChildren().addAll(profilePicture, details);
        Button reportBtn = new Button("Report User");
        reportBtn.getStyleClass().add("button-danger");
        reportBtn.setOnAction(e -> {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Report User");
            reasonDialog.setHeaderText("Why are you reporting this user?");
            reasonDialog.setContentText("Reason:");
            var result = reasonDialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                try {
                    var stmt = dbConnection.prepareStatement("INSERT INTO Reports (reporterUsername, reportedUsername, role, reason) VALUES (?, ?, ?, ?)");
                    stmt.setString(1, currentCustomer != null ? currentCustomer.getUsername() : "");
                    stmt.setString(2, farmer.getUsername());
                    stmt.setString(3, "Farmer");
                    stmt.setString(4, result.get().trim());
                    stmt.executeUpdate();
                    showAlert("Reported", "User has been reported to the admin.");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to submit report: " + ex.getMessage());
                }
            }
        });
        content.getChildren().add(reportBtn);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.showAndWait();
    }

    private void showEditProfileDialog(Farmer farmer) {
        Dialog<Farmer> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your profile information");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(farmer.getFullName());
        TextField farmField = new TextField(farmer.getFarmName());
        TextField emailField = new TextField(farmer.getEmail());
        TextField phoneField = new TextField(farmer.getPhoneNumber());
        TextField locationField = new TextField(farmer.getFarmLocation());

        // Create labels with proper text color for farmer profile editing
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label farmLabel = new Label("Farm:");
        farmLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label phoneLabel = new Label("Phone:");
        phoneLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label locationLabel = new Label("Location:");
        locationLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(farmLabel, 0, 1);
        grid.add(farmField, 1, 1);
        grid.add(emailLabel, 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(phoneLabel, 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(locationLabel, 0, 4);
        grid.add(locationField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Validation
                if (nameField.getText().trim().isEmpty() || farmField.getText().trim().isEmpty() ||
                    emailField.getText().trim().isEmpty() || phoneField.getText().trim().isEmpty() ||
                    locationField.getText().trim().isEmpty()) {
                    showAlert("Validation Error", "All fields are required.");
                    return null;
                }
                if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    showAlert("Validation Error", "Invalid email format.");
                    return null;
                }
                if (!phoneField.getText().matches("^[0-9\\-+() ]{7,}$")) {
                    showAlert("Validation Error", "Invalid phone number format.");
                    return null;
                }
                // Update DB
                try {
                    var stmt = dbConnection.prepareStatement(
                        "UPDATE Farmer SET name=?, farmName=?, email=?, phone=?, farmLocation=? WHERE username=?");
                    stmt.setString(1, nameField.getText().trim());
                    stmt.setString(2, farmField.getText().trim());
                    stmt.setString(3, emailField.getText().trim());
                    stmt.setString(4, phoneField.getText().trim());
                    stmt.setString(5, locationField.getText().trim());
                    stmt.setString(6, farmer.getUsername());
                    stmt.executeUpdate();
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update profile: " + ex.getMessage());
                    return null;
                }
                farmer.setFullName(nameField.getText());
                farmer.setFarmName(farmField.getText());
                farmer.setEmail(emailField.getText());
                farmer.setPhoneNumber(phoneField.getText());
                farmer.setFarmLocation(locationField.getText());
                return farmer;
            }
            return null;
        });

        Optional<Farmer> result = dialog.showAndWait();
        result.ifPresent(updatedFarmer -> {
            showAlert("Success", "Profile updated successfully!");
        });
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Account Settings");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Profile Image Section
        VBox profileImageSection = new VBox(15);
        Label profileImageLabel = new Label("Profile Picture");
        profileImageLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 14));
        
        HBox profileImageContainer = new HBox(15);
        profileImageContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Current profile image display
        ImageView currentProfileImage = new ImageView();
        currentProfileImage.setFitWidth(80);
        currentProfileImage.setFitHeight(80);
        currentProfileImage.setPreserveRatio(true);
        currentProfileImage.setSmooth(true);
        currentProfileImage.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 2; -fx-border-radius: 40;");
        
        // Load current profile image or show default
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.hasProfileImage()) {
            try {
                Image profileImg = new Image("file:" + currentUser.getProfileImagePath());
                currentProfileImage.setImage(profileImg);
            } catch (Exception e) {
                // Use default image if loading fails
                setDefaultProfileImage(currentProfileImage);
            }
        } else {
            setDefaultProfileImage(currentProfileImage);
        }
        
        VBox profileImageButtons = new VBox(10);
        Button uploadImageBtn = new Button("📷 Upload New Image");
        uploadImageBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5;");
        
        Button removeImageBtn = new Button("Remove Image");
        removeImageBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 5;");
        removeImageBtn.setDisable(!currentUser.hasProfileImage());
        
        Label imageStatusLabel = new Label("");
        imageStatusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        // Upload image handler
        uploadImageBtn.setOnAction(e -> {
            File imageFile = ImageUploadService.chooseImageFile(
                (Stage) dialog.getDialogPane().getScene().getWindow(), 
                "Select Profile Image"
            );
            
            if (imageFile != null) {
                ImageUploadService.ValidationResult validation = ImageUploadService.validateImageFile(imageFile);
                if (validation.isValid()) {
                    ImageUploadService.UploadResult uploadResult = ImageUploadService.saveProfileImage(
                        imageFile, currentUser.getId()
                    );
                    
                    if (uploadResult.isSuccess()) {
                        // Update user profile image
                        currentUser.updateProfileImage(
                            uploadResult.getFilePath(),
                            imageFile.getName(),
                            imageFile.length(),
                            "image/jpeg" // You might want to detect this properly
                        );
                        
                        // Update UI
                        try {
                            Image newProfileImg = new Image("file:" + uploadResult.getFilePath());
                            currentProfileImage.setImage(newProfileImg);
                            imageStatusLabel.setText("✓ Profile image updated successfully");
                            imageStatusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                            removeImageBtn.setDisable(false);
                        } catch (Exception ex) {
                            imageStatusLabel.setText("⚠ Image uploaded but preview failed");
                            imageStatusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
                        }
                    } else {
                        imageStatusLabel.setText("✗ " + uploadResult.getMessage());
                        imageStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    }
                } else {
                    imageStatusLabel.setText("✗ " + validation.getMessage());
                    imageStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                }
            }
        });
        
        // Remove image handler
        removeImageBtn.setOnAction(e -> {
            if (currentUser.hasProfileImage()) {
                // Delete the file
                ImageUploadService.deleteImage(currentUser.getProfileImagePath());
                // Update user model
                currentUser.removeProfileImage();
                // Update UI
                setDefaultProfileImage(currentProfileImage);
                imageStatusLabel.setText("Profile image removed");
                imageStatusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                removeImageBtn.setDisable(true);
            }
        });
        
        profileImageButtons.getChildren().addAll(uploadImageBtn, removeImageBtn, imageStatusLabel);
        profileImageContainer.getChildren().addAll(currentProfileImage, profileImageButtons);
        profileImageSection.getChildren().addAll(profileImageLabel, profileImageContainer);

        // Notification Settings
        VBox notificationSection = new VBox(10);
        Label notificationLabel = new Label("Notification Preferences");
        notificationLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 14));
        
        CheckBox emailNotificationsCheck = new CheckBox("Email Notifications");
        CheckBox smsNotificationsCheck = new CheckBox("SMS Notifications");
        CheckBox orderNotificationsCheck = new CheckBox("Order Updates");
        CheckBox promotionalNotificationsCheck = new CheckBox("Promotional Offers");
        
        // Set current values if user exists
        if (currentUser != null) {
            emailNotificationsCheck.setSelected(currentUser.isEmailNotificationsEnabled());
            smsNotificationsCheck.setSelected(currentUser.isSmsNotificationsEnabled());
            orderNotificationsCheck.setSelected(currentUser.isOrderNotificationsEnabled());
            promotionalNotificationsCheck.setSelected(currentUser.isPromotionalNotificationsEnabled());
        }
        
        notificationSection.getChildren().addAll(
            notificationLabel, emailNotificationsCheck, smsNotificationsCheck, 
            orderNotificationsCheck, promotionalNotificationsCheck
        );

        // App Settings
        VBox appSection = new VBox(10);
        Label appLabel = new Label("Application Settings");
        appLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 14));
        
        CheckBox darkModeCheck = new CheckBox("Dark Mode (Coming Soon)");
        darkModeCheck.setDisable(true);
        
        appSection.getChildren().addAll(appLabel, darkModeCheck);

        content.getChildren().addAll(profileImageSection, new Separator(), notificationSection, new Separator(), appSection);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Handle save button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && currentUser != null) {
                // Update notification preferences
                currentUser.updateNotificationPreferences(
                    emailNotificationsCheck.isSelected(),
                    smsNotificationsCheck.isSelected(),
                    orderNotificationsCheck.isSelected(),
                    promotionalNotificationsCheck.isSelected()
                );
                
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Settings Saved");
                    alert.setHeaderText(null);
                    alert.setContentText("Your settings have been saved successfully!");
                    alert.showAndWait();
                });
            }
            return null;
        });

        dialog.showAndWait();
    }
    
    private User getCurrentUser() {
        // Return the current logged-in user (farmer or customer)
        // This is a placeholder - you might want to implement proper user session management
        return demoFarmer; // or demoCustomer depending on who's logged in
    }
    
    private void setDefaultProfileImage(ImageView imageView) {
        try {
            // Try to load a default profile image
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/example/images/default-profile.png"));
            if (defaultImage.isError()) {
                // If default image doesn't exist, create a simple colored circle
                imageView.setImage(null);
                imageView.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 40;");
            } else {
                imageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            // Fallback: just show empty with background color
            imageView.setImage(null);
            imageView.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 40;");
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);

        Label menuLabel = new Label("Menu");
        menuLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        VBox.setMargin(menuLabel, new Insets(0, 0, 20, 0));

        Button dashboardBtn = createMenuButton("Dashboard", true);
        Button productsBtn = createMenuButton("Products", false);
        Button ordersBtn = createMenuButton("Orders", false);
        Button messagesBtn = createMenuButton("Messages", false);
        Button settingsBtn = createMenuButton("Settings", false);

        // Add click handler for dashboard button
        dashboardBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show dashboard page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createDashboardContent(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for products button
        productsBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show products page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createMainContent(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for orders button
        ordersBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show orders page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createOrdersPage(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for messages button
        messagesBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show messages page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createMessagesPage());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for settings button
        settingsBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");

            // Show settings page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createSettingsPage());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        sidebar.getChildren().addAll(
                menuLabel,
                dashboardBtn,
                productsBtn,
                ordersBtn,
                messagesBtn,
                settingsBtn,
                spacer,
                logoutBtn);

        return sidebar;
    }

    private Button createMenuButton(String text, boolean isSelected) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setFont(Font.font("Roboto", 14));
        if (isSelected) {
            button.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
        } else {
            button.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
        }
        return button;
    }

    private StackPane createMainContent(Farmer farmer) {
        StackPane mainContent = new StackPane();
        mainContent.setPadding(new Insets(20));

        // Products Management Section
        VBox productsSection = new VBox(20);
        productsSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        productsSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Products Management");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addProductBtn = new Button("Add New Product");
        addProductBtn.getStyleClass().add("button-primary");
        addProductBtn.setFont(Font.font("Roboto", 14));

        header.getChildren().addAll(title, spacer, addProductBtn);

        // Products Table
        productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productsTable.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-secondary");
                deleteBtn.getStyleClass().add("button-danger");
                buttons.setAlignment(Pos.CENTER);
                editBtn.setFont(Font.font("Roboto", 12));
                deleteBtn.setFont(Font.font("Roboto", 12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Set header font for all columns
        for (TableColumn<Product, ?> column : productsTable.getColumns()) {
            column.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");
        }

        productsTable.getColumns().addAll(nameCol, priceCol, unitCol, quantityCol, actionsCol);
        productsTable.setItems(productsList);

        // Add Product Dialog
        addProductBtn.setOnAction(e -> showAddProductDialog(farmer));

        productsSection.getChildren().addAll(header, productsTable);
        mainContent.getChildren().add(productsSection);

        return mainContent;
    }

    private void showAddProductDialog(Farmer farmer) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter product details");

        // Create the custom dialog content with enhanced fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Basic product fields
        TextField nameField = new TextField();
        nameField.setPromptText("Product name");
        
        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        
        TextField unitField = new TextField();
        unitField.setPromptText("kg, piece, bag, etc.");
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Available quantity");
        
        TextField minStockField = new TextField();
        minStockField.setPromptText("10");
        minStockField.setText("10");
        
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setPromptText("Product description...");

        // Enhanced product fields
        ComboBox<String> categoryField = new ComboBox<>();
        categoryField.getItems().addAll(
            "Vegetables", "Fruits", "Grains", "Dairy", "Herbs", 
            "Spices", "Nuts", "Seeds", "Meat", "Eggs", "Honey", "Other"
        );
        categoryField.setValue("Other");
        
        CheckBox organicBox = new CheckBox("Organic Product");
        organicBox.setStyle("-fx-text-fill: #333333;");
        CheckBox seasonalBox = new CheckBox("Seasonal Product");
        seasonalBox.setStyle("-fx-text-fill: #333333;");
        
        TextField originField = new TextField();
        originField.setPromptText("Farm location or region");
        
        TextField harvestDateField = new TextField();
        harvestDateField.setPromptText("e.g., March 2024");
        
        TextField shelfLifeField = new TextField();
        shelfLifeField.setPromptText("e.g., 7 days, 2 weeks");
        
        TextArea storageArea = new TextArea();
        storageArea.setPrefRowCount(2);
        storageArea.setPromptText("Storage instructions...");

        // Image upload section
        VBox imageSection = new VBox(10);
        Label imageLabel = new Label("Product Image:");
        imageLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        Button selectImageBtn = new Button("📷 Select Image");
        selectImageBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5;");
        
        Label imageStatusLabel = new Label("No image selected");
        imageStatusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(120);
        imagePreview.setFitHeight(80);
        imagePreview.setPreserveRatio(true);
        imagePreview.setSmooth(true);
        imagePreview.setVisible(false);
        
        // Store selected image file
        final File[] selectedImageFile = {null};
        
        selectImageBtn.setOnAction(e -> {
            File imageFile = ImageUploadService.chooseImageFile(
                (Stage) dialog.getDialogPane().getScene().getWindow(), 
                "Select Product Image"
            );
            
            if (imageFile != null) {
                ImageUploadService.ValidationResult validation = ImageUploadService.validateImageFile(imageFile);
                if (validation.isValid()) {
                    selectedImageFile[0] = imageFile;
                    imageStatusLabel.setText("✓ " + imageFile.getName());
                    imageStatusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                    
                    // Show preview
                    try {
                        Image previewImage = new Image(imageFile.toURI().toString());
                        imagePreview.setImage(previewImage);
                        imagePreview.setVisible(true);
                    } catch (Exception ex) {
                        imageStatusLabel.setText("⚠ Could not load image preview");
                        imageStatusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px;");
                    }
                } else {
                    imageStatusLabel.setText("✗ " + validation.getMessage());
                    imageStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    selectedImageFile[0] = null;
                    imagePreview.setVisible(false);
                }
            }
        });
        
        Button removeImageBtn = new Button("Remove Image");
        removeImageBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 11px;");
        removeImageBtn.setOnAction(e -> {
            selectedImageFile[0] = null;
            imageStatusLabel.setText("No image selected");
            imageStatusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            imagePreview.setVisible(false);
        });
        
        HBox imageButtons = new HBox(10);
        imageButtons.getChildren().addAll(selectImageBtn, removeImageBtn);
        
        imageSection.getChildren().addAll(imageButtons, imageStatusLabel, imagePreview);

        // Layout the enhanced form with properly styled labels
        int row = 0;
        
        Label productNameLabel = new Label("Product Name:");
        productNameLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(productNameLabel, 0, row);
        grid.add(nameField, 1, row++);
        
        Label categoryLabel = new Label("Category:");
        categoryLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(categoryLabel, 0, row);
        grid.add(categoryField, 1, row++);
        
        Label priceLabel = new Label("Price:");
        priceLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(priceLabel, 0, row);
        grid.add(priceField, 1, row++);
        
        Label unitLabel = new Label("Unit:");
        unitLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(unitLabel, 0, row);
        grid.add(unitField, 1, row++);
        
        Label quantityLabel = new Label("Quantity:");
        quantityLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(quantityLabel, 0, row);
        grid.add(quantityField, 1, row++);
        
        Label minStockLabel = new Label("Min Stock Level:");
        minStockLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(minStockLabel, 0, row);
        grid.add(minStockField, 1, row++);
        
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(descriptionLabel, 0, row);
        grid.add(descriptionArea, 1, row++);
        
        // Create a container for checkboxes
        HBox checkBoxContainer = new HBox(15);
        checkBoxContainer.getChildren().addAll(organicBox, seasonalBox);
        Label propertiesLabel = new Label("Properties:");
        propertiesLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(propertiesLabel, 0, row);
        grid.add(checkBoxContainer, 1, row++);
        
        Label originLabel = new Label("Origin:");
        originLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(originLabel, 0, row);
        grid.add(originField, 1, row++);
        
        Label harvestLabel = new Label("Harvest Date:");
        harvestLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(harvestLabel, 0, row);
        grid.add(harvestDateField, 1, row++);
        
        Label shelfLifeLabel = new Label("Shelf Life:");
        shelfLifeLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(shelfLifeLabel, 0, row);
        grid.add(shelfLifeField, 1, row++);
        
        Label storageLabel = new Label("Storage Instructions:");
        storageLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        grid.add(storageLabel, 0, row);
        grid.add(storageArea, 1, row++);
        
        grid.add(imageLabel, 0, row);
        grid.add(imageSection, 1, row++);

        dialog.getDialogPane().setContent(grid);
        
        ButtonType addButtonType = new ButtonType("Add Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    // Validation
                    if (nameField.getText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Product name is required");
                    }
                    if (unitField.getText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Unit is required");
                    }
                    
                    double price = Double.parseDouble(priceField.getText().trim());
                    if (price < 0) {
                        throw new IllegalArgumentException("Price cannot be negative");
                    }
                    
                    int quantity = Integer.parseInt(quantityField.getText().trim());
                    if (quantity < 0) {
                        throw new IllegalArgumentException("Quantity cannot be negative");
                    }
                    
                    int minStock = Integer.parseInt(minStockField.getText().trim());
                    if (minStock < 0) {
                        throw new IllegalArgumentException("Minimum stock level cannot be negative");
                    }

                    // Create enhanced product
                    Product product = new Product(
                            nameField.getText().trim(),
                            price,
                            descriptionArea.getText().trim(),
                            unitField.getText().trim(),
                            quantity,
                            farmer.getId(),
                            categoryField.getValue(),
                            organicBox.isSelected()
                    );
                    
                    // Set additional properties
                    product.setMinStockLevel(minStock);
                    product.setSeasonal(seasonalBox.isSelected());
                    product.setOrigin(originField.getText().trim());
                    product.setHarvestDate(harvestDateField.getText().trim());
                    product.setShelfLife(shelfLifeField.getText().trim());
                    product.setStorageInstructions(storageArea.getText().trim());
                    
                    // Handle image upload if selected
                    if (selectedImageFile[0] != null) {
                        ImageUploadService.UploadResult uploadResult = ImageUploadService.saveProductImage(
                            selectedImageFile[0], farmer.getId(), product.getId()
                        );
                        
                        if (uploadResult.isSuccess()) {
                            product.setImagePath(uploadResult.getFilePath());
                        } else {
                            // Show warning but don't fail the product creation
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Image Upload Warning");
                            alert.setHeaderText("Product created but image upload failed");
                            alert.setContentText(uploadResult.getMessage());
                            alert.showAndWait();
                        }
                    }
                    
                    // Add default tags based on category and properties
                    if (organicBox.isSelected()) {
                        product.addTag("organic");
                    }
                    if (seasonalBox.isSelected()) {
                        product.addTag("seasonal");
                    }
                    product.addTag("fresh");
                    product.addTag("local");
                    
                    return product;
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter valid numbers for price, quantity, and minimum stock level.");
                    alert.showAndWait();
                    return null;
                } catch (IllegalArgumentException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            // Save product to database
            try {
                String insertProductSQL = "INSERT INTO Product (name, price, description, unit, quantity, farmerId, imagePath) VALUES (?, ?, ?, ?, ?, ?, ?)";
                var preparedStatement = dbConnection.prepareStatement(insertProductSQL);
                preparedStatement.setString(1, product.getName());
                preparedStatement.setDouble(2, product.getPrice());
                preparedStatement.setString(3, product.getDescription());
                preparedStatement.setString(4, product.getUnit());
                preparedStatement.setInt(5, product.getQuantity());
                preparedStatement.setLong(6, Long.parseLong(farmer.getId()));
                preparedStatement.setString(7, product.getImagePath());
                
                preparedStatement.executeUpdate();
                
                // Also add to local lists
                productsList.add(product);
                farmer.addProduct(product);
                
                showAlert("Success", "Product '" + product.getName() + "' added successfully and saved to database!");
            } catch (Exception e) {
                showAlert("Error", "Failed to save product to database: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showCustomerDashboard(Customer customer) {
        this.currentCustomer = customer;
        BorderPane dashboard = new BorderPane();
        dashboard.setStyle("-fx-background-color: white;");

        // Top Bar
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // --- Add logo at the left ---
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            HBox.setMargin(logoView, new Insets(0, 18, 0, 0));
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }

        // Dashboard Button
        Button dashboardBtn = new Button("Dashboard");
        dashboardBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        dashboardBtn.setOnAction(e -> {
            ScrollPane scrollPane = new ScrollPane(createCustomerDashboardContent(customer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            dashboard.setCenter(scrollPane);
        });

        Label welcomeLabel = new Label("Welcome, " + customer.getFullName());
        welcomeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        HBox rightSection = new HBox(15);
        rightSection.setAlignment(Pos.CENTER_RIGHT);

        // Messages Button with Badge
        StackPane messagesContainer = new StackPane();
        Button messagesBtn = new Button("Messages");
        messagesBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");

        Label messagesBadge = new Label("2");
        messagesBadge.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 10px;");
        messagesBadge.setVisible(true);

        StackPane.setAlignment(messagesBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(messagesBadge, new Insets(-5, -5, 0, 0));

        messagesContainer.getChildren().addAll(messagesBtn, messagesBadge);
        messagesBtn.setOnAction(e -> showCustomerMessages(customer));

        // Browse Farmers Button
        Button browseFarmersBtn = new Button("Browse Farmers");
        browseFarmersBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        browseFarmersBtn.setOnAction(e -> showFarmersDirectory(customer));

        // Cart Button with Badge
        StackPane cartButtonContainer = new StackPane();
        Button cartButton = new Button("Cart");
        cartButton.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");

        cartBadge = new Label("0");
        cartBadge.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 10px;");
        cartBadge.setVisible(false);

        StackPane.setAlignment(cartBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(cartBadge, new Insets(-5, -5, 0, 0));

        cartButtonContainer.getChildren().addAll(cartButton, cartBadge);
        cartButton.setOnAction(e -> showCart());

        // Profile Button with Menu
        StackPane profileContainer = new StackPane();
        Button profileBtn = new Button("Profile");
        profileBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        profileContainer.getChildren().add(profileBtn);

        // Create profile menu
        ContextMenu profileMenu = new ContextMenu();
        MenuItem viewProfile = new MenuItem("View Profile");
        MenuItem editProfile = new MenuItem("Edit Profile");
        MenuItem orderHistory = new MenuItem("Order History");
        MenuItem analytics = new MenuItem("Analytics Dashboard");
        MenuItem preferences = new MenuItem("Preferences");

        profileMenu.getItems().addAll(viewProfile, editProfile, orderHistory, analytics, preferences);

        profileBtn.setOnAction(e -> {
            profileMenu.show(profileBtn, Side.BOTTOM, 0, 0);
        });

        viewProfile.setOnAction(e -> showCustomerProfile(customer));
        editProfile.setOnAction(e -> showEditCustomerProfileDialog(customer));
        orderHistory.setOnAction(e -> showCustomerOrderHistory(customer));
        analytics.setOnAction(e -> showAnalyticsDashboard());
        preferences.setOnAction(e -> showCustomerPreferences(customer));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        rightSection.getChildren().addAll(messagesContainer, browseFarmersBtn, cartButtonContainer, profileContainer, logoutBtn);
        if (logoView != null) {
            topBar.getChildren().add(logoView);
        }
        topBar.getChildren().addAll(dashboardBtn, welcomeLabel, rightSection);
        HBox.setHgrow(rightSection, Priority.ALWAYS);

        // Main Content
        ScrollPane scrollPane = new ScrollPane(createCustomerDashboardContent(customer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dashboard.setTop(topBar);
        dashboard.setCenter(scrollPane);

        primaryStage.setScene(new Scene(dashboard));
    }

    private void showCustomerMessages(Customer customer) {
        VBox messagesContent = new VBox(20);
        messagesContent.setPadding(new Insets(20));
        messagesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Messages");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox messagesList = new VBox(10);
        messagesList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample messages
        messagesList.getChildren().addAll(
                createMessageItem("John's Organic Farm", "Your order #1001 has been delivered!", "2 hours ago", true),
                createMessageItem("Green Valley Farm", "New organic products available!", "5 hours ago", true),
                createMessageItem("Fresh Harvest Co.", "Thank you for your recent order!", "1 day ago", false),
                createMessageItem("Local Farmers Market", "Special weekend discounts available!", "2 days ago", false));

        Button newMessageBtn = new Button("New Message");
        newMessageBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        newMessageBtn.setOnAction(e -> showNewMessageDialog(customer));

        messagesContent.getChildren().addAll(titleLabel, messagesList, newMessageBtn);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(messagesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showNewMessageDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Message");
        dialog.setHeaderText("Send a message to a farmer");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        ComboBox<String> farmerComboBox = new ComboBox<>();
        farmerComboBox.getItems().addAll(
                "John's Organic Farm",
                "Green Valley Farm",
                "Fresh Harvest Co.",
                "Local Farmers Market");
        farmerComboBox.setPromptText("Select Farmer");
        farmerComboBox.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Type your message here...");
        messageArea.setPrefRowCount(5);
        messageArea.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        content.getChildren().addAll(
                new Label("Select Farmer:"),
                farmerComboBox,
                new Label("Message:"),
                messageArea);

        dialog.getDialogPane().setContent(content);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                String selectedFarmer = farmerComboBox.getValue();
                String message = messageArea.getText();

                if (selectedFarmer != null && !message.trim().isEmpty()) {
                    // In a real app, this would send the message to the selected farmer
                    showNotification("Message sent to " + selectedFarmer);
                    return null;
                } else {
                    showError("Error", "Please select a farmer and enter a message.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showCustomerProfile(Customer customer) {
        VBox profileContent = new VBox(20);
        profileContent.setPadding(new Insets(20));
        profileContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Profile Information");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        // Basic Information Section
        VBox infoBox = new VBox(15);
        infoBox.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        addProfileField(infoBox, "Full Name", customer.getFullName());
        addProfileField(infoBox, "Email", customer.getEmail());
        addProfileField(infoBox, "Phone", customer.getPhoneNumber());
        addProfileField(infoBox, "Location", customer.getLocation());
        addProfileField(infoBox, "Member Since", customer.getJoinDateString());
        
        // Account Status and Security
        String accountStatus = customer.isActive() ? "Active" : "Inactive";
        if (customer.isAccountLocked()) {
            accountStatus += " (Locked)";
        }
        addProfileField(infoBox, "Account Status", accountStatus);
        addProfileField(infoBox, "Email Verified", customer.isVerified() ? "✓ Verified" : "❌ Not Verified");
        addProfileField(infoBox, "Two-Factor Auth", customer.isTwoFactorEnabled() ? "✓ Enabled" : "❌ Disabled");

        // Security & Settings Section
        Label securityLabel = new Label("Security & Notification Settings");
        securityLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-padding: 20px 0 10px 0;");

        VBox securityBox = new VBox(15);
        securityBox.setStyle("-fx-background-color: #E8F5E8; -fx-padding: 20px; -fx-background-radius: 10px;");

        // Two-Factor Authentication Toggle
        HBox twoFactorBox = new HBox(10);
        twoFactorBox.setAlignment(Pos.CENTER_LEFT);
        Label twoFactorLabel = new Label("Two-Factor Authentication:");
        twoFactorLabel.setStyle("-fx-font-weight: bold;");
        Button twoFactorBtn = new Button(customer.isTwoFactorEnabled() ? "Disable 2FA" : "Enable 2FA");
        twoFactorBtn.setStyle(customer.isTwoFactorEnabled() ? 
            "-fx-background-color: #d32f2f; -fx-text-fill: white;" : 
            "-fx-background-color: #2E7D32; -fx-text-fill: white;");
        twoFactorBtn.setOnAction(e -> toggleTwoFactorAuth(customer, twoFactorBtn));
        twoFactorBox.getChildren().addAll(twoFactorLabel, twoFactorBtn);

        // Notification Preferences
        VBox notificationBox = new VBox(10);
        Label notifLabel = new Label("Notification Preferences:");
        notifLabel.setStyle("-fx-font-weight: bold;");
        
        CheckBox emailNotif = new CheckBox("Email Notifications");
        emailNotif.setSelected(customer.isEmailNotificationsEnabled());
        emailNotif.setOnAction(e -> customer.setEmailNotificationsEnabled(emailNotif.isSelected()));
        
        CheckBox smsNotif = new CheckBox("SMS Notifications");
        smsNotif.setSelected(customer.isSmsNotificationsEnabled());
        smsNotif.setOnAction(e -> customer.setSmsNotificationsEnabled(smsNotif.isSelected()));
        
        CheckBox orderNotif = new CheckBox("Order Notifications");
        orderNotif.setSelected(customer.isOrderNotificationsEnabled());
        orderNotif.setOnAction(e -> customer.setOrderNotificationsEnabled(orderNotif.isSelected()));
        
        CheckBox promoNotif = new CheckBox("Promotional Notifications");
        promoNotif.setSelected(customer.isPromotionalNotificationsEnabled());
        promoNotif.setOnAction(e -> customer.setPromotionalNotificationsEnabled(promoNotif.isSelected()));
        
        notificationBox.getChildren().addAll(notifLabel, emailNotif, smsNotif, orderNotif, promoNotif);

        securityBox.getChildren().addAll(twoFactorBox, new Separator(), notificationBox);

        // Action Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button editButton = new Button("Edit Profile");
        editButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        editButton.setOnAction(e -> showEditCustomerProfileDialog(customer));

        Button changePasswordBtn = new Button("Change Password");
        changePasswordBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        changePasswordBtn.setOnAction(e -> showChangePasswordDialog(customer));

        Button saveSettingsBtn = new Button("Save Settings");
        saveSettingsBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        saveSettingsBtn.setOnAction(e -> {
            // Save notification preferences to database here
            showAlert("Settings Saved", "Your notification preferences have been updated.");
        });

        buttonBox.getChildren().addAll(editButton, changePasswordBtn, saveSettingsBtn);

        profileContent.getChildren().addAll(titleLabel, infoBox, securityLabel, securityBox, buttonBox);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(profileContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);

        Button reportBtn = new Button("Report User");
        reportBtn.getStyleClass().add("button-danger");
        reportBtn.setOnAction(e -> {
            TextInputDialog reasonDialog = new TextInputDialog();
            reasonDialog.setTitle("Report User");
            reasonDialog.setHeaderText("Why are you reporting this user?");
            reasonDialog.setContentText("Reason:");
            var result = reasonDialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                try {
                    var stmt = dbConnection.prepareStatement("INSERT INTO Reports (reporterUsername, reportedUsername, role, reason) VALUES (?, ?, ?, ?)");
                    stmt.setString(1, currentCustomer != null ? currentCustomer.getUsername() : "");
                    stmt.setString(2, customer.getUsername());
                    stmt.setString(3, "Customer");
                    stmt.setString(4, result.get().trim());
                    stmt.executeUpdate();
                    showAlert("Reported", "User has been reported to the admin.");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to submit report: " + ex.getMessage());
                }
            }
        });
        profileContent.getChildren().add(reportBtn);
    }

    private void addProfileField(VBox container, String label, String value) {
        HBox field = new HBox(10);
        field.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 120px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #666;");

        field.getChildren().addAll(labelNode, valueNode);
        container.getChildren().add(field);
    }

    private void showEditCustomerProfileDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your profile information");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(customer.getFullName());
        TextField emailField = new TextField(customer.getEmail());
        TextField phoneField = new TextField(customer.getPhoneNumber());
        TextField locationField = new TextField(customer.getLocation());

        // Create labels with proper text color for customer profile editing
        Label customerNameLabel = new Label("Name:");
        customerNameLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label customerEmailLabel = new Label("Email:");
        customerEmailLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label customerPhoneLabel = new Label("Phone:");
        customerPhoneLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label customerLocationLabel = new Label("Location:");
        customerLocationLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        grid.add(customerNameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(customerEmailLabel, 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(customerPhoneLabel, 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(customerLocationLabel, 0, 3);
        grid.add(locationField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Validation
                if (nameField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty() ||
                    phoneField.getText().trim().isEmpty() || locationField.getText().trim().isEmpty()) {
                    showAlert("Validation Error", "All fields are required.");
                    return null;
                }
                if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    showAlert("Validation Error", "Invalid email format.");
                    return null;
                }
                if (!phoneField.getText().matches("^[0-9\\-+() ]{7,}$")) {
                    showAlert("Validation Error", "Invalid phone number format.");
                    return null;
                }
                // Update DB
                try {
                    var stmt = dbConnection.prepareStatement(
                        "UPDATE Customer SET name=?, email=?, phone=?, address=? WHERE username=?");
                    stmt.setString(1, nameField.getText().trim());
                    stmt.setString(2, emailField.getText().trim());
                    stmt.setString(3, phoneField.getText().trim());
                    stmt.setString(4, locationField.getText().trim());
                    stmt.setString(5, customer.getUsername());
                    stmt.executeUpdate();
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update profile: " + ex.getMessage());
                    return null;
                }
                customer.setFullName(nameField.getText());
                customer.setEmail(emailField.getText());
                customer.setPhoneNumber(phoneField.getText());
                customer.setLocation(locationField.getText());
                return customer;
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(updatedCustomer -> {
            showAlert("Success", "Profile updated successfully!");
        });
    }

    /**
     * Toggle Two-Factor Authentication for a user
     */
    private void toggleTwoFactorAuth(Customer customer, Button toggleButton) {
        if (customer.isTwoFactorEnabled()) {
            // Disable 2FA
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Disable Two-Factor Authentication");
            confirmAlert.setHeaderText("Are you sure you want to disable Two-Factor Authentication?");
            confirmAlert.setContentText("This will make your account less secure.");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                customer.disableTwoFactor();
                toggleButton.setText("Enable 2FA");
                toggleButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
                showAlert("2FA Disabled", "Two-Factor Authentication has been disabled for your account.");
            }
        } else {
            // Enable 2FA
            showTwoFactorSetupDialog(customer, toggleButton);
        }
    }

    /**
     * Show Two-Factor Authentication setup dialog
     */
    private void showTwoFactorSetupDialog(Customer customer, Button toggleButton) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Enable Two-Factor Authentication");
        dialog.setHeaderText("Set up Two-Factor Authentication");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label infoLabel = new Label("1. Install Google Authenticator or similar app on your phone");
        infoLabel.setWrapText(true);
        
        Label qrLabel = new Label("2. Scan the QR code below (or enter the secret manually):");
        qrLabel.setWrapText(true);
        
        // Generate a mock secret for demo purposes
        String secret = "JBSWY3DPEHPK3PXP"; // In real app, use TwoFactorAuth.generateSecret()
        String qrUrl = "https://chart.googleapis.com/chart?chs=200x200&chld=M|0&cht=qr&chl=" +
                       "otpauth://totp/FarmConnect:" + customer.getEmail() + "?secret=" + secret + "&issuer=FarmConnect";
        
        Label secretLabel = new Label("Secret: " + secret);
        secretLabel.setStyle("-fx-font-family: monospace; -fx-background-color: #f0f0f0; -fx-padding: 10px;");
        
        Label verifyLabel = new Label("3. Enter the 6-digit code from your app to verify:");
        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setMaxWidth(120);
        
        content.getChildren().addAll(infoLabel, qrLabel, secretLabel, verifyLabel, codeField);
        dialog.getDialogPane().setContent(content);
        
        ButtonType enableButtonType = new ButtonType("Enable", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enableButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == enableButtonType) {
                String code = codeField.getText().trim();
                if (code.length() == 6 && code.matches("\\d{6}")) {
                    return true;
                } else {
                    showAlert("Invalid Code", "Please enter a valid 6-digit code.");
                    return false;
                }
            }
            return false;
        });
        
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            customer.enableTwoFactor(secret, "[]"); // Empty backup codes for demo
            toggleButton.setText("Disable 2FA");
            toggleButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");
            showAlert("2FA Enabled", "Two-Factor Authentication has been enabled for your account!");
        }
    }

    /**
     * Show change password dialog
     */
    private void showChangePasswordDialog(Customer customer) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Update your password");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField currentPasswordField = new PasswordField();
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        
        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        
        Label strengthLabel = new Label("Password strength will appear here");
        strengthLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        grid.add(strengthLabel, 1, 3);
        
        // Password strength indicator
        newPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() < 6) {
                strengthLabel.setText("Too short (minimum 6 characters)");
                strengthLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12px;");
            } else if (newText.length() < 8) {
                strengthLabel.setText("Weak password");
                strengthLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
            } else if (newText.matches(".*[A-Z].*") && newText.matches(".*[a-z].*") && newText.matches(".*[0-9].*")) {
                strengthLabel.setText("Strong password");
                strengthLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px;");
            } else {
                strengthLabel.setText("Medium password");
                strengthLabel.setStyle("-fx-text-fill: #2196f3; -fx-font-size: 12px;");
            }
        });
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                String currentPassword = currentPasswordField.getText();
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                
                // Validate current password
                if (!customer.checkPassword(currentPassword)) {
                    showAlert("Error", "Current password is incorrect.");
                    return false;
                }
                
                // Validate new password
                if (newPassword.length() < 6) {
                    showAlert("Error", "New password must be at least 6 characters long.");
                    return false;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    showAlert("Error", "New passwords do not match.");
                    return false;
                }
                
                // Update password
                customer.setPassword(newPassword);
                
                // Update database (in real app)
                try {
                    var stmt = dbConnection.prepareStatement("UPDATE Customer SET passwordHash=?, passwordSalt=? WHERE username=?");
                    stmt.setString(1, customer.getPasswordHash());
                    stmt.setString(2, customer.getPasswordSalt());
                    stmt.setString(3, customer.getUsername());
                    stmt.executeUpdate();
                    return true;
                } catch (Exception ex) {
                    showAlert("Error", "Failed to update password: " + ex.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            showAlert("Success", "Password changed successfully!");
        }
    }

    private void showCustomerOrderHistory(Customer customer) {
        VBox orderHistoryContent = new VBox(20);
        orderHistoryContent.setPadding(new Insets(20));
        orderHistoryContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Order History");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample orders (in a real app, these would come from a database)
        for (int i = 0; i < 5; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            Button viewDetailsBtn = new Button("View Details");
            viewDetailsBtn.setStyle(
                    "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate, viewDetailsBtn);
            ordersList.getChildren().add(orderItem);
        }

        orderHistoryContent.getChildren().addAll(titleLabel, ordersList);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(orderHistoryContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showCustomerPreferences(Customer customer) {
        VBox preferencesContent = new VBox(20);
        preferencesContent.setPadding(new Insets(20));
        preferencesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Preferences");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox preferencesBox = new VBox(15);
        preferencesBox.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        // Notification Preferences
        Label notificationTitle = new Label("Notification Preferences");
        notificationTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ToggleSwitch emailNotifications = new ToggleSwitch("Email Notifications");
        ToggleSwitch smsNotifications = new ToggleSwitch("SMS Notifications");
        ToggleSwitch orderUpdates = new ToggleSwitch("Order Updates");
        ToggleSwitch promotions = new ToggleSwitch("Promotions and Offers");

        // Privacy Settings
        Label privacyTitle = new Label("Privacy Settings");
        privacyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ToggleSwitch shareLocation = new ToggleSwitch("Share Location with Farmers");
        ToggleSwitch showProfile = new ToggleSwitch("Show Profile to Farmers");

        preferencesBox.getChildren().addAll(
                notificationTitle,
                emailNotifications,
                smsNotifications,
                orderUpdates,
                promotions,
                new Separator(),
                privacyTitle,
                shareLocation,
                showProfile);

        Button saveButton = new Button("Save Preferences");
        saveButton.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        saveButton.setOnAction(e -> {
            // Save preferences logic here
            showNotification("Preferences saved successfully!");
        });

        preferencesContent.getChildren().addAll(titleLabel, preferencesBox, saveButton);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(preferencesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createCustomerDashboardContent(Customer customer) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Welcome Section
        VBox welcomeSection = new VBox(10);
        welcomeSection.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        Label welcomeTitle = new Label("Welcome back, " + customer.getFullName() + "!");
        welcomeTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label welcomeSubtitle = new Label("Here's what's happening with your orders and favorite products");
        welcomeSubtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 16px;");

        welcomeSection.getChildren().addAll(welcomeTitle, welcomeSubtitle);

        // Quick Stats Section
        HBox statsSection = new HBox(20);
        statsSection.setAlignment(Pos.CENTER_LEFT);

        // Active Orders Card
        VBox activeOrdersCard = createStatCard(
                "Active Orders",
                "2",
                "Orders in progress",
                "📦",
                () -> showCustomerOrderHistory(customer));

        // Favorite Products Card
        VBox favoriteProductsCard = createStatCard(
                "Favorite Products",
                "5",
                "Saved items",
                "❤️",
                () -> showFavoriteProducts(customer));

        // Recent Activity Card
        VBox recentActivityCard = createStatCard(
                "Recent Activity",
                "3",
                "New updates",
                "🔄",
                () -> showRecentActivity(customer));

        statsSection.getChildren().addAll(activeOrdersCard, favoriteProductsCard, recentActivityCard);

        // Featured Products Section
        Label featuredTitle = new Label("Enhanced Featured Products");
        featuredTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane featuredProducts = new FlowPane();
        featuredProducts.setHgap(20);
        featuredProducts.setVgap(20);
        featuredProducts.setPrefWrapLength(800);

        // Add enhanced sample products
        List<Product> sampleProducts = loadProductsFromDatabase();
        for (Product product : sampleProducts) {
            VBox productCard = createProductCard(product);
            featuredProducts.getChildren().add(productCard);
        }

        // Recent Orders Section
        Label ordersTitle = new Label("Recent Orders");
        ordersTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add some sample orders
        for (int i = 0; i < 3; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            Button viewDetailsBtn = new Button("View Details");
            viewDetailsBtn.setStyle(
                    "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate, viewDetailsBtn);
            ordersList.getChildren().add(orderItem);
        }

        // Add all sections to the content
        content.getChildren().addAll(
                welcomeSection,
                statsSection,
                featuredTitle,
                featuredProducts,
                ordersTitle,
                ordersList);

        return content;
    }

    private void showFavoriteProducts(Customer customer) {
        VBox favoritesContent = new VBox(20);
        favoritesContent.setPadding(new Insets(20));
        favoritesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Favorite Products");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane productsGrid = new FlowPane();
        productsGrid.setHgap(20);
        productsGrid.setVgap(20);
        productsGrid.setPrefWrapLength(800);

        // Add enhanced sample favorite products
        List<Product> sampleProducts = loadProductsFromDatabase();
        for (Product product : sampleProducts) {
            if (sampleProducts.indexOf(product) < 6) { // Limit to 6 products for favorites
                VBox productCard = createProductCard(product);
                productsGrid.getChildren().add(productCard);
            }
        }

        favoritesContent.getChildren().addAll(titleLabel, productsGrid);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(favoritesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showRecentActivity(Customer customer) {
        VBox activityContent = new VBox(20);
        activityContent.setPadding(new Insets(20));
        activityContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Recent Activity");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox activityList = new VBox(10);
        activityList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample activities
        activityList.getChildren().addAll(
                createActivityItem("Order #1001 has been delivered", "2 hours ago", true),
                createActivityItem("New product available: Organic Apples", "5 hours ago", true),
                createActivityItem("Order #1000 has been shipped", "1 day ago", false),
                createActivityItem("Price update: Organic Tomatoes", "2 days ago", false));

        activityContent.getChildren().addAll(titleLabel, activityList);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(activityContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showCart() {
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createCartContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createCartContent() {
        VBox cartSection = new VBox(20);
        cartSection.setStyle("-fx-background-color: white; -fx-padding: 20;");

        Label cartTitle = new Label("Shopping Cart");
        cartTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox cartItems = new VBox(15);
        cartItems.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add cart items
        for (CartItem item : this.cartItems) {
            cartItems.getChildren().add(createCartItem(item));
        }

        // Cart Summary
        VBox summary = new VBox(10);
        summary.setStyle(
                "-fx-background-color: white; -fx-padding: 20px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        double subtotal = this.cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        double shipping = 5.99;
        double total = subtotal + shipping;

        Label subtotalLabel = new Label(String.format("Subtotal: $%.2f", subtotal));
        Label shippingLabel = new Label(String.format("Shipping: $%.2f", shipping));
        Label totalLabel = new Label(String.format("Total: $%.2f", total));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Button checkoutBtn = new Button("Proceed to Checkout");
        checkoutBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        checkoutBtn.setOnAction(e -> showCheckoutDialog());

        summary.getChildren().addAll(subtotalLabel, shippingLabel, totalLabel, checkoutBtn);

        cartSection.getChildren().addAll(cartTitle, cartItems, summary);
        return cartSection;
    }

    private VBox createCartItem(CartItem item) {
        VBox itemBox = new VBox(10);
        itemBox.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

        HBox itemHeader = new HBox(10);
        itemHeader.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label priceLabel = new Label(String.format("$%.2f/%s", item.getPrice(), item.getUnit()));
        priceLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");

        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("-");
        minusBtn.setStyle(
                "-fx-background-color: #E0E0E0; -fx-text-fill: black; -fx-font-weight: bold; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15px;");

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30px; -fx-alignment: center;");

        Button plusBtn = new Button("+");
        plusBtn.setStyle(
                "-fx-background-color: #E0E0E0; -fx-text-fill: black; -fx-font-weight: bold; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15px;");

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

        quantityBox.getChildren().addAll(minusBtn, quantityLabel, plusBtn);

        minusBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                quantityLabel.setText(String.valueOf(item.getQuantity()));
                updateCartBadge();
            }
        });

        plusBtn.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            quantityLabel.setText(String.valueOf(item.getQuantity()));
            updateCartBadge();
        });

        removeBtn.setOnAction(e -> {
            cartItems.remove(item);
            itemBox.setVisible(false);
            itemBox.setManaged(false);
            updateCartBadge();
        });

        itemHeader.getChildren().addAll(nameLabel, priceLabel);
        itemBox.getChildren().addAll(itemHeader, quantityBox, removeBtn);

        return itemBox;
    }

    private void showCheckoutDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Complete Your Order");

        // Create main content with tabs
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-tab-min-width: 120px;");

        // Tab 1: Delivery Details with ScrollPane
        Tab deliveryTab = new Tab("📦 Delivery");
        deliveryTab.setClosable(false);
        VBox deliveryContent = createDeliveryTab();
        ScrollPane deliveryScrollPane = new ScrollPane(deliveryContent);
        deliveryScrollPane.setFitToWidth(true);
        deliveryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        deliveryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        deliveryScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        deliveryScrollPane.setPrefViewportHeight(300);
        deliveryScrollPane.setMinViewportHeight(250);
        deliveryTab.setContent(deliveryScrollPane);

        // Tab 2: Payment Method with ScrollPane
        Tab paymentTab = new Tab("💳 Payment");
        paymentTab.setClosable(false);
        VBox paymentContent = createPaymentTab();
        ScrollPane paymentScrollPane = new ScrollPane(paymentContent);
        paymentScrollPane.setFitToWidth(true);
        paymentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        paymentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paymentScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        paymentScrollPane.setPrefViewportHeight(300); // Set minimum viewport height
        paymentScrollPane.setMinViewportHeight(250); // Ensure minimum height for content visibility
        paymentTab.setContent(paymentScrollPane);

        // Tab 3: Order Review with ScrollPane
        Tab reviewTab = new Tab("📋 Review");
        reviewTab.setClosable(false);
        VBox reviewContent = createOrderReviewTab();
        ScrollPane reviewScrollPane = new ScrollPane(reviewContent);
        reviewScrollPane.setFitToWidth(true);
        reviewScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        reviewScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        reviewScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        reviewScrollPane.setPrefViewportHeight(300);
        reviewScrollPane.setMinViewportHeight(250);
        reviewTab.setContent(reviewScrollPane);

        tabPane.getTabs().addAll(deliveryTab, paymentTab, reviewTab);
        
        // Create container for tabs and summary
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(15)); // Reduced padding to save space
        
        // Add order summary at the top
        VBox orderSummary = createOrderSummary();
        
        mainContent.getChildren().addAll(orderSummary, tabPane);

        dialog.getDialogPane().setContent(mainContent);

        // Add buttons with back button
        ButtonType backToCartButton = new ButtonType("⬅️ Back to Cart", ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType placeOrderButton = new ButtonType("🛒 Place Order", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(backToCartButton, cancelButton, placeOrderButton);

        // Style the dialog with reduced height
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-min-width: 650px; -fx-max-width: 700px; -fx-min-height: 400px; -fx-max-height: 550px; -fx-pref-height: 500px;");
        
        // Get the actual buttons to add event handlers
        Button actualBackToCartButton = (Button) dialog.getDialogPane().lookupButton(backToCartButton);
        Button actualPlaceOrderButton = (Button) dialog.getDialogPane().lookupButton(placeOrderButton);
        Button actualCancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        
        // Handle back to cart button click
        actualBackToCartButton.setOnAction(e -> {
            dialog.close();
            // Show cart dialog after a brief delay to ensure smooth transition
            Platform.runLater(() -> showCart());
        });
        
        // Handle place order button click
        actualPlaceOrderButton.setOnAction(e -> {
            try {
                processOrder(deliveryContent, paymentContent);
                dialog.close(); // Close dialog on success
            } catch (Exception ex) {
                showAlert("Validation Error", ex.getMessage(), Alert.AlertType.WARNING);
                // Don't close dialog, let user fix the issues
            }
        });
        
        // Handle cancel button
        actualCancelButton.setOnAction(e -> dialog.close());
        
        // Remove the default result converter since we're handling buttons manually
        dialog.setResultConverter(dialogButton -> null);

        dialog.showAndWait();
    }

    private VBox createDeliveryTab() {
        VBox content = new VBox(10); // Reduced spacing from 15 to 10
        content.setPadding(new Insets(15)); // Reduced padding from 20 to 15

        TextField addressField = new TextField();
        addressField.setPromptText("Enter full delivery address");
        addressField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;"); // Reduced padding and font

        TextField cityField = new TextField();
        cityField.setPromptText("City");
        cityField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        HBox locationBox = new HBox(8); // Reduced spacing
        TextField stateField = new TextField();
        stateField.setPromptText("State");
        stateField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");
        
        TextField zipField = new TextField();
        zipField.setPromptText("ZIP Code");
        zipField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");
        
        locationBox.getChildren().addAll(stateField, zipField);
        HBox.setHgrow(stateField, Priority.ALWAYS);
        HBox.setHgrow(zipField, Priority.ALWAYS);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number for delivery updates");
        phoneField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        ComboBox<String> shippingSpeed = new ComboBox<>();
        shippingSpeed.getItems().addAll("📦 Standard Shipping (5-7 days) - $5.99", "🚀 Express Shipping (2-3 days) - $12.99");
        shippingSpeed.setValue("📦 Standard Shipping (5-7 days) - $5.99");
        shippingSpeed.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        TextArea notesField = new TextArea();
        notesField.setPromptText("Special delivery instructions (Optional)");
        notesField.setPrefRowCount(2); // Reduced from 3 to 2 rows
        notesField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        Label freeShippingNote = new Label("💡 Free shipping on orders over $50!");
        freeShippingNote.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11px; -fx-font-style: italic;");

        // Create labels with proper text color
        Label addressLabel = new Label("📍 Address:");
        addressLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label cityLabel = new Label("🏙️ City:");
        cityLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label stateZipLabel = new Label("📍 State & ZIP:");
        stateZipLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label phoneLabel = new Label("📞 Phone:");
        phoneLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label shippingLabel = new Label("� Shipping:");
        shippingLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label notesLabel = new Label("📝 Notes:");
        notesLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        content.getChildren().addAll(
                addressLabel,
                addressField,
                cityLabel,
                cityField,
                stateZipLabel,
                locationBox,
                phoneLabel,
                phoneField,
                shippingLabel,
                shippingSpeed,
                freeShippingNote,
                notesLabel,
                notesField);

        // Store references for later use
        addressField.setId("addressField");
        phoneField.setId("phoneField");
        notesField.setId("notesField");
        shippingSpeed.setId("shippingSpeed");

        return content;
    }

    private VBox createPaymentTab() {
        VBox content = new VBox(12); // Increased spacing for better visibility
        content.setPadding(new Insets(20)); // Increased padding

        // Payment method selection
        Label paymentLabel = new Label("💳 Select Payment Method:");
        paymentLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        VBox paymentMethods = new VBox(10); // Increased spacing for radio buttons
        ToggleGroup paymentGroup = new ToggleGroup();

        // Credit Card option
        RadioButton creditCard = new RadioButton("💳 Credit/Debit Card");
        creditCard.setToggleGroup(paymentGroup);
        creditCard.setSelected(true);
        creditCard.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #333333;"); // Added text color for better visibility
        creditCard.setMinHeight(25); // Ensure minimum height for visibility

        // Card details form
        VBox cardDetails = new VBox(8);
        cardDetails.setPadding(new Insets(12));
        cardDetails.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8px;");

        TextField cardNumber = new TextField();
        cardNumber.setPromptText("1234 5678 9012 3456");
        cardNumber.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        HBox cardRow = new HBox(8);
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        expiryField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");
        
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV");
        cvvField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");
        
        cardRow.getChildren().addAll(expiryField, cvvField);
        HBox.setHgrow(expiryField, Priority.ALWAYS);

        TextField cardHolderName = new TextField();
        cardHolderName.setPromptText("Cardholder Name");
        cardHolderName.setStyle("-fx-padding: 8px; -fx-background-radius: 5px; -fx-font-size: 13px;");

        // Create card detail labels with proper text color
        Label cardNumberLabel = new Label("Card Number:");
        cardNumberLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label expiryLabel = new Label("Expiry & CVV:");
        expiryLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
        
        Label holderNameLabel = new Label("Cardholder Name:");
        holderNameLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");

        cardDetails.getChildren().addAll(
                cardNumberLabel,
                cardNumber,
                expiryLabel,
                cardRow,
                holderNameLabel,
                cardHolderName);

        // PayPal option
        RadioButton paypal = new RadioButton("🎯 PayPal");
        paypal.setToggleGroup(paymentGroup);
        paypal.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #333333;");
        paypal.setMinHeight(25);

        // Digital Wallet options
        RadioButton applePay = new RadioButton("🍎 Apple Pay");
        applePay.setToggleGroup(paymentGroup);
        applePay.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #333333;");
        applePay.setMinHeight(25);

        RadioButton googlePay = new RadioButton("🅖 Google Pay");
        googlePay.setToggleGroup(paymentGroup);
        googlePay.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #333333;");
        googlePay.setMinHeight(25);

        // Cash on delivery
        RadioButton cashOnDelivery = new RadioButton("💵 Cash on Delivery");
        cashOnDelivery.setToggleGroup(paymentGroup);
        cashOnDelivery.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #333333;");
        cashOnDelivery.setMinHeight(25);

        paymentMethods.getChildren().addAll(creditCard, cardDetails, paypal, applePay, googlePay, cashOnDelivery);

        // Security note
        Label securityNote = new Label("🔒 Your payment information is secured with 256-bit SSL encryption");
        securityNote.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");

        content.getChildren().addAll(paymentLabel, paymentMethods, securityNote);

        // Store references
        creditCard.setId("creditCardRadio");
        cardNumber.setId("cardNumber");
        expiryField.setId("expiryField");
        cvvField.setId("cvvField");
        cardHolderName.setId("cardHolderName");
        paypal.setId("paypalRadio");
        applePay.setId("applePayRadio");
        googlePay.setId("googlePayRadio");
        cashOnDelivery.setId("cashOnDeliveryRadio");

        // Show/hide card details based on selection
        paymentGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            cardDetails.setVisible(newToggle == creditCard);
            cardDetails.setManaged(newToggle == creditCard);
        });

        return content;
    }

    private VBox createOrderReviewTab() {
        VBox content = new VBox(10); // Reduced spacing from 15 to 10
        content.setPadding(new Insets(15)); // Reduced padding from 20 to 15

        Label reviewLabel = new Label("📋 Order Summary");
        reviewLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;"); // Added text color

        // Items summary
        VBox itemsList = new VBox(8); // Reduced spacing
        double subtotal = 0;
        for (CartItem item : cartItems) {
            HBox itemRow = new HBox();
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setSpacing(8); // Reduced spacing
            
            Label itemName = new Label(item.getName());
            itemName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333333;"); // Added text color
            
            Label quantity = new Label("x" + item.getQuantity());
            quantity.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;"); // Reduced font size
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            double itemTotal = item.getPrice() * item.getQuantity();
            Label price = new Label(String.format("$%.2f", itemTotal));
            price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            
            itemRow.getChildren().addAll(itemName, quantity, spacer, price);
            itemsList.getChildren().add(itemRow);
            
            subtotal += itemTotal;
        }

        // Cost breakdown
        VBox costBreakdown = new VBox(6); // Reduced spacing
        costBreakdown.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12px; -fx-background-radius: 8px;"); // Reduced padding

        Label subtotalLabel = new Label(String.format("Subtotal: $%.2f", subtotal));
        subtotalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;"); // Added text color
        
        Label taxLabel = new Label(String.format("Tax (8.5%%): $%.2f", subtotal * 0.085));
        taxLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        
        Label shippingLabel = new Label(subtotal >= 50 ? "Shipping: FREE" : "Shipping: $5.99");
        shippingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        
        Separator separator = new Separator();
        
        double tax = subtotal * 0.085;
        double shipping = subtotal >= 50 ? 0 : 5.99;
        double total = subtotal + tax + shipping;
        
        Label totalLabel = new Label(String.format("Total: $%.2f", total));
        totalLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;"); // Reduced font size

        costBreakdown.getChildren().addAll(subtotalLabel, taxLabel, shippingLabel, separator, totalLabel);

        content.getChildren().addAll(reviewLabel, itemsList, new Separator(), costBreakdown);

        return content;
    }

    private VBox createOrderSummary() {
        VBox summary = new VBox(4); // Reduced spacing
        summary.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 12px; -fx-background-radius: 8px;"); // Reduced padding

        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        double subtotal = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();

        Label summaryTitle = new Label("🛒 Order Summary");
        summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        Label itemsLabel = new Label(String.format("%d items in cart", totalItems));
        itemsLabel.setStyle("-fx-text-fill: #333333;");
        
        Label subtotalLabel = new Label(String.format("Subtotal: $%.2f", subtotal));
        subtotalLabel.setStyle("-fx-text-fill: #333333;");

        summary.getChildren().addAll(summaryTitle, itemsLabel, subtotalLabel);

        return summary;
    }

    private void processOrder(VBox deliveryContent, VBox paymentContent) throws Exception {
        // Get form values
        TextField addressField = (TextField) deliveryContent.lookup("#addressField");
        TextField phoneField = (TextField) deliveryContent.lookup("#phoneField");
        TextArea notesField = (TextArea) deliveryContent.lookup("#notesField");
        ComboBox<String> shippingSpeed = (ComboBox<String>) deliveryContent.lookup("#shippingSpeed");

        // Validate required fields
        if (addressField.getText().trim().isEmpty() || phoneField.getText().trim().isEmpty()) {
            throw new Exception("Please fill in all required delivery fields.");
        }

        // Get payment method
        String selectedPaymentMethod = getSelectedPaymentMethod(paymentContent);
        if (selectedPaymentMethod == null) {
            throw new Exception("Please select a payment method.");
        }

        // Create transaction
        boolean expressShipping = shippingSpeed.getValue().contains("Express");
        processPaymentTransaction(selectedPaymentMethod, expressShipping, addressField.getText(), phoneField.getText(), notesField.getText());
    }

    private String getSelectedPaymentMethod(VBox paymentContent) throws Exception {
        RadioButton creditCard = (RadioButton) paymentContent.lookup("#creditCardRadio");
        RadioButton paypal = (RadioButton) paymentContent.lookup("#paypalRadio");
        RadioButton applePay = (RadioButton) paymentContent.lookup("#applePayRadio");
        RadioButton googlePay = (RadioButton) paymentContent.lookup("#googlePayRadio");
        RadioButton cashOnDelivery = (RadioButton) paymentContent.lookup("#cashOnDeliveryRadio");

        if (creditCard.isSelected()) {
            // Validate card fields
            TextField cardNumber = (TextField) paymentContent.lookup("#cardNumber");
            TextField expiryField = (TextField) paymentContent.lookup("#expiryField");
            TextField cvvField = (TextField) paymentContent.lookup("#cvvField");
            TextField cardHolderName = (TextField) paymentContent.lookup("#cardHolderName");

            if (cardNumber.getText().trim().isEmpty() || expiryField.getText().trim().isEmpty() || 
                cvvField.getText().trim().isEmpty() || cardHolderName.getText().trim().isEmpty()) {
                throw new Exception("Please fill in all card details.");
            }
            return "CREDIT_CARD";
        } else if (paypal.isSelected()) {
            return "PAYPAL";
        } else if (applePay.isSelected()) {
            return "APPLE_PAY";
        } else if (googlePay.isSelected()) {
            return "GOOGLE_PAY";
        } else if (cashOnDelivery.isSelected()) {
            return "CASH_ON_DELIVERY";
        }

        throw new Exception("Please select a payment method.");
    }

    private void processPaymentTransaction(String paymentMethodType, boolean expressShipping, 
                                         String address, String phone, String notes) {
        try {
            // Initialize payment service
            com.example.services.PaymentService paymentService = new com.example.services.PaymentService();

            // Calculate totals
            double subtotal = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
            
            // Create a mock payment method for demo
            com.example.models.PaymentMethod.PaymentType type = com.example.models.PaymentMethod.PaymentType.valueOf(paymentMethodType);
            com.example.models.PaymentMethod paymentMethod = new com.example.models.PaymentMethod(
                currentCustomer.getId(), type, type.getDisplayName() + " •••• 1234"
            );

            // Save payment method
            paymentService.savePaymentMethod(paymentMethod);

            // Create transaction
            String orderId = "ORD_" + System.currentTimeMillis();
            com.example.models.Transaction transaction = new com.example.models.Transaction(
                orderId, currentCustomer.getId(), paymentMethod.getId(),
                com.example.models.Transaction.TransactionType.PURCHASE,
                java.math.BigDecimal.valueOf(subtotal), "USD"
            );

            // Add items to transaction
            for (CartItem cartItem : cartItems) {
                com.example.models.Transaction.TransactionItem transactionItem = 
                    new com.example.models.Transaction.TransactionItem(
                        "ITEM_" + System.currentTimeMillis(), // Using generated ID since CartItem doesn't have product ID
                        cartItem.getName(),
                        cartItem.getQuantity(),
                        java.math.BigDecimal.valueOf(cartItem.getPrice())
                    );
                transactionItem.setCategory("Agricultural Products"); // Default category
                transaction.addItem(transactionItem);
            }

            // Calculate amounts with tax and shipping
            transaction = paymentService.calculateTransactionAmounts(transaction, expressShipping);
            transaction.setDescription("Agricultural products order - " + cartItems.size() + " items");

            // Show processing dialog
            showPaymentProcessingDialog(transaction, paymentService, address, phone, notes);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Payment Error", "Failed to process payment: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showPaymentProcessingDialog(com.example.models.Transaction transaction, 
                                           com.example.services.PaymentService paymentService,
                                           String address, String phone, String notes) {
        Dialog<Void> processingDialog = new Dialog<>();
        processingDialog.setTitle("Processing Payment");
        processingDialog.setHeaderText("Please wait...");

        VBox content = new VBox(15);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);

        Label statusLabel = new Label("Processing your payment...");
        statusLabel.setStyle("-fx-font-size: 14px;");

        Label amountLabel = new Label("Amount: " + transaction.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        content.getChildren().addAll(progressIndicator, statusLabel, amountLabel);
        processingDialog.getDialogPane().setContent(content);
        processingDialog.getDialogPane().setStyle("-fx-background-color: white;");

        // Disable close button during processing
        processingDialog.getDialogPane().getButtonTypes().clear();

        processingDialog.show();

        // Process payment asynchronously
        paymentService.processPayment(transaction).thenAccept(result -> {
            Platform.runLater(() -> {
                processingDialog.close();
                
                if (result.getStatus().isSuccessful()) {
                    showOrderConfirmation(result, address, phone, notes);
                } else {
                    showPaymentFailureDialog(result, paymentService);
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                processingDialog.close();
                showAlert("Payment Error", "An unexpected error occurred: " + throwable.getMessage(), Alert.AlertType.ERROR);
            });
            return null;
        });
    }

    private void showPaymentFailureDialog(com.example.models.Transaction transaction, 
                                        com.example.services.PaymentService paymentService) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Payment Failed");
        alert.setHeaderText("Payment could not be processed");
        alert.setContentText("Reason: " + transaction.getFailureReason() + "\n\nWould you like to try again or choose a different payment method?");

        ButtonType retryButton = new ButtonType("Try Again");
        ButtonType changeMethodButton = new ButtonType("Change Payment Method");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(retryButton, changeMethodButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == retryButton && transaction.canBeRetried()) {
                // Retry payment
                transaction.incrementRetryCount();
                showPaymentProcessingDialog(transaction, paymentService, "", "", "");
            } else if (result.get() == changeMethodButton) {
                // Show checkout dialog again
                showCheckoutDialog();
            }
        }
    }

    private void showOrderConfirmation(com.example.models.Transaction transaction, 
                                      String address, String phone, String notes) {
        // Clear the cart
        cartItems.clear();
        updateCartBadge();

        // Create detailed confirmation dialog
        Dialog<Void> confirmationDialog = new Dialog<>();
        confirmationDialog.setTitle("Order Confirmed! 🎉");
        confirmationDialog.setHeaderText("Thank you for your order!");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        // Success icon and message
        Label successIcon = new Label("✅");
        successIcon.setStyle("-fx-font-size: 48px;");

        Label successMessage = new Label("Your order has been placed successfully!");
        successMessage.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        // Order details
        VBox orderDetails = new VBox(10);
        orderDetails.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20px; -fx-background-radius: 10px;");

        Label orderIdLabel = new Label("📋 Order ID: " + transaction.getOrderId());
        orderIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label transactionIdLabel = new Label("💳 Transaction ID: " + transaction.getId());
        transactionIdLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Label amountLabel = new Label("💰 Total Amount: " + transaction.getFormattedAmount());
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label paymentStatusLabel = new Label("💳 Payment Status: " + transaction.getStatusDisplay());
        paymentStatusLabel.setStyle("-fx-font-size: 14px;");

        Label deliveryLabel = new Label("📦 Delivery Address: " + address);
        deliveryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        orderDetails.getChildren().addAll(orderIdLabel, transactionIdLabel, amountLabel, 
                                        paymentStatusLabel, deliveryLabel);

        // Next steps
        VBox nextSteps = new VBox(8);
        nextSteps.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 15px; -fx-background-radius: 8px;");

        Label nextStepsTitle = new Label("📋 What happens next:");
        nextStepsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label step1 = new Label("✉️ You'll receive a confirmation email shortly");
        step1.setStyle("-fx-font-size: 12px;");

        Label step2 = new Label("📦 We'll prepare and package your items");
        step2.setStyle("-fx-font-size: 12px;");

        Label step3 = new Label("🚚 You'll get tracking information when shipped");
        step3.setStyle("-fx-font-size: 12px;");

        Label step4 = new Label("📞 We'll call you at " + phone + " for delivery coordination");
        step4.setStyle("-fx-font-size: 12px;");

        nextSteps.getChildren().addAll(nextStepsTitle, step1, step2, step3, step4);

        content.getChildren().addAll(successIcon, successMessage, orderDetails, nextSteps);

        confirmationDialog.getDialogPane().setContent(content);
        confirmationDialog.getDialogPane().setStyle("-fx-background-color: white;");

        // Add close button
        ButtonType closeButton = new ButtonType("Continue Shopping", ButtonBar.ButtonData.OK_DONE);
        confirmationDialog.getDialogPane().getButtonTypes().add(closeButton);

        confirmationDialog.setResultConverter(dialogButton -> {
            if (dialogButton == closeButton) {
                // Return to the main dashboard
                showCustomerDashboard(currentCustomer);
            }
            return null;
        });

        confirmationDialog.showAndWait();
    }

    // Keep the old method for compatibility
    private void showOrderConfirmation() {
        // Create a mock transaction for the old flow
        com.example.models.Transaction mockTransaction = new com.example.models.Transaction(
            "ORD_" + System.currentTimeMillis(), 
            currentCustomer != null ? currentCustomer.getId() : "unknown",
            "mock-payment-method",
            com.example.models.Transaction.TransactionType.PURCHASE,
            java.math.BigDecimal.valueOf(0), 
            "USD"
        );
        mockTransaction.setStatus(com.example.models.Transaction.TransactionStatus.COMPLETED);
        
        showOrderConfirmation(mockTransaction, "Address not provided", "Phone not provided", "");
    }

    private void showFarmerDetails(Product product, VBox farmerDetails) {
        System.out.println("Showing farmer details for product: " + product.getName());
        System.out.println("Product farmer ID: " + product.getFarmerId());

        // Find the farmer who owns this product
        Farmer farmer = findFarmerById(product.getFarmerId());
        System.out.println("Found farmer: " + (farmer != null ? farmer.getFullName() : "null"));

        if (farmer != null) {
            farmerDetails.getChildren().clear();
            farmerDetails.setSpacing(10);
            farmerDetails.setPadding(new Insets(10));
            farmerDetails.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

            Label farmerName = new Label("Farmer: " + farmer.getFullName());
            farmerName.setFont(Font.font("System", FontWeight.BOLD, 14));

            Label farmName = new Label("Farm: " + farmer.getFarmName());
            farmName.setFont(Font.font("System", 12));

            Label contact = new Label("Contact: " + farmer.getPhoneNumber());
            contact.setFont(Font.font("System", 12));

            Label email = new Label("Email: " + farmer.getEmail());
            email.setFont(Font.font("System", 12));

            Label location = new Label("Location: " + farmer.getFarmLocation());
            location.setFont(Font.font("System", 12));

            Button contactButton = new Button("Contact Farmer");
            contactButton.getStyleClass().add("button-primary");
            contactButton.setMaxWidth(Double.MAX_VALUE);
            contactButton.setOnAction(e -> {
                showAlert("Contact Information",
                        "Farmer: " + farmer.getFullName() + "\n" +
                                "Farm: " + farmer.getFarmName() + "\n" +
                                "Phone: " + farmer.getPhoneNumber() + "\n" +
                                "Email: " + farmer.getEmail() + "\n" +
                                "Location: " + farmer.getFarmLocation());
            });

            farmerDetails.getChildren().addAll(farmerName, farmName, contact, email, location, contactButton);
            farmerDetails.setVisible(true);
            farmerDetails.setManaged(true);

            // Force layout update
            farmerDetails.requestLayout();
            System.out.println("Farmer details added to UI");
        } else {
            System.out.println("No farmer found for product");
            showError("Error", "Could not find farmer details");
        }
    }

    private Farmer findFarmerById(String farmerId) {
        // Return the demo farmer for the demo product
        return demoFarmer;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private VBox createOrdersPage(Farmer farmer) {
        VBox ordersSection = new VBox(20);
        ordersSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        ordersSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Orders Management");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer);

        // Create Orders Table
        TableView<Order> ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ordersTable.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");

        // Customer Name Column
        TableColumn<Order, String> customerNameCol = new TableColumn<>("Customer Name");
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerNameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Location Column
        TableColumn<Order, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Product Name Column
        TableColumn<Order, String> productNameCol = new TableColumn<>("Product Name");
        productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productNameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Quantity Column
        TableColumn<Order, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Status Column
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Actions Column
        TableColumn<Order, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accept");
            private final Button rejectBtn = new Button("Reject");
            private final HBox buttons = new HBox(10, acceptBtn, rejectBtn);

            {
                acceptBtn.getStyleClass().add("button-primary");
                rejectBtn.getStyleClass().add("button-danger");
                buttons.setAlignment(Pos.CENTER);
                acceptBtn.setFont(Font.font("Roboto", 12));
                rejectBtn.setFont(Font.font("Roboto", 12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Set header font for all columns
        for (TableColumn<Order, ?> column : ordersTable.getColumns()) {
            column.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");
        }

        ordersTable.getColumns().addAll(customerNameCol, locationCol, productNameCol, quantityCol, statusCol,
                actionsCol);

        // Add sample orders (in a real app, this would come from a database)
        ObservableList<Order> orders = FXCollections.observableArrayList(
                new Order("John Doe", "123 Main St, City", "Organic Tomatoes", 5, "Pending"),
                new Order("Jane Smith", "456 Oak Ave, Town", "Fresh Lettuce", 3, "Pending"),
                new Order("Mike Johnson", "789 Pine Rd, Village", "Organic Carrots", 2, "Pending"));
        ordersTable.setItems(orders);

        ordersSection.getChildren().addAll(header, ordersTable);
        return ordersSection;
    }

    // Order class to represent order data
    public static class Order {
        private final String customerName;
        private final String location;
        private final String productName;
        private final int quantity;
        private final String status;

        public Order(String customerName, String location, String productName, int quantity, String status) {
            this.customerName = customerName;
            this.location = location;
            this.productName = productName;
            this.quantity = quantity;
            this.status = status;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getLocation() {
            return location;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getStatus() {
            return status;
        }
    }

    private VBox createDashboardContent(Farmer farmer) {
        VBox dashboardSection = new VBox(20);
        dashboardSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        dashboardSection.setPadding(new Insets(20));

        // Header with refresh button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Dashboard Overview");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 28));

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 20;");
        refreshBtn.setOnAction(e -> refreshDashboard());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, refreshBtn, spacer);

        // Statistics Cards Grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(20));

        // Total Products Card
        VBox totalProductsCard = createStatCard(
                "Total Products",
                String.valueOf(productsList.size()),
                "Products listed in your store",
                "📦",
                () -> showProductDetails());

        // Pending Orders Card
        VBox pendingOrdersCard = createStatCard(
                "Pending Orders",
                "3",
                "Orders awaiting your response",
                "⏳",
                () -> showPendingOrders());

        // Fulfilled Orders Card
        VBox fulfilledOrdersCard = createStatCard(
                "Fulfilled Orders",
                "12",
                "Successfully completed orders",
                "✅",
                () -> showFulfilledOrders());

        // Total Revenue Card
        VBox totalRevenueCard = createStatCard(
                "Total Revenue",
                "$1,234.56",
                "Total earnings from all orders",
                "💰",
                () -> showRevenueDetails());

        // New Revenue Card
        VBox newRevenueCard = createStatCard(
                "New Revenue",
                "$234.56",
                "Earnings from last 7 days",
                "📈",
                () -> showNewRevenueDetails());

        // New Messages Card
        VBox newMessagesCard = createStatCard(
                "New Messages",
                "5",
                "Unread customer inquiries",
                "📩",
                () -> showMessages());

        // Add cards to grid
        statsGrid.add(totalProductsCard, 0, 0);
        statsGrid.add(pendingOrdersCard, 1, 0);
        statsGrid.add(fulfilledOrdersCard, 2, 0);
        statsGrid.add(totalRevenueCard, 0, 1);
        statsGrid.add(newRevenueCard, 1, 1);
        statsGrid.add(newMessagesCard, 2, 1);

        // Recent Activity Section with refresh
        VBox recentActivitySection = new VBox(10);
        recentActivitySection.setPadding(new Insets(20));
        recentActivitySection.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        HBox activityHeader = new HBox(10);
        Label recentActivityTitle = new Label("Recent Activity");
        recentActivityTitle.setFont(Font.font("Roboto", FontWeight.BOLD, 24));

        Button refreshActivityBtn = new Button("↻");
        refreshActivityBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15;");
        refreshActivityBtn.setOnAction(e -> refreshRecentActivity());

        activityHeader.getChildren().addAll(recentActivityTitle, refreshActivityBtn);

        // Sample recent activities
        VBox activitiesList = new VBox(10);
        activitiesList.getChildren().addAll(
                createActivityItem("New order received from John Doe", "2 minutes ago", true),
                createActivityItem("Product 'Organic Tomatoes' stock updated", "1 hour ago", false),
                createActivityItem("New message from Jane Smith", "3 hours ago", true));

        recentActivitySection.getChildren().addAll(activityHeader, activitiesList);

        dashboardSection.getChildren().addAll(header, statsGrid, recentActivitySection);
        return dashboardSection;
    }

    private VBox createStatCard(String title, String value, String description, String icon, Runnable onClick) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setPrefHeight(200);

        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 3); -fx-cursor: hand;");
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
            card.setTranslateY(0);
        });

        // Add click handler
        card.setOnMouseClicked(e -> onClick.run());

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("System", 36));
        iconLabel.setStyle("-fx-padding: 0 0 10 0;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(Font.font("Roboto", 14));
        descriptionLabel.setTextFill(Color.GRAY);

        // Add trend indicator if applicable
        if (title.contains("Revenue") || title.contains("Orders")) {
            HBox trendBox = new HBox(5);
            Label trendIcon = new Label("↑");
            trendIcon.setTextFill(Color.GREEN);
            trendIcon.setFont(Font.font("System", 16));
            Label trendText = new Label("12% from last week");
            trendText.setFont(Font.font("Roboto", 14));
            trendText.setTextFill(Color.GRAY);
            trendBox.getChildren().addAll(trendIcon, trendText);
            card.getChildren().addAll(iconLabel, valueLabel, titleLabel, descriptionLabel, trendBox);
        } else {
            card.getChildren().addAll(iconLabel, valueLabel, titleLabel, descriptionLabel);
        }

        return card;
    }

    private HBox createActivityItem(String activity, String time, boolean isNew) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: " + (isNew ? "#f0fdf4" : "white") + "; -fx-background-radius: 5;");

        // Add hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: " + (isNew ? "#dcfce7" : "#f8f9fa")
                    + "; -fx-background-radius: 5; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: " + (isNew ? "#f0fdf4" : "white") + "; -fx-background-radius: 5;");
        });

        Label activityLabel = new Label(activity);
        activityLabel.setFont(Font.font("Roboto", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Roboto", 12));
        timeLabel.setTextFill(Color.GRAY);

        item.getChildren().addAll(activityLabel, spacer, timeLabel);

        return item;
    }

    // Action handlers for card clicks
    private void showProductDetails() {
        // Switch to products page
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createMainContent(demoFarmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        dashboardRoot.setCenter(scrollPane);
    }

    private void showPendingOrders() {
        // Show pending orders dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pending Orders");
        alert.setHeaderText("Orders Awaiting Response");
        alert.setContentText("You have 3 pending orders that need your attention.");
        alert.showAndWait();
    }

    private void showFulfilledOrders() {
        // Show fulfilled orders dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fulfilled Orders");
        alert.setHeaderText("Completed Orders");
        alert.setContentText("You have successfully completed 12 orders.");
        alert.showAndWait();
    }

    private void showRevenueDetails() {
        // Show revenue details dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Revenue Details");
        alert.setHeaderText("Total Revenue Breakdown");
        alert.setContentText(
                "Total Revenue: $1,234.56\nBreakdown by product category:\n- Vegetables: $500.00\n- Fruits: $400.00\n- Other: $334.56");
        alert.showAndWait();
    }

    private void showNewRevenueDetails() {
        // Show new revenue details dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Revenue");
        alert.setHeaderText("Last 7 Days Revenue");
        alert.setContentText(
                "New Revenue: $234.56\nDaily breakdown:\n- Today: $50.00\n- Yesterday: $45.00\n- Previous days: $139.56");
        alert.showAndWait();
    }

    private void showMessages() {
        // Show messages dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Messages");
        alert.setHeaderText("Unread Messages");
        alert.setContentText("You have 5 unread messages from customers.");
        alert.showAndWait();
    }

    private void refreshDashboard() {
        // Refresh all dashboard data
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createDashboardContent(demoFarmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        dashboardRoot.setCenter(scrollPane);
    }

    private void refreshRecentActivity() {
        // Refresh recent activity section
        // In a real app, this would fetch new data from the server
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Refresh");
        alert.setHeaderText("Recent Activity Updated");
        alert.setContentText("Recent activity has been refreshed.");
        alert.showAndWait();
    }

    private VBox createMessagesPage() {
        VBox messagesSection = new VBox(20);
        messagesSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        messagesSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Customer Messages");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 29)); // Increased from 24
        title.setTextFill(Color.BLACK);

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 20;");
        refreshBtn.setOnAction(e -> refreshMessages());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, refreshBtn, spacer);

        // Messages List
        VBox messagesList = new VBox(15);
        messagesList.setPadding(new Insets(10));

        // Sample messages (in a real app, these would come from a database)
        messagesList.getChildren().addAll(
                createMessageItem("John Doe", "I'm interested in your organic tomatoes. Do you have any available?",
                        "2 hours ago", true),
                createMessageItem("Jane Smith", "What's the minimum order quantity for your products?", "5 hours ago",
                        true),
                createMessageItem("Mike Johnson", "Can you deliver to Accra Central?", "1 day ago", false),
                createMessageItem("Sarah Wilson", "Do you offer bulk discounts?", "2 days ago", false));

        messagesSection.getChildren().addAll(header, messagesList);
        return messagesSection;
    }

    private VBox createMessageItem(String sender, String message, String time, boolean isUnread) {
        VBox messageItem = new VBox(10);
        messageItem.setStyle("-fx-background-color: " + (isUnread ? "#f0fdf4" : "white")
                + "; -fx-background-radius: 10; -fx-padding: 15;");

        // Add hover effect
        messageItem.setOnMouseEntered(e -> {
            messageItem.setStyle("-fx-background-color: " + (isUnread ? "#dcfce7" : "#f8f9fa")
                    + "; -fx-background-radius: 10; -fx-cursor: hand;");
        });
        messageItem.setOnMouseExited(e -> {
            messageItem.setStyle(
                    "-fx-background-color: " + (isUnread ? "#f0fdf4" : "white") + "; -fx-background-radius: 10;");
        });

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label senderLabel = new Label(sender);
        senderLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 19)); // Increased from 14
        senderLabel.setTextFill(Color.BLACK);

        if (isUnread) {
            Label unreadLabel = new Label("•");
            unreadLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            unreadLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 25)); // Increased from 20
            header.getChildren().add(unreadLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Roboto", 17)); // Increased from 12
        timeLabel.setTextFill(Color.BLACK);

        header.getChildren().addAll(senderLabel, spacer, timeLabel);

        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Roboto", 19)); // Increased from 14
        messageLabel.setTextFill(Color.BLACK);
        messageLabel.setWrapText(true);

        Button replyBtn = new Button("Reply");
        replyBtn.getStyleClass().add("button-primary");
        replyBtn.setMaxWidth(100);
        replyBtn.setOnAction(e -> showReplyDialog(sender));

        messageItem.getChildren().addAll(header, messageLabel, replyBtn);
        return messageItem;
    }

    private void showReplyDialog(String recipient) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reply to " + recipient);
        dialog.setHeaderText("Write your reply");

        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your message here...");
        replyArea.setPrefRowCount(5);
        replyArea.setWrapText(true);

        dialog.getDialogPane().setContent(replyArea);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                return replyArea.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reply -> {
            showAlert("Message Sent", "Your reply has been sent to " + recipient);
        });
    }

    private void refreshMessages() {
        // In a real app, this would fetch new messages from the server
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Refresh");
        alert.setHeaderText("Messages Updated");
        alert.setContentText("Messages have been refreshed.");
        alert.showAndWait();
    }

    private VBox createSettingsPage() {
        VBox settingsSection = new VBox(20);
        settingsSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        settingsSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Settings");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer);

        // Settings Categories Grid
        GridPane categoriesGrid = new GridPane();
        categoriesGrid.setHgap(20);
        categoriesGrid.setVgap(20);
        categoriesGrid.setPadding(new Insets(20));

        // Create category buttons
        Button accountSettingsBtn = createSettingsCategoryButton("Account Settings", "Manage your personal information",
                "👤");
        Button farmInfoBtn = createSettingsCategoryButton("Farm Information", "Update your farm details", "🏡");
        Button produceDefaultsBtn = createSettingsCategoryButton("Produce Defaults",
                "Set default values for your products", "🌾");
        Button privacySecurityBtn = createSettingsCategoryButton("Privacy & Security", "Manage your privacy settings",
                "🔒");
        Button termsSupportBtn = createSettingsCategoryButton("Terms & Support", "View terms and get help", "📋");
        Button accountManagementBtn = createSettingsCategoryButton("Account Management", "Manage your account status",
                "⚙️");

        // Add click handlers
        Runnable showCategories = () -> showSettingsContent(new VBox(categoriesGrid), () -> {
        }); // disables back button on main
        accountSettingsBtn.setOnAction(e -> showSettingsContent(createAccountSettings(), showCategories));
        farmInfoBtn.setOnAction(e -> showSettingsContent(createFarmInformation(), showCategories));
        produceDefaultsBtn.setOnAction(e -> showSettingsContent(createProduceDefaults(), showCategories));
        privacySecurityBtn.setOnAction(e -> showSettingsContent(createPrivacySecurity(), showCategories));
        termsSupportBtn.setOnAction(e -> showSettingsContent(createTermsSupport(), showCategories));
        accountManagementBtn.setOnAction(e -> showSettingsContent(createAccountManagement(), showCategories));

        // Add buttons to grid
        categoriesGrid.add(accountSettingsBtn, 0, 0);
        categoriesGrid.add(farmInfoBtn, 1, 0);
        categoriesGrid.add(produceDefaultsBtn, 2, 0);
        categoriesGrid.add(privacySecurityBtn, 0, 1);
        categoriesGrid.add(termsSupportBtn, 1, 1);
        categoriesGrid.add(accountManagementBtn, 2, 1);

        // Content area for settings
        StackPane contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        contentArea.setPadding(new Insets(20));
        contentArea.setPrefHeight(400);

        // Initially show the categories grid
        contentArea.getChildren().add(categoriesGrid);

        settingsSection.getChildren().addAll(header, contentArea);
        return settingsSection;
    }

    private Button createSettingsCategoryButton(String title, String description, String icon) {
        VBox buttonContent = new VBox(5);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(15));

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("System", 29)); // Increased from 24
        iconLabel.setTextFill(Color.BLACK);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 21)); // Increased from 16
        titleLabel.setTextFill(Color.BLACK);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Roboto", 17)); // Increased from 12
        descLabel.setTextFill(Color.BLACK);
        descLabel.setWrapText(true);

        buttonContent.getChildren().addAll(iconLabel, titleLabel, descLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(150);
        button.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");

        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-border-color: " + COLOR_PRIMARY_GREEN
                            + "; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;");
            titleLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            descLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            iconLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        });
        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");
            titleLabel.setTextFill(Color.BLACK);
            descLabel.setTextFill(Color.BLACK);
            iconLabel.setTextFill(Color.BLACK);
        });

        return button;
    }

    private void showSettingsContent(VBox content, Runnable onBack) {
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        VBox wrapper = new VBox();
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(30, 0, 0, 0));
        wrapper.setSpacing(20);
        // Back button
        Button backBtn = new Button("← Back to Categories");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_PRIMARY_GREEN
                + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        backBtn.setOnAction(e -> onBack.run());
        wrapper.getChildren().add(backBtn);
        // Centered content
        HBox centerBox = new HBox(content);
        centerBox.setAlignment(Pos.CENTER);
        wrapper.getChildren().add(centerBox);
        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createAccountSettings() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        // Profile Picture
        HBox profileSection = new HBox(15);
        profileSection.setAlignment(Pos.CENTER_LEFT);

        StackPane profilePicture = new StackPane();
        profilePicture.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 50;");
        profilePicture.setPrefSize(80, 80);

        ImageView profileImage = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profileImage.setImage(image);
        } catch (Exception e) {
            Label initials = new Label(demoFarmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 32));
            initials.setTextFill(Color.WHITE);
            profilePicture.getChildren().add(initials);
        }

        profileImage.setFitWidth(80);
        profileImage.setFitHeight(80);
        profileImage.setPreserveRatio(true);
        profileImage.setSmooth(true);
        profilePicture.getChildren().add(profileImage);

        Button changePhotoBtn = new Button("Change Photo");
        changePhotoBtn.getStyleClass().add("button-secondary");
        VBox.setMargin(changePhotoBtn, new Insets(20, 0, 0, 0));

        profileSection.getChildren().addAll(profilePicture, changePhotoBtn);

        // Personal Information
        GridPane personalInfo = new GridPane();
        personalInfo.setHgap(10);
        personalInfo.setVgap(10);

        TextField fullNameField = new TextField(demoFarmer.getFullName());
        TextField emailField = new TextField(demoFarmer.getEmail());
        TextField phoneField = new TextField(demoFarmer.getPhoneNumber());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter new password");

        Label fullNameLabel = new Label("Full Name:");
        Label emailLabel = new Label("Email:");
        Label phoneLabel = new Label("Phone:");
        Label passwordLabel = new Label("Password:");

        fullNameLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        emailLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        phoneLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        passwordLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        personalInfo.add(fullNameLabel, 0, 0);
        personalInfo.add(fullNameField, 1, 0);
        personalInfo.add(emailLabel, 0, 1);
        personalInfo.add(emailField, 1, 1);
        personalInfo.add(phoneLabel, 0, 2);
        personalInfo.add(phoneField, 1, 2);
        personalInfo.add(passwordLabel, 0, 3);
        personalInfo.add(passwordField, 1, 3);

        // Language Preference
        Label languageLabel = new Label("Language Preference:");
        languageLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "French", "Spanish", "Arabic");
        languageCombo.setValue("English");
        languageCombo.setPromptText("Select Language");

        // 2FA Toggle
        ToggleSwitch twoFactorSwitch = new ToggleSwitch("Enable Two-Factor Authentication");

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(profileSection, personalInfo,
                languageLabel, languageCombo,
                twoFactorSwitch, saveBtn);

        return content;
    }

    private VBox createFarmInformation() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        TextField farmNameField = new TextField(demoFarmer.getFarmName());
        farmNameField.setPromptText("Farm Name");

        Label farmNameLabel = new Label("Farm Name:");
        Label regionLabel = new Label("Region:");
        Label locationLabel = new Label("Specific Location:");

        farmNameLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        regionLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        locationLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        // Location Selection
        ComboBox<String> regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll("Greater Accra", "Ashanti", "Western", "Eastern", "Central", "Northern");
        regionCombo.setPromptText("Select Region");

        TextField specificLocation = new TextField();
        specificLocation.setPromptText("Specific Location/Address");

        Button viewMapBtn = new Button("View on Map");
        viewMapBtn.getStyleClass().add("button-secondary");

        Button saveBtn = new Button("Save Farm Information");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                farmNameLabel, farmNameField,
                regionLabel, regionCombo,
                locationLabel, specificLocation,
                viewMapBtn, saveBtn);

        return content;
    }

    private VBox createProduceDefaults() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Label unitLabel = new Label("Preferred Unit:");
        Label currencyLabel = new Label("Default Currency:");
        Label priceRangeLabel = new Label("Price Range Guidance:");

        unitLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        currencyLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        priceRangeLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        // Preferred Unit
        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("Kilograms (kg)", "Bags", "Pounds (lbs)", "Crates", "Litres (L)");
        unitCombo.setPromptText("Select Preferred Unit");

        // Default Currency
        ComboBox<String> currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll("Ghana Cedi (GHS)", "US Dollar (USD)", "Euro (EUR)", "British Pound (GBP)");
        currencyCombo.setPromptText("Select Currency");

        // Price Range
        HBox priceRange = new HBox(10);
        TextField minPrice = new TextField();
        minPrice.setPromptText("Minimum Price");
        TextField maxPrice = new TextField();
        maxPrice.setPromptText("Maximum Price");
        priceRange.getChildren().addAll(minPrice, maxPrice);

        CheckBox dynamicPricing = new CheckBox("Enable Dynamic Pricing");
        dynamicPricing.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        Button saveBtn = new Button("Save Preferences");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                unitLabel, unitCombo,
                currencyLabel, currencyCombo,
                priceRangeLabel, priceRange,
                dynamicPricing, saveBtn);

        return content;
    }

    private VBox createPrivacySecurity() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        // Location Visibility
        ToggleSwitch locationVisibility = new ToggleSwitch("Show Farm Location to Buyers");

        // Blocked Buyers
        VBox blockedBuyers = new VBox(10);
        Label blockedLabel = new Label("Blocked Buyers:");
        blockedLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ListView<String> blockedList = new ListView<>();
        blockedList.getItems().addAll("John Doe", "Jane Smith", "Mike Johnson");
        blockedList.setPrefHeight(100);

        Button unblockBtn = new Button("Unblock Selected");
        unblockBtn.getStyleClass().add("button-secondary");

        blockedBuyers.getChildren().addAll(
                blockedLabel,
                blockedList,
                unblockBtn);

        // Session History
        VBox sessionHistory = new VBox(10);
        Label sessionsLabel = new Label("Recent Sessions:");
        sessionsLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ListView<String> sessions = new ListView<>();
        sessions.getItems().addAll(
                "Login from Chrome - Accra, Ghana (2 hours ago)",
                "Login from Mobile App - Kumasi, Ghana (1 day ago)",
                "Login from Firefox - Tema, Ghana (3 days ago)");
        sessions.setPrefHeight(100);

        sessionHistory.getChildren().addAll(
                sessionsLabel,
                sessions);

        Button saveBtn = new Button("Save Privacy Settings");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                locationVisibility,
                blockedBuyers,
                sessionHistory,
                saveBtn);

        return content;
    }

    private VBox createTermsSupport() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Button termsBtn = new Button("View Terms of Use");
        Button privacyBtn = new Button("View Privacy Policy");
        Button supportBtn = new Button("Contact Support");
        Button faqBtn = new Button("Help Center / FAQ");

        termsBtn.getStyleClass().add("button-secondary");
        privacyBtn.getStyleClass().add("button-secondary");
        supportBtn.getStyleClass().add("button-secondary");
        faqBtn.getStyleClass().add("button-secondary");

        content.getChildren().addAll(termsBtn, privacyBtn, supportBtn, faqBtn);
        return content;
    }

    private VBox createAccountManagement() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Button logoutBtn = new Button("Logout");
        Button deactivateBtn = new Button("Deactivate Account");
        Button deleteBtn = new Button("Delete Account");

        logoutBtn.getStyleClass().add("button-primary");
        deactivateBtn.getStyleClass().add("button-danger");
        deleteBtn.getStyleClass().add("button-danger");

        deactivateBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to deactivate your account? You can reactivate by contacting support.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Deactivate Account");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    if (currentCustomer != null) {
                        var stmt = dbConnection.prepareStatement("UPDATE Customer SET deactivated=TRUE WHERE username=?");
                        stmt.setString(1, currentCustomer.getUsername());
                        stmt.executeUpdate();
                    } else if (demoFarmer != null) {
                        var stmt = dbConnection.prepareStatement("UPDATE Farmer SET deactivated=TRUE WHERE username=?");
                        stmt.setString(1, demoFarmer.getUsername());
                        stmt.executeUpdate();
                    }
                    showAlert("Deactivated", "Your account has been deactivated.");
                    primaryStage.setScene(scene);
                    primaryStage.setTitle("Farmers & Customers Interaction App");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to deactivate account: " + ex.getMessage());
                }
            }
        });
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently delete your account? This cannot be undone.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Delete Account");
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                try {
                    if (currentCustomer != null) {
                        var stmt = dbConnection.prepareStatement("DELETE FROM Customer WHERE username=?");
                        stmt.setString(1, currentCustomer.getUsername());
                        stmt.executeUpdate();
                    } else if (demoFarmer != null) {
                        var stmt = dbConnection.prepareStatement("DELETE FROM Farmer WHERE username=?");
                        stmt.setString(1, demoFarmer.getUsername());
                        stmt.executeUpdate();
                    }
                    showAlert("Deleted", "Your account has been deleted.");
                    primaryStage.setScene(scene);
                    primaryStage.setTitle("Farmers & Customers Interaction App");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to delete account: " + ex.getMessage());
                }
            }
        });
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });
        content.getChildren().addAll(logoutBtn, deactivateBtn, deleteBtn);
        return content;
    }

    // Custom Toggle Switch Control
    private class ToggleSwitch extends HBox {
        private final Label label;
        private final Button button;
        private boolean selected;

        public ToggleSwitch(String text) {
            label = new Label(text);
            label.setFont(Font.font("Roboto", 14));
            label.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

            button = new Button();
            button.setPrefSize(50, 25);
            button.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 12;");
            button.setOnAction(e -> toggle());

            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(label, button);
        }

        private void toggle() {
            selected = !selected;
            button.setStyle(selected ? "-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-background-radius: 12;"
                    : "-fx-background-color: #e5e7eb; -fx-background-radius: 12;");
        }

        public boolean isSelected() {
            return selected;
        }
    }

    private VBox createCustomerMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Featured Products Section
        Label featuredTitle = new Label("Featured Products");
        featuredTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane featuredProducts = new FlowPane();
        featuredProducts.setHgap(20);
        featuredProducts.setVgap(20);
        featuredProducts.setPrefWrapLength(800);

        // Add enhanced sample featured products
        List<Product> sampleProducts = loadProductsFromDatabase();
        for (Product product : sampleProducts) {
            VBox productCard = createProductCard(product);
            featuredProducts.getChildren().add(productCard);
        }

        // Recent Orders Section
        Label ordersTitle = new Label("Recent Orders");
        ordersTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add some sample orders
        for (int i = 0; i < 3; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate);
            ordersList.getChildren().add(orderItem);
        }

        // Add all sections to the content
        content.getChildren().addAll(
                featuredTitle,
                featuredProducts,
                ordersTitle,
                ordersList);

        return content;
    }

    private List<Product> loadProductsFromDatabase() {
        List<Product> products = new ArrayList<>();
        try {
            String selectProductsSQL = "SELECT p.*, f.fullName as farmerName FROM Product p " +
                    "JOIN Farmer f ON p.farmerId = f.id ORDER BY p.id DESC";
            var statement = dbConnection.createStatement();
            var resultSet = statement.executeQuery(selectProductsSQL);
            
            while (resultSet.next()) {
                Product product = new Product(
                    resultSet.getString("name"),
                    resultSet.getDouble("price"),
                    resultSet.getString("description"),
                    resultSet.getString("unit"),
                    resultSet.getInt("quantity"),
                    String.valueOf(resultSet.getLong("farmerId")),
                    "General", // Default category
                    true // Available
                );
                product.setId(String.valueOf(resultSet.getLong("id")));
                product.setImagePath(resultSet.getString("imagePath"));
                product.setOrigin(resultSet.getString("farmerName") + "'s Farm");
                products.add(product);
            }
        } catch (Exception e) {
            System.err.println("Error loading products from database: " + e.getMessage());
            e.printStackTrace();
            // If database loading fails, return sample products as fallback
            return createSampleEnhancedProducts();
        }
        
        // If no products in database, return sample products
        if (products.isEmpty()) {
            return createSampleEnhancedProducts();
        }
        
        return products;
    }

    // Sample enhanced products for demonstration
    private List<Product> createSampleEnhancedProducts() {
        List<Product> products = new ArrayList<>();
        
        // Create some sample products with enhanced features
        Product tomatoes = new Product("Organic Cherry Tomatoes", 4.99, "Fresh, sweet cherry tomatoes grown organically", "kg", 25, "farmer1", "Vegetables", true);
        tomatoes.setSeasonal(true);
        tomatoes.addTag("fresh");
        tomatoes.addTag("local");
        tomatoes.setOrigin("Green Valley Farm");
        tomatoes.setHarvestDate("2024-03-15");
        tomatoes.setShelfLife("7 days");
        tomatoes.setStorageInstructions("Keep refrigerated");
        
        // Add some reviews
        tomatoes.addReview(new ProductReview(tomatoes.getId(), "customer1", "John D.", 5, "Amazing quality!", "Amazing quality! Very fresh and flavorful."));
        tomatoes.addReview(new ProductReview(tomatoes.getId(), "customer2", "Sarah M.", 4, "Good tomatoes", "Good tomatoes, will buy again."));
        tomatoes.addReview(new ProductReview(tomatoes.getId(), "customer3", "Mike R.", 5, "Perfect for salads!", "Perfect for salads!"));
        
        products.add(tomatoes);
        
        // Low stock product
        Product potatoes = new Product("Golden Potatoes", 2.99, "Premium golden potatoes perfect for roasting", "kg", 8, "farmer2", "Vegetables", false);
        potatoes.addTag("fresh");
        potatoes.addTag("versatile");
        potatoes.setOrigin("Hillside Farm");
        potatoes.setMinStockLevel(15); // This will make it low stock
        potatoes.addReview(new ProductReview(potatoes.getId(), "customer4", "Emma T.", 4, "Great for fries!", "Great for making fries!"));
        
        products.add(potatoes);
        
        // Out of stock product
        Product apples = new Product("Organic Red Apples", 3.49, "Crisp and sweet organic red apples", "kg", 0, "farmer1", "Fruits", true);
        apples.setSeasonal(true);
        apples.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        apples.addTag("organic");
        apples.addTag("sweet");
        apples.setOrigin("Orchard Hills");
        
        products.add(apples);
        
        // Premium product with high rating
        Product carrots = new Product("Premium Baby Carrots", 3.99, "Sweet and tender baby carrots, perfect for snacking", "kg", 30, "farmer3", "Vegetables", true);
        carrots.addTag("premium");
        carrots.addTag("baby");
        carrots.addTag("sweet");
        carrots.setOrigin("Sunshine Farm");
        
        // Add multiple reviews for high rating
        carrots.addReview(new ProductReview(carrots.getId(), "customer5", "Lisa K.", 5, "Best carrots ever!", "Best carrots I've ever had!"));
        carrots.addReview(new ProductReview(carrots.getId(), "customer6", "David P.", 5, "So sweet and crunchy!", "So sweet and crunchy!"));
        carrots.addReview(new ProductReview(carrots.getId(), "customer7", "Anna B.", 5, "Kids love these!", "Kids love these!"));
        carrots.addReview(new ProductReview(carrots.getId(), "customer8", "Tom W.", 4, "Very good quality", "Very good quality"));
        
        products.add(carrots);
        
        return products;
    }

    // Enhanced createProductCard method for Product objects
    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(220);
        card.setPrefHeight(320);

        // Product Image (placeholder)
        Rectangle imagePlaceholder = new Rectangle(190, 120);
        imagePlaceholder.setFill(Color.LIGHTGRAY);
        imagePlaceholder.setArcWidth(10);
        imagePlaceholder.setArcHeight(10);

        // Product badges container
        HBox badgesContainer = new HBox(5);
        badgesContainer.setAlignment(Pos.TOP_LEFT);
        
        // Category badge
        Label categoryBadge = new Label(ProductCategory.getCategoryIcon(product.getCategory()) + " " + product.getCategory());
        categoryBadge.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; -fx-padding: 2px 6px; -fx-background-radius: 10px; -fx-font-size: 10px; -fx-font-weight: bold;");
        badgesContainer.getChildren().add(categoryBadge);
        
        // Organic badge
        if (product.isOrganic()) {
            Label organicBadge = new Label("🌱 ORGANIC");
            organicBadge.setStyle("-fx-background-color: #E8F5E8; -fx-text-fill: #2E7D32; -fx-padding: 2px 6px; -fx-background-radius: 10px; -fx-font-size: 10px; -fx-font-weight: bold;");
            badgesContainer.getChildren().add(organicBadge);
        }
        
        // Seasonal badge
        if (product.isSeasonal()) {
            Label seasonalBadge = new Label("🍂 SEASONAL");
            seasonalBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-padding: 2px 6px; -fx-background-radius: 10px; -fx-font-size: 10px; -fx-font-weight: bold;");
            badgesContainer.getChildren().add(seasonalBadge);
        }

        // Product name
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        nameLabel.setWrapText(true);

        // Rating display (if has reviews)
        HBox ratingContainer = new HBox(5);
        ratingContainer.setAlignment(Pos.CENTER_LEFT);
        
        if (product.getAverageRating() > 0) {
            // Star rating
            for (int i = 1; i <= 5; i++) {
                Label star = new Label(i <= product.getAverageRating() ? "★" : "☆");
                star.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px;");
                ratingContainer.getChildren().add(star);
            }
            
            Label ratingText = new Label(String.format("%.1f (%d)", product.getAverageRating(), product.getReviews().size()));
            ratingText.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            ratingContainer.getChildren().add(ratingText);
        } else {
            Label noRating = new Label("No reviews yet");
            noRating.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-font-style: italic;");
            ratingContainer.getChildren().add(noRating);
        }

        // Product description
        Label descLabel = new Label(product.getDescription());
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        descLabel.setPrefHeight(30);

        // Availability status
        HBox availabilityContainer = new HBox(5);
        availabilityContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label statusIndicator = new Label();
        Label availabilityLabel = new Label();
        
        if (!product.isAvailable()) {
            switch (product.getStatus()) {
                case OUT_OF_STOCK:
                    statusIndicator.setText("�");
                    availabilityLabel.setText("Out of Stock");
                    availabilityLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 12px; -fx-font-weight: bold;");
                    break;
                case INACTIVE:
                    statusIndicator.setText("⚫");
                    availabilityLabel.setText("Not Available");
                    availabilityLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold;");
                    break;
                case DISCONTINUED:
                    statusIndicator.setText("⚫");
                    availabilityLabel.setText("Discontinued");
                    availabilityLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold;");
                    break;
                default:
                    statusIndicator.setText("⚫");
                    availabilityLabel.setText("Unavailable");
                    availabilityLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-font-weight: bold;");
                    break;
            }
        } else if (product.isLowStock()) {
            statusIndicator.setText("🟡");
            availabilityLabel.setText("Low Stock (" + product.getQuantity() + " " + product.getUnit() + ")");
            availabilityLabel.setStyle("-fx-text-fill: #F57C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            statusIndicator.setText("🟢");
            availabilityLabel.setText("In Stock (" + product.getQuantity() + " " + product.getUnit() + ")");
            availabilityLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
        
        availabilityContainer.getChildren().addAll(statusIndicator, availabilityLabel);

        // Price label
        Label priceLabel = new Label(String.format("$%.2f/%s", product.getPrice(), product.getUnit()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2E7D32;");

        // Add to Cart button
        Button addToCartBtn = new Button("Add to Cart");
        boolean canAddToCart = product.isAvailable() && !product.isLowStock(); // Can add if available and not low stock
        
        if (canAddToCart || (product.isAvailable() && product.isLowStock())) { // Allow adding even if low stock
            addToCartBtn.setStyle(
                    "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
            addToCartBtn.setOnAction(e -> {
                cartItems.add(new CartItem(product.getName(), product.getPrice(), product.getUnit(), 1));
                updateCartBadge();
                showNotification("Added to cart: " + product.getName());
            });
        } else {
            addToCartBtn.setText("Unavailable");
            addToCartBtn.setStyle(
                    "-fx-background-color: #BDBDBD; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
            addToCartBtn.setDisable(true);
        }

        card.getChildren().addAll(imagePlaceholder, badgesContainer, nameLabel, ratingContainer, 
                                 descLabel, availabilityContainer, priceLabel, addToCartBtn);
        return card;
    }

    // Legacy createProductCard method for backward compatibility
    private VBox createProductCard(String name, String description, double price, String unit) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(200);
        card.setPrefHeight(250);

        // Product Image (placeholder)
        Rectangle imagePlaceholder = new Rectangle(170, 120);
        imagePlaceholder.setFill(Color.LIGHTGRAY);
        imagePlaceholder.setArcWidth(10);
        imagePlaceholder.setArcHeight(10);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        descLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("$%.2f/%s", price, unit));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2E7D32;");

        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        addToCartBtn.setOnAction(e -> {
            // Add to cart logic
            cartItems.add(new CartItem(name, price, unit, 1));
            updateCartBadge();
            showNotification("Added to cart: " + name);
        });

        card.getChildren().addAll(imagePlaceholder, nameLabel, descLabel, priceLabel, addToCartBtn);
        return card;
    }

    private void showNotification(String message) {
        VBox notification = new VBox();
        notification.setStyle("-fx-background-color: #2E7D32; -fx-padding: 15px; -fx-background-radius: 5px;");
        notification.setMaxWidth(300);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        notification.getChildren().add(messageLabel);

        StackPane.setAlignment(notification, Pos.TOP_RIGHT);
        StackPane.setMargin(notification, new Insets(20));

        // Add to the root stack pane
        root.getChildren().add(notification);

        // Animate and remove after 3 seconds
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notification);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5));

        fadeIn.play();
        fadeOut.play();

        fadeOut.setOnFinished(e -> root.getChildren().remove(notification));
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void showFarmersDirectory(Customer customer) {
        Stage farmersStage = new Stage();
        farmersStage.setTitle("Browse Farmers Directory");
        farmersStage.initOwner(primaryStage);
        farmersStage.initModality(Modality.WINDOW_MODAL);

        VBox farmersContent = new VBox(20);
        farmersContent.setPadding(new Insets(20));
        farmersContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Registered Farmers");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        ScrollPane scrollPane = new ScrollPane();
        VBox farmersList = new VBox(15);
        farmersList.setPadding(new Insets(10));

        // Load farmers from database
        try {
            String selectFarmersSQL = "SELECT * FROM Farmer ORDER BY name";
            var statement = dbConnection.createStatement();
            var resultSet = statement.executeQuery(selectFarmersSQL);

            while (resultSet.next()) {
                VBox farmerCard = createFarmerCard(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("farmLocation"),
                    resultSet.getString("email"),
                    customer
                );
                farmersList.getChildren().add(farmerCard);
            }

            if (farmersList.getChildren().isEmpty()) {
                Label noFarmersLabel = new Label("No farmers registered yet.");
                noFarmersLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
                farmersList.getChildren().add(noFarmersLabel);
            }

        } catch (Exception e) {
            Label errorLabel = new Label("Error loading farmers: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF0000;");
            farmersList.getChildren().add(errorLabel);
        }

        scrollPane.setContent(farmersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 400);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        closeBtn.setOnAction(e -> farmersStage.close());

        farmersContent.getChildren().addAll(titleLabel, scrollPane, closeBtn);

        Scene scene = new Scene(farmersContent, 650, 500);
        farmersStage.setScene(scene);
        farmersStage.show();
    }

    private VBox createFarmerCard(long farmerId, String farmerName, String location, String email, Customer customer) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px; -fx-border-color: #E0E0E0; -fx-border-radius: 10px;");

        Label nameLabel = new Label(farmerName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label locationLabel = new Label("📍 " + (location != null ? location : "Location not specified"));
        locationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Label emailLabel = new Label("✉️ " + email);
        emailLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button viewProductsBtn = new Button("View Products");
        viewProductsBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 15px;");
        viewProductsBtn.setOnAction(e -> showFarmerProducts(farmerId, farmerName));

        Button messageBtn = new Button("Send Message");
        messageBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 15px;");
        messageBtn.setOnAction(e -> showMessageDialog(customer, farmerId, farmerName));

        buttonsBox.getChildren().addAll(viewProductsBtn, messageBtn);
        card.getChildren().addAll(nameLabel, locationLabel, emailLabel, buttonsBox);

        return card;
    }

    private void showFarmerProducts(long farmerId, String farmerName) {
        Stage productsStage = new Stage();
        productsStage.setTitle(farmerName + "'s Products");
        productsStage.initOwner(primaryStage);
        productsStage.initModality(Modality.WINDOW_MODAL);

        VBox productsContent = new VBox(20);
        productsContent.setPadding(new Insets(20));
        productsContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(farmerName + "'s Products");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        ScrollPane scrollPane = new ScrollPane();
        FlowPane productsGrid = new FlowPane();
        productsGrid.setHgap(20);
        productsGrid.setVgap(20);
        productsGrid.setPrefWrapLength(600);

        // Load farmer's products from database
        try {
            String selectProductsSQL = "SELECT * FROM Product WHERE farmerId = ? ORDER BY name";
            var preparedStatement = dbConnection.prepareStatement(selectProductsSQL);
            preparedStatement.setLong(1, farmerId);
            var resultSet = preparedStatement.executeQuery();

            boolean hasProducts = false;
            while (resultSet.next()) {
                hasProducts = true;
                Product product = new Product(
                    resultSet.getString("name"),
                    resultSet.getDouble("price"),
                    resultSet.getString("description"),
                    resultSet.getString("unit"),
                    resultSet.getInt("quantity"),
                    String.valueOf(farmerId),
                    "General",
                    true
                );
                product.setId(String.valueOf(resultSet.getLong("id")));
                product.setImagePath(resultSet.getString("imagePath"));
                product.setOrigin(farmerName + "'s Farm");

                VBox productCard = createProductCard(product);
                productsGrid.getChildren().add(productCard);
            }

            if (!hasProducts) {
                Label noProductsLabel = new Label("This farmer hasn't posted any products yet.");
                noProductsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
                productsGrid.getChildren().add(noProductsLabel);
            }

        } catch (Exception e) {
            Label errorLabel = new Label("Error loading products: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #FF0000;");
            productsGrid.getChildren().add(errorLabel);
        }

        scrollPane.setContent(productsGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(650, 400);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        closeBtn.setOnAction(e -> productsStage.close());

        productsContent.getChildren().addAll(titleLabel, scrollPane, closeBtn);

        Scene scene = new Scene(productsContent, 700, 500);
        productsStage.setScene(scene);
        productsStage.show();
    }

    private void showMessageDialog(Customer customer, long farmerId, String farmerName) {
        Stage messageStage = new Stage();
        messageStage.setTitle("Send Message to " + farmerName);
        messageStage.initOwner(primaryStage);
        messageStage.initModality(Modality.WINDOW_MODAL);

        VBox messageContent = new VBox(15);
        messageContent.setPadding(new Insets(20));
        messageContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Send Message to " + farmerName);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label subjectLabel = new Label("Subject:");
        subjectLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextField subjectField = new TextField();
        subjectField.setPromptText("Enter message subject");
        subjectField.setPrefHeight(35);

        Label messageLabel = new Label("Message:");
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Type your message here...");
        messageArea.setPrefRowCount(6);
        messageArea.setWrapText(true);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button sendBtn = new Button("Send Message");
        sendBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        cancelBtn.setOnAction(e -> messageStage.close());

        sendBtn.setOnAction(e -> {
            String subject = subjectField.getText().trim();
            String message = messageArea.getText().trim();

            if (subject.isEmpty() || message.isEmpty()) {
                showAlert("Error", "Please fill in both subject and message fields.");
                return;
            }

            // Save message to database (you can implement a Messages table)
            try {
                // For now, just show success message
                showAlert("Success", "Your message has been sent to " + farmerName + "!");
                messageStage.close();
            } catch (Exception ex) {
                showAlert("Error", "Failed to send message: " + ex.getMessage());
            }
        });

        buttonsBox.getChildren().addAll(sendBtn, cancelBtn);
        messageContent.getChildren().addAll(titleLabel, subjectLabel, subjectField, messageLabel, messageArea, buttonsBox);

        Scene scene = new Scene(messageContent, 450, 400);
        messageStage.setScene(scene);
        messageStage.show();
    }

    // Report class for TableView
    private static class Report {
        long id;
        String reporterUsername;
        String reportedUsername;
        String role;
        String reason;
        String status;
        public Report(long id, String reporterUsername, String reportedUsername, String role, String reason, String status) {
            this.id = id;
            this.reporterUsername = reporterUsername;
            this.reportedUsername = reportedUsername;
            this.role = role;
            this.reason = reason;
            this.status = status;
        }
    }
    private javafx.collections.ObservableList<Report> getAllReports() {
        javafx.collections.ObservableList<Report> list = javafx.collections.FXCollections.observableArrayList();
        try {
            var stmt = dbConnection.createStatement();
            var rs = stmt.executeQuery("SELECT * FROM Reports ORDER BY createdAt DESC");
            while (rs.next()) {
                list.add(new Report(
                    rs.getLong("id"),
                    rs.getString("reporterUsername"),
                    rs.getString("reportedUsername"),
                    rs.getString("role"),
                    rs.getString("reason"),
                    rs.getString("status")
                ));
            }
        } catch (Exception ex) {
            showAlert("Error", "Failed to fetch reports: " + ex.getMessage());
        }
        return list;
    }

    private void showAnalyticsDashboard() {
        // Create the analytics dashboard with current user ID
        // For demonstration, using "admin" as user ID - in production, use actual user ID
        AnalyticsDashboard dashboard = new AnalyticsDashboard("admin");
        dashboard.show();
    }
}
