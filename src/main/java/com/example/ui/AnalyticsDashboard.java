package com.example.ui;

import com.example.services.AnalyticsService;
import com.example.services.AnalyticsService.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced Analytics Dashboard for Agricultural Marketplace
 * Provides comprehensive business intelligence and insights
 */
public class AnalyticsDashboard {
    
    private Stage stage;
    private BorderPane root;
    private ScrollPane scrollPane;
    private VBox mainContent;
    private String currentUserId;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("#0.0%");
    private DecimalFormat numberFormat = new DecimalFormat("#,##0");
    
    // UI Components
    private Label todayRevenueLabel;
    private Label todayOrdersLabel;
    private Label todayCustomersLabel;
    private ProgressIndicator loadingIndicator;
    private ListView<String> recentActivityList;
    private LineChart<String, Number> revenueChart;
    private PieChart productSalesChart;
    private BarChart<String, Number> customerSegmentChart;
    private VBox alertsContainer;
    
    public AnalyticsDashboard(String userId) {
        this.currentUserId = userId;
        initializeUI();
    }
    
    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("Analytics Dashboard - Agro Marketplace");
        stage.setWidth(1200);
        stage.setHeight(800);
        
        root = new BorderPane();
        
        // Create header
        createHeader();
        
        // Create main content area
        createMainContent();
        
        // Create footer with refresh controls
        createFooter();
        
