package app;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Balance;
import model.Group;
import model.User;
import service.AuthService;
import service.BalanceManager;
import service.ExpenseManager;
import service.GroupService;
import strategy.DistanceSplit;
import strategy.EqualSplit;
import strategy.PercentageSplit;
import strategy.SplitStrategy;
import strategy.WeightSplit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern JavaFX presentation layer. It contains no financial calculations;
 * every action delegates to the corresponding application service.
 */
public final class FairFareApplication extends Application {
    private final AuthService auth = new AuthService();
    private final GroupService groups = new GroupService();
    private final BalanceManager balances = new BalanceManager();
    private final ExpenseManager expenses = new ExpenseManager(balances);
    private final ObservableList<Group> groupItems = FXCollections.observableArrayList();
    private Stage stage;
    private User currentUser;
    private Group selectedGroup;
    private FlowPane groupCards;
    private Label receivableLabel;
    private Label pendingLabel;
    private Label netLabel;
    private Label selectedGroupLabel;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("FairFare · Spend together, stay clear");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        showWelcome();
        stage.show();
    }

    private void showWelcome() {
        VBox story = new VBox(18);
        story.getStyleClass().add("hero-panel");
        story.setPadding(new Insets(56));
        Label mark = new Label("FF"); mark.getStyleClass().add("brand-mark");
        Label headline = new Label("Every shared moment,\nsettled beautifully."); headline.getStyleClass().add("hero-title");
        Label copy = new Label("FairFare turns shared spending into a calm, clear ritual—so your friendships stay about the memories."); copy.getStyleClass().add("hero-copy"); copy.setWrapText(true); copy.setMaxWidth(390);
        Label featureOne = new Label("✦  Effortless group spending");
        Label featureTwo = new Label("✦  Clear balances, always");
        featureOne.getStyleClass().add("hero-feature"); featureTwo.getStyleClass().add("hero-feature");
        story.getChildren().addAll(mark, headline, copy, spacer(), featureOne, featureTwo);

        VBox form = new VBox(16); form.getStyleClass().add("auth-card"); form.setMaxWidth(370);
        Label welcome = new Label("Welcome back"); welcome.getStyleClass().add("auth-title");
        Label hint = new Label("Sign in to see where your shared life stands."); hint.getStyleClass().add("muted");
        TextField username = field("Username"); PasswordField password = passwordField("Password");
        Button login = primaryButton("Log in"); login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(event -> login(username.getText(), password.getText()));
        Button register = secondaryButton("Create a FairFare account"); register.setMaxWidth(Double.MAX_VALUE); register.setOnAction(event -> showRegister());
        form.getChildren().addAll(welcome, hint, labelled("USERNAME", username), labelled("PASSWORD", password), login, register);
        form.setAlignment(Pos.CENTER_LEFT);

        StackPane right = new StackPane(form); right.getStyleClass().add("auth-side"); right.setPadding(new Insets(42));
        HBox root = new HBox(story, right); HBox.setHgrow(story, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        story.prefWidthProperty().bind(root.widthProperty().multiply(.54)); right.prefWidthProperty().bind(root.widthProperty().multiply(.46));
        setScene(root);
    }

    private void showRegister() {
        Dialog<ButtonType> dialog = baseDialog("Create your account", "A few details and you’re ready to share fairly.");
        TextField username = field("Choose a username"); TextField email = field("you@example.com"); PasswordField password = passwordField("Create a password");
        VBox body = new VBox(12, labelled("USERNAME", username), labelled("EMAIL", email), labelled("PASSWORD", password));
        dialog.getDialogPane().setContent(body); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try { auth.register(username.getText(), email.getText(), password.getText()); info("You’re all set", "Your FairFare account is ready. Sign in to continue."); }
                catch (IllegalArgumentException exception) { error("Couldn’t create account", exception.getMessage()); }
            }
            return button;
        }); dialog.showAndWait();
    }

    private void login(String username, String password) {
        auth.login(username.trim(), password).ifPresentOrElse(user -> { currentUser = user; showDashboard(); }, () -> error("We couldn’t sign you in", "Check your username and password, then try again."));
    }

    private void showDashboard() {
        BorderPane root = new BorderPane(); root.getStyleClass().add("app-shell");
        root.setLeft(sidebar()); root.setTop(topbar()); root.setCenter(mainContent());
        refreshDashboard(); setScene(root);
    }

    private VBox sidebar() {
        VBox box = new VBox(12); box.getStyleClass().add("sidebar"); box.setPadding(new Insets(26, 18, 24, 18)); box.setPrefWidth(230);
        HBox brand = new HBox(10); brand.setAlignment(Pos.CENTER_LEFT); Label icon = new Label("FF"); icon.getStyleClass().add("mini-mark"); Label name = new Label("FairFare"); name.getStyleClass().add("sidebar-brand"); brand.getChildren().addAll(icon, name);
        Label section = new Label("WORKSPACE"); section.getStyleClass().add("side-caption");
        Button overview = navButton("⌂   Overview"); overview.getStyleClass().add("nav-active"); overview.setOnAction(event -> { selectedGroup = null; refreshDashboard(); });
        Button groupsButton = navButton("◈   My groups"); groupsButton.setOnAction(event -> { if (!groupItems.isEmpty()) { selectedGroup = groupItems.get(0); refreshDashboard(); } });
        Region growth = spacer(); VBox.setVgrow(growth, Priority.ALWAYS);
        Label user = new Label(currentUser.getUsername()); user.getStyleClass().add("user-name"); Label email = new Label(currentUser.getEmail()); email.getStyleClass().add("user-email");
        Button logout = navButton("↪   Log out"); logout.setOnAction(event -> { currentUser = null; selectedGroup = null; showWelcome(); });
        box.getChildren().addAll(brand, spacer(22), section, overview, groupsButton, growth, user, email, logout); return box;
    }

    private HBox topbar() {
        HBox bar = new HBox(14); bar.getStyleClass().add("topbar"); bar.setPadding(new Insets(20, 30, 16, 30)); bar.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(3); Label crumb = new Label("YOUR SHARED WORLD"); crumb.getStyleClass().add("eyebrow"); Label greeting = new Label("Good to see you, " + currentUser.getUsername() + "."); greeting.getStyleClass().add("page-title"); text.getChildren().addAll(crumb, greeting);
        Region stretch = spacer(); HBox.setHgrow(stretch, Priority.ALWAYS);
        Button addExpense = primaryButton("＋ Add expense"); addExpense.setOnAction(event -> showExpenseDialog());
        bar.getChildren().addAll(text, stretch, addExpense); return bar;
    }

    private ScrollPane mainContent() {
        VBox content = new VBox(24); content.setPadding(new Insets(12, 30, 34, 30)); content.getStyleClass().add("content");
        HBox metrics = new HBox(16); receivableLabel = new Label(); pendingLabel = new Label(); netLabel = new Label();
        metrics.getChildren().addAll(metricCard("TO RECEIVE", receivableLabel, "mint"), metricCard("TO PAY", pendingLabel, "rose"), metricCard("NET POSITION", netLabel, "violet"));
        Label groupsTitle = new Label("Your circles"); groupsTitle.getStyleClass().add("section-title");
        Label groupsCopy = new Label("Pick a group to manage its people, spending, and balances."); groupsCopy.getStyleClass().add("muted");
        Button create = secondaryButton("＋ New group"); create.setOnAction(event -> showCreateGroup());
        Region stretch = spacer(); HBox.setHgrow(stretch, Priority.ALWAYS); HBox groupHeader = new HBox(12, new VBox(3, groupsTitle, groupsCopy), stretch, create); groupHeader.setAlignment(Pos.CENTER_LEFT);
        groupCards = new FlowPane(16, 16); groupCards.getStyleClass().add("group-cards");
        selectedGroupLabel = new Label(); selectedGroupLabel.getStyleClass().add("section-title");
        Button manageMembers = secondaryButton("Manage members"); manageMembers.setOnAction(event -> showMemberPicker());
        Button history = secondaryButton("Expense history"); history.setOnAction(event -> showHistory());
        Button balancesButton = secondaryButton("View balances"); balancesButton.setOnAction(event -> showBalances());
        HBox actions = new HBox(10, manageMembers, history, balancesButton); actions.setAlignment(Pos.CENTER_LEFT);
        VBox details = new VBox(12, selectedGroupLabel, actions); details.getStyleClass().add("selected-group");
        content.getChildren().addAll(metrics, groupHeader, groupCards, details);
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); return scroll;
    }

    private VBox metricCard(String title, Label value, String style) {
        VBox card = new VBox(10); card.getStyleClass().addAll("metric-card", style); card.setPadding(new Insets(18)); card.setPrefWidth(210); HBox.setHgrow(card, Priority.ALWAYS);
        Label heading = new Label(title); heading.getStyleClass().add("metric-label"); value.getStyleClass().add("metric-value"); card.getChildren().addAll(heading, value); return card;
    }

    private void showCreateGroup() {
        Dialog<ButtonType> dialog = baseDialog("Create a new circle", "Start with a name. You can select exactly who joins next.");
        TextField name = field("e.g. Goa Trip"); dialog.getDialogPane().setContent(new VBox(labelled("GROUP NAME", name))); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> { if (button == ButtonType.OK) try { Group group = groups.createGroup(name.getText(), currentUser); selectedGroup = group; refreshDashboard(); showMemberPicker(); } catch (IllegalArgumentException exception) { error("Couldn’t create group", exception.getMessage()); } return button; }); dialog.showAndWait();
    }

    /** Opens a per-group checkbox picker; only checked people are added. */
    private void showMemberPicker() {
        if (requireGroup() == null) return;
        List<User> candidates = auth.getUsers().stream().filter(user -> !selectedGroup.getMembers().contains(user)).toList();
        Dialog<ButtonType> dialog = baseDialog("Add people to " + selectedGroup.getName(), "Choose only the registered people you want in this group.");
        VBox choices = new VBox(8); List<CheckBox> boxes = new ArrayList<>();
        if (candidates.isEmpty()) choices.getChildren().add(new Label("Everyone registered is already in this group."));
        for (User user : candidates) { CheckBox box = new CheckBox(user.getUsername() + "  ·  " + user.getEmail()); box.getStyleClass().add("member-choice"); boxes.add(box); choices.getChildren().add(box); }
        Label current = new Label("Already in this group: " + selectedGroup.getMembers()); current.getStyleClass().add("muted");
        dialog.getDialogPane().setContent(new VBox(14, current, choices)); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> { if (button == ButtonType.OK) { boxes.stream().filter(CheckBox::isSelected).forEach(box -> { int index = boxes.indexOf(box); groups.addMember(selectedGroup, candidates.get(index)); }); refreshDashboard(); } return button; }); dialog.showAndWait();
    }

    private void showExpenseDialog() {
        if (requireGroup() == null) return;
        Dialog<ButtonType> dialog = baseDialog("Add an expense", "Every detail stays clear for your group.");
        TextField title = field("e.g. Dinner at Olive"); TextField amount = field("0.00");
        ComboBox<User> payer = new ComboBox<>(FXCollections.observableArrayList(selectedGroup.getMembers())); payer.getSelectionModel().select(currentUser); payer.getStyleClass().add("combo-box");
        ComboBox<String> method = new ComboBox<>(FXCollections.observableArrayList("Equal", "Percentage", "Weight", "Distance")); method.getSelectionModel().selectFirst();
        VBox participantBox = new VBox(7); Map<User, CheckBox> participantChoices = new LinkedHashMap<>();
        selectedGroup.getMembers().forEach(user -> { CheckBox checkbox = new CheckBox(user.getUsername()); checkbox.setSelected(true); participantChoices.put(user, checkbox); participantBox.getChildren().add(checkbox); });
        GridPane inputs = new GridPane(); inputs.setHgap(10); inputs.setVgap(8);
        Runnable updateInputs = () -> { inputs.getChildren().clear(); if (!"Equal".equals(method.getValue())) { int row = 0; for (User user : selectedGroup.getMembers()) { TextField input = field("0"); input.setId("split-" + user.getId()); inputs.add(new Label(user.getUsername() + " " + splitUnit(method.getValue())), 0, row); inputs.add(input, 1, row++); } } };
        method.setOnAction(event -> updateInputs.run()); updateInputs.run();
        VBox body = new VBox(12, labelled("EXPENSE TITLE", title), labelled("AMOUNT", amount), labelled("PAID BY", payer), labelled("SPLIT METHOD", method), new Label("PARTICIPANTS"), participantBox, inputs);
        dialog.getDialogPane().setContent(body); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) try {
                List<User> participants = participantChoices.entrySet().stream().filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).toList();
                SplitStrategy strategy = strategyFor(method.getValue()); Map<User, BigDecimal> inputsByUser = collectSplitInputs(inputs, participants, strategy);
                expenses.addExpense(selectedGroup, title.getText(), new BigDecimal(amount.getText()), payer.getValue(), participants, strategy, inputsByUser); refreshDashboard();
            } catch (NumberFormatException exception) { error("Check the numbers", "Enter valid positive numeric amounts for the expense and split inputs."); }
              catch (IllegalArgumentException exception) { error("Couldn’t add expense", exception.getMessage()); }
            return button;
        }); dialog.showAndWait();
    }

    private Map<User, BigDecimal> collectSplitInputs(GridPane inputs, List<User> participants, SplitStrategy strategy) {
        if (strategy instanceof EqualSplit) return Map.of();
        Map<User, BigDecimal> values = new LinkedHashMap<>();
        for (User participant : participants) {
            TextField input = (TextField) inputs.lookup("#split-" + participant.getId());
            values.put(participant, new BigDecimal(input.getText()));
        }
        return values;
    }

    private void showHistory() {
        if (requireGroup() == null) return;
        String text = selectedGroup.getExpenses().isEmpty() ? "No expenses in this group yet." : selectedGroup.getExpenses().stream().map(expense -> expense.getTitle() + "  ·  ₹" + money(expense.getAmount()) + "  ·  paid by " + expense.getPayer()).reduce((a, b) -> a + "\n" + b).orElse("");
        info(selectedGroup.getName() + " · Expense history", text);
    }
    private void showBalances() {
        if (requireGroup() == null) return;
        String text = selectedGroup.getBalances().isEmpty() ? "Everyone is settled up." : selectedGroup.getBalances().stream().map(this::formatBalance).reduce((a, b) -> a + "\n" + b).orElse("");
        info(selectedGroup.getName() + " · Balances", text);
    }
    private String formatBalance(Balance balance) { return balance.getDebtor() + " owes " + balance.getCreditor() + "  ₹" + money(balance.getAmount()); }

    private void refreshDashboard() {
        groupItems.setAll(groups.groupsFor(currentUser)); if (selectedGroup != null && !groupItems.contains(selectedGroup)) selectedGroup = null;
        receivableLabel.setText("₹" + money(balances.receivableFor(currentUser))); pendingLabel.setText("₹" + money(balances.pendingFor(currentUser))); BigDecimal net = balances.netFor(currentUser); netLabel.setText((net.signum() >= 0 ? "+" : "") + "₹" + money(net));
        groupCards.getChildren().clear();
        if (groupItems.isEmpty()) { Label empty = new Label("No circles yet. Create one to begin sharing fairly."); empty.getStyleClass().add("empty-state"); groupCards.getChildren().add(empty); }
        groupItems.forEach(group -> groupCards.getChildren().add(groupCard(group)));
        selectedGroupLabel.setText(selectedGroup == null ? "Select a group to see its actions" : selectedGroup.getName() + "  ·  " + selectedGroup.getMembers().size() + " members");
    }
    private Button groupCard(Group group) {
        Button card = new Button(); card.getStyleClass().add("group-card"); VBox box = new VBox(8); Label title = new Label(group.getName()); title.getStyleClass().add("group-card-title"); Label members = new Label(group.getMembers().size() + " members  ·  " + group.getExpenses().size() + " expenses"); members.getStyleClass().add("muted"); Label status = new Label(group.getBalances().isEmpty() ? "All settled" : group.getBalances().size() + " open balances"); status.getStyleClass().add("card-status"); box.getChildren().addAll(title, members, status); card.setGraphic(box); card.setOnAction(event -> { selectedGroup = group; refreshDashboard(); }); return card;
    }
    private Group requireGroup() { if (selectedGroup == null) error("Choose a group first", "Select one of your circles before taking that action."); return selectedGroup; }
    private SplitStrategy strategyFor(String type) { return switch (type) { case "Percentage" -> new PercentageSplit(); case "Weight" -> new WeightSplit(); case "Distance" -> new DistanceSplit(); default -> new EqualSplit(); }; }
    private String splitUnit(String type) { return switch (type) { case "Percentage" -> "%"; case "Distance" -> "km"; default -> "weight"; }; }
    private void setScene(javafx.scene.Parent root) { Scene scene = new Scene(root, 1180, 760); scene.getStylesheets().add(getClass().getResource("fairfare.css").toExternalForm()); stage.setScene(scene); }
    private Dialog<ButtonType> baseDialog(String title, String header) { Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(header); dialog.initOwner(stage); dialog.getDialogPane().getStyleClass().add("fair-dialog"); return dialog; }
    private TextField field(String prompt) { TextField field = new TextField(); field.setPromptText(prompt); field.getStyleClass().add("text-field"); return field; }
    private PasswordField passwordField(String prompt) { PasswordField field = new PasswordField(); field.setPromptText(prompt); field.getStyleClass().add("text-field"); return field; }
    private VBox labelled(String label, javafx.scene.Node field) { Label caption = new Label(label); caption.getStyleClass().add("field-label"); return new VBox(6, caption, field); }
    private Button primaryButton(String text) { Button button = new Button(text); button.getStyleClass().add("primary-button"); return button; }
    private Button secondaryButton(String text) { Button button = new Button(text); button.getStyleClass().add("secondary-button"); return button; }
    private Button navButton(String text) { Button button = new Button(text); button.getStyleClass().add("nav-button"); button.setMaxWidth(Double.MAX_VALUE); return button; }
    private Region spacer() { Region region = new Region(); HBox.setHgrow(region, Priority.ALWAYS); return region; }
    private Region spacer(double height) { Region region = spacer(); region.setMinHeight(height); return region; }
    private String money(BigDecimal amount) { return amount.setScale(2).toPlainString(); }
    private void info(String title, String message) { alert(Alert.AlertType.INFORMATION, title, message); }
    private void error(String title, String message) { alert(Alert.AlertType.ERROR, title, message); }
    private void alert(Alert.AlertType type, String title, String message) { Alert alert = new Alert(type, message, ButtonType.OK); alert.setTitle("FairFare"); alert.setHeaderText(title); alert.initOwner(stage); alert.showAndWait(); }
}