        // Set up scene
        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
        } catch (Exception e) {
            // CSS file not found, continue without styling
            System.out.println("CSS file not found, using default styling");
        }
        stage.setScene(scene);
        
        // Load initial data
        refreshAllData();
    }
    
    private void createHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("📊 Analytics Dashboard");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        
        // Time period selector
        ComboBox<String> timePeriodCombo = new ComboBox<>();
        timePeriodCombo.getItems().addAll("Today", "This Week", "This Month", "Last 3 Months", "This Year");
        timePeriodCombo.setValue("This Month");
        timePeriodCombo.setStyle("-fx-background-color: white;");
        
        // Export button
        Button exportButton = new Button("📤 Export Report");
        exportButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        exportButton.setOnAction(e -> showExportDialog());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, spacer, new Label("Period:"), timePeriodCombo, exportButton);
        root.setTop(header);
    }
    
    private void createMainContent() {
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F5F5;");
        
        mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        
        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        
        // Create dashboard sections
        createQuickStatsSection();
        createChartsSection();
        createAlertsSection();
        createRecentActivitySection();
        
        scrollPane.setContent(mainContent);
        root.setCenter(scrollPane);
    }
    
    private void createQuickStatsSection() {
        Label sectionTitle = new Label("📈 Today's Overview");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #2E7D32;");
        
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER);
        
        // Revenue card
        VBox revenueCard = createStatCard("💰 Revenue", "$0.00", "#4CAF50");
        todayRevenueLabel = (Label) revenueCard.getChildren().get(1);
        
        // Orders card
        VBox ordersCard = createStatCard("📦 Orders", "0", "#2196F3");
        todayOrdersLabel = (Label) ordersCard.getChildren().get(1);
        
        // Customers card
        VBox customersCard = createStatCard("👥 Customers", "0", "#FF9800");
        todayCustomersLabel = (Label) customersCard.getChildren().get(1);
        
        // Growth card
        VBox growthCard = createStatCard("📊 Growth", "+0%", "#9C27B0");
        
        statsContainer.getChildren().addAll(revenueCard, ordersCard, customersCard, growthCard);
        
        VBox quickStatsSection = new VBox(10);
        quickStatsSection.getChildren().addAll(sectionTitle, statsContainer);
        mainContent.getChildren().add(quickStatsSection);
    }
    
    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(250);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        titleLabel.setStyle("-fx-text-fill: #666;");
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }
    
    private void createChartsSection() {
        Label sectionTitle = new Label("📊 Analytics Charts");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #2E7D32;");
        
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(20);
        chartsGrid.setVgap(20);
        chartsGrid.setAlignment(Pos.CENTER);
        
        // Revenue trend chart
        createRevenueChart();
        VBox revenueChartContainer = createChartContainer("Monthly Revenue Trend", revenueChart);
        
        // Product sales chart
        createProductSalesChart();
        VBox productChartContainer = createChartContainer("Product Sales Distribution", productSalesChart);
        
        // Customer segments chart
        createCustomerSegmentChart();
        VBox customerChartContainer = createChartContainer("Customer Segments", customerSegmentChart);
        
        // Performance metrics chart
        BarChart<String, Number> performanceChart = createPerformanceChart();
        VBox performanceChartContainer = createChartContainer("Performance Metrics", performanceChart);
        
        chartsGrid.add(revenueChartContainer, 0, 0);
        chartsGrid.add(productChartContainer, 1, 0);
        chartsGrid.add(customerChartContainer, 0, 1);
        chartsGrid.add(performanceChartContainer, 1, 1);
        
        VBox chartsSection = new VBox(15);
        chartsSection.getChildren().addAll(sectionTitle, chartsGrid);
        mainContent.getChildren().add(chartsSection);
    }
    
    private VBox createChartContainer(String title, Chart chart) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #333;");
        
        chart.setPrefSize(400, 300);
        
        container.getChildren().addAll(titleLabel, chart);
        return container;
    }
    
    private void createRevenueChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setTitle("Revenue Over Time");
        revenueChart.setLegendVisible(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        
        // Initialize with placeholder data
        series.getData().add(new XYChart.Data<>("Jan", 0));
        series.getData().add(new XYChart.Data<>("Feb", 0));
        series.getData().add(new XYChart.Data<>("Mar", 0));
        series.getData().add(new XYChart.Data<>("Apr", 0));
        series.getData().add(new XYChart.Data<>("May", 0));
        series.getData().add(new XYChart.Data<>("Jun", 0));
        
        revenueChart.getData().add(series);
    }
    
    private void createProductSalesChart() {
        productSalesChart = new PieChart();
        productSalesChart.setTitle("Product Sales");
        productSalesChart.setLegendSide(javafx.geometry.Side.RIGHT);
    }
    
    private void createCustomerSegmentChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        customerSegmentChart = new BarChart<>(xAxis, yAxis);
        customerSegmentChart.setTitle("Customer Segments");
        customerSegmentChart.setLegendVisible(false);
    }
    
    private BarChart<String, Number> createPerformanceChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setUpperBound(100);
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Performance Metrics (%)");
        chart.setLegendVisible(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Order Fulfill.", 0));
        series.getData().add(new XYChart.Data<>("Customer Sat.", 0));
        series.getData().add(new XYChart.Data<>("Delivery Time", 0));
        series.getData().add(new XYChart.Data<>("Quality Score", 0));
        
        chart.getData().add(series);
        return chart;
    }
    
    private void createAlertsSection() {
        Label sectionTitle = new Label("🚨 Alerts & Notifications");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #2E7D32;");
        
        alertsContainer = new VBox(10);
        alertsContainer.setPadding(new Insets(15));
        alertsContainer.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                               "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // Placeholder alert
        addAlertItem("ℹ️ Welcome to Analytics Dashboard", "INFO", "View your business insights here");
        
        VBox alertsSection = new VBox(10);
        alertsSection.getChildren().addAll(sectionTitle, alertsContainer);
        mainContent.getChildren().add(alertsSection);
    }
    
    private void addAlertItem(String message, String severity, String description) {
        HBox alertItem = new HBox(15);
        alertItem.setPadding(new Insets(10));
        alertItem.setAlignment(Pos.CENTER_LEFT);
        
        String backgroundColor = switch (severity) {
            case "HIGH" -> "#FFEBEE";
            case "MEDIUM" -> "#FFF3E0";
            case "LOW" -> "#E8F5E8";
            default -> "#F5F5F5";
        };
        
        alertItem.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 5;");
        
        VBox textContainer = new VBox(5);
        
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        
        textContainer.getChildren().addAll(messageLabel, descriptionLabel);
        
        Button dismissButton = new Button("✕");
        dismissButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999;");
        dismissButton.setOnAction(e -> alertsContainer.getChildren().remove(alertItem));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        alertItem.getChildren().addAll(textContainer, spacer, dismissButton);
        alertsContainer.getChildren().add(alertItem);
    }
    
    private void createRecentActivitySection() {
        Label sectionTitle = new Label("📋 Recent Activity");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setStyle("-fx-text-fill: #2E7D32;");
        
        recentActivityList = new ListView<>();
        recentActivityList.setPrefHeight(200);
        recentActivityList.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        VBox activitySection = new VBox(10);
        activitySection.getChildren().addAll(sectionTitle, recentActivityList);
        mainContent.getChildren().add(activitySection);
    }
    
    private void createFooter() {
        HBox footer = new HBox(20);
        footer.setPadding(new Insets(15));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");
        
        Button refreshButton = new Button("🔄 Refresh Data");
        refreshButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> refreshAllData());
        
        Button settingsButton = new Button("⚙️ Settings");
        settingsButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        settingsButton.setOnAction(e -> showSettingsDialog());
        
        Label lastUpdatedLabel = new Label("Last updated: " + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        lastUpdatedLabel.setStyle("-fx-text-fill: #666;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        footer.getChildren().addAll(lastUpdatedLabel, spacer, refreshButton, settingsButton);
        root.setBottom(footer);
    }
    
    private void refreshAllData() {
        setLoading(true);
        
        // Load dashboard data
        CompletableFuture<DashboardData> dashboardFuture = AnalyticsService.generateDashboardData(currentUserId);
        CompletableFuture<SalesAnalytics> salesFuture = AnalyticsService.generateSalesAnalytics(currentUserId);
        CompletableFuture<CustomerAnalytics> customerFuture = AnalyticsService.generateCustomerAnalytics();
        CompletableFuture<PerformanceMetrics> performanceFuture = AnalyticsService.generatePerformanceMetrics();
        
        CompletableFuture.allOf(dashboardFuture, salesFuture, customerFuture, performanceFuture)
            .thenRun(() -> {
                Platform.runLater(() -> {
                    try {
                        updateDashboardData(dashboardFuture.get());
                        updateSalesData(salesFuture.get());
                        updateCustomerData(customerFuture.get());
                        updatePerformanceData(performanceFuture.get());
                        setLoading(false);
                    } catch (Exception e) {
                        showError("Failed to load analytics data: " + e.getMessage());
                        setLoading(false);
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Error loading analytics: " + throwable.getMessage());
                    setLoading(false);
                });
                return null;
            });
    }
    
    private void updateDashboardData(DashboardData data) {
        todayRevenueLabel.setText(currencyFormat.format(data.getTodayRevenue()));
        todayOrdersLabel.setText(numberFormat.format(data.getTodayOrders()));
        todayCustomersLabel.setText(numberFormat.format(data.getTodayCustomers()));
        
        // Update recent activity
        recentActivityList.getItems().clear();
        recentActivityList.getItems().addAll(data.getRecentActivity());
    }
    
    private void updateSalesData(SalesAnalytics analytics) {
        // Update revenue chart
        XYChart.Series<String, Number> revenueSeries = revenueChart.getData().get(0);
        revenueSeries.getData().clear();
        
        for (Map.Entry<String, Double> entry : analytics.getMonthlyRevenue().entrySet()) {
            revenueSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        // Update product sales chart
        productSalesChart.getData().clear();
        for (Map.Entry<String, Integer> entry : analytics.getProductSales().entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey(), entry.getValue());
            productSalesChart.getData().add(slice);
        }
    }
    
    private void updateCustomerData(CustomerAnalytics analytics) {
        // Update customer segment chart
        XYChart.Series<String, Number> customerSeries = new XYChart.Series<>();
        customerSeries.setName("Customers");
        
        for (Map.Entry<String, Integer> entry : analytics.getCustomerSegmentation().entrySet()) {
            customerSeries.getData().add(new XYChart.Data<>(entry.getKey().replace(" Customers", ""), entry.getValue()));
        }
        
        customerSegmentChart.getData().clear();
        customerSegmentChart.getData().add(customerSeries);
    }
    
    private void updatePerformanceData(PerformanceMetrics metrics) {
        // Update alerts
        alertsContainer.getChildren().clear();
        for (AnalyticsService.Alert alert : metrics.getAlerts()) {
            addAlertItem(alert.getType() + ": " + alert.getMessage(), alert.getSeverity(), 
                        "Reported at " + alert.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        if (loading && !mainContent.getChildren().contains(loadingIndicator)) {
            mainContent.getChildren().add(0, loadingIndicator);
        } else if (!loading) {
            mainContent.getChildren().remove(loadingIndicator);
        }
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Analytics Error");
        alert.setHeaderText("Failed to Load Analytics Data");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showExportDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Export Analytics Report");
        dialog.setHeaderText("Choose export format and options");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton pdfRadio = new RadioButton("PDF Report");
        RadioButton csvRadio = new RadioButton("CSV Data");
        RadioButton excelRadio = new RadioButton("Excel Spreadsheet");
        
        pdfRadio.setToggleGroup(formatGroup);
        csvRadio.setToggleGroup(formatGroup);
        excelRadio.setToggleGroup(formatGroup);
        pdfRadio.setSelected(true);
        
        CheckBox includeCharts = new CheckBox("Include Charts");
        CheckBox includeRawData = new CheckBox("Include Raw Data");
        includeCharts.setSelected(true);
        
        content.getChildren().addAll(
            new Label("Export Format:"),
            pdfRadio, csvRadio, excelRadio,
            new Separator(),
            new Label("Options:"),
            includeCharts, includeRawData
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                RadioButton selected = (RadioButton) formatGroup.getSelectedToggle();
                return selected.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(format -> {
            // Simulate export
            javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            confirmAlert.setTitle("Export Complete");
            confirmAlert.setHeaderText("Analytics Report Exported");
            confirmAlert.setContentText("Report exported successfully as " + format + " format.");
            confirmAlert.showAndWait();
        });
    }
    
    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Analytics Settings");
        dialog.setHeaderText("Configure analytics preferences");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        CheckBox autoRefresh = new CheckBox("Auto-refresh every 5 minutes");
        CheckBox emailReports = new CheckBox("Send daily email reports");
        CheckBox pushNotifications = new CheckBox("Push notifications for alerts");
        
        ComboBox<String> reportFrequency = new ComboBox<>();
        reportFrequency.getItems().addAll("Daily", "Weekly", "Monthly");
        reportFrequency.setValue("Weekly");
        
        content.getChildren().addAll(
            new Label("General Settings:"),
            autoRefresh, emailReports, pushNotifications,
            new Separator(),
            new Label("Report Frequency:"),
            reportFrequency
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
    
    public Stage getStage() {
        return stage;
    }
}
