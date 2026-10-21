import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.Locale;

public class CrudApp extends Application {
    // SHORTCUT = Ctrl no Windows/Linux, Cmd no Mac
    private static final KeyCombination FIND = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination NEW = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);

    private static final double FORM_WIDTH = 320;
    private static final String[] AVATAR_COLORS = {
            "#1f3f8a", "#a62a22", "#2f6b35", "#7a4b1e", "#5b3a8c", "#0f6266", "#8a5a00"
    };
    private static final String SEARCH_ICON =
            "M6.5 1a5.5 5.5 0 0 1 4.38 8.82l3.65 3.65-1.06 1.06-3.65-3.65A5.5 5.5 0 1 1 6.5 1zm0 1.5a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
    private static final String PERSON_ICON =
            "M16 4a6 6 0 1 1 0 12 6 6 0 0 1 0-12zm0 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"
                    + "M16 18c6.1 0 11 3.4 11 8v2H5v-2c0-4.6 4.9-8 11-8zm0 2c-4.6 0-8.4 2.3-8.9 5h17.8c-.5-2.7-4.3-5-8.9-5z";

    private final PersonStore store = PersonStore.inUserHome();
    private Crud crud;
    private IOException loadError;

    // mesma ordem do Crud: índice em people = índice no Crud
    private final ObservableList<Person> people = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredPeople = new FilteredList<>(people, person -> true);
    private final TableView<Person> table = new TableView<>();

    private final TextField searchField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField ageField = new TextField();

    private final Label countLabel = new Label();
    private final Label averageLabel = new Label();
    private final Label placeholderTitle = new Label();
    private final Label placeholderHint = new Label();
    private final Label formTitle = new Label("Nova pessoa");
    private final Label formHint = new Label();
    private final Label editBadge = new Label("Editando");
    private final Label feedbackLabel = new Label();

    private Stage primaryStage;
    private String stylesheetUrl;
    private VBox content;
    private VBox listCard;
    private VBox formCard;
    private Button saveButton;
    private Button deleteButton;
    private Button cancelButton;
    private Animation feedbackAnimation;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Cadastro de pessoas");
        stage.setMinWidth(860);
        stage.setMinHeight(600);

        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet == null) {
            throw new IllegalStateException("Não foi possível encontrar styles.css.");
        }
        stylesheetUrl = stylesheet.toExternalForm();

        crud = openCrud();

        Scene scene = new Scene(createLayout(), 1060, 700);
        scene.getStylesheets().add(stylesheetUrl);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcuts);

        stage.setScene(scene);
        stage.show();
        refreshTable();
        Platform.runLater(nameField::requestFocus);
        animateEntrance(content);
        if (loadError != null) {
            Platform.runLater(this::showLoadError);
        }
    }

    // CSV corrompido: abre só em memória pra não sobrescrever o arquivo
    private Crud openCrud() {
        try {
            return new Crud(store);
        } catch (IOException e) {
            loadError = e;
            return new Crud();
        }
    }

    private StackPane createLayout() {
        configureTable();
        buildListCard();
        buildFormCard();

        Label title = new Label("Gestão de pessoas");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(crud.isPersistent()
                ? "Cadastre, edite e organize seus registros. Tudo é salvo automaticamente."
                : "Modo temporário: as alterações não serão salvas ao fechar.");
        subtitle.getStyleClass().add("page-subtitle");
        if (crud.isPersistent()) {
            subtitle.setTooltip(new Tooltip("Arquivo: " + store.getFile()));
        } else {
            subtitle.getStyleClass().add("warning-text");
        }
        VBox heading = new VBox(6, title, subtitle);

        HBox body = new HBox(20, listCard, formCard);
        HBox.setHgrow(listCard, Priority.ALWAYS);

        content = new VBox(22, heading, body);
        content.getStyleClass().add("content");
        VBox.setVgrow(body, Priority.ALWAYS);

        StackPane layout = new StackPane(content);
        layout.getStyleClass().add("app-background");

        setEditing(false);
        updateSummary();
        return layout;
    }

    private void configureTable() {
        TableColumn<Person, Void> indexColumn = new TableColumn<>("#");
        // aplica no header e nas células (margem vermelha do CSS)
        indexColumn.getStyleClass().add("index-column");
        indexColumn.setSortable(false);
        indexColumn.setResizable(false);
        indexColumn.setReorderable(false);
        indexColumn.setMinWidth(56);
        indexColumn.setMaxWidth(56);
        indexColumn.setCellFactory(column -> {
            TableCell<Person, Void> cell = new TableCell<>();
            cell.getStyleClass().add("index-cell");
            cell.textProperty().bind(Bindings.when(cell.emptyProperty())
                    .then("")
                    .otherwise(cell.indexProperty().add(1).asString()));
            return cell;
        });

        TableColumn<Person, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setReorderable(false);
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getNome()));
        nameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                setText(null);
                setGraphic(empty || name == null ? null : nameWithAvatar(name));
            }
        });

        TableColumn<Person, Number> ageColumn = new TableColumn<>("Idade");
        ageColumn.setReorderable(false);
        ageColumn.setMinWidth(110);
        ageColumn.setMaxWidth(110);
        ageColumn.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getIdade()));
        ageColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number age, boolean empty) {
                super.updateItem(age, empty);
                setText(empty || age == null ? null : formatAge(age.intValue()));
            }
        });

        table.getColumns().addAll(indexColumn, nameColumn, ageColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add("people-table");
        table.setMinHeight(160);

        SortedList<Person> sortedPeople = new SortedList<>(filteredPeople);
        sortedPeople.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedPeople);

        SVGPath placeholderIcon = new SVGPath();
        placeholderIcon.setContent(PERSON_ICON);
        placeholderIcon.getStyleClass().add("placeholder-icon");
        placeholderTitle.getStyleClass().add("placeholder-title");
        placeholderHint.getStyleClass().add("placeholder-text");
        VBox placeholder = new VBox(8, placeholderIcon, placeholderTitle, placeholderHint);
        placeholder.setAlignment(Pos.CENTER);
        table.setPlaceholder(placeholder);

        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previousPerson, selectedPerson) -> {
                    if (selectedPerson == null) {
                        // a busca escondeu quem tava em edição; sem limpar, o Enter criaria uma cópia
                        nameField.clear();
                        ageField.clear();
                        clearValidation();
                        setEditing(false);
                        return;
                    }
                    nameField.setText(selectedPerson.getNome());
                    ageField.setText(String.valueOf(selectedPerson.getIdade()));
                    clearValidation();
                    setEditing(true);
                });

        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deletePerson();
            }
        });
    }

    private void buildListCard() {
        Label listTitle = new Label("Pessoas cadastradas");
        listTitle.getStyleClass().add("section-title");
        Label listSubtitle = new Label("Clique em um registro para editar ou remover.");
        listSubtitle.getStyleClass().add("section-subtitle");

        countLabel.getStyleClass().add("count-badge");
        averageLabel.getStyleClass().add("stat-badge");
        // janela estreita: só o subtítulo encolhe
        listTitle.setMinWidth(Region.USE_PREF_SIZE);
        countLabel.setMinWidth(Region.USE_PREF_SIZE);
        averageLabel.setMinWidth(Region.USE_PREF_SIZE);
        listSubtitle.setMinWidth(0);

        HBox listHeader = new HBox(10, new VBox(4, listTitle, listSubtitle), spacer(), averageLabel, countLabel);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Buscar por nome  (Ctrl+F)");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, oldText, newText) -> applyFilter(newText));

        // ícone por cima do campo, o padding do CSS abre o espaço
        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent(SEARCH_ICON);
        searchIcon.getStyleClass().add("search-icon");
        searchIcon.setMouseTransparent(true);
        StackPane.setMargin(searchIcon, new Insets(0, 0, 0, 13));
        StackPane search = new StackPane(searchField, searchIcon);
        search.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        Button exportButton = createButton("Exportar CSV", "secondary-button", this::exportCsv);
        exportButton.setTooltip(new Tooltip("Salva todos os registros num arquivo que abre no Excel"));
        exportButton.disableProperty().bind(Bindings.isEmpty(people));

        HBox toolbar = new HBox(10, search, exportButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        listCard = new VBox(14, listHeader, toolbar, table);
        listCard.getStyleClass().add("sheet");
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void buildFormCard() {
        nameField.setPromptText("Ex.: Maria Silva");
        ageField.setPromptText("Ex.: 25");

        nameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= Person.MAX_NAME_LENGTH ? change : null));
        ageField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,3}") ? change : null));

        nameField.textProperty().addListener((observable, oldText, newText) -> markValid(nameField));
        ageField.textProperty().addListener((observable, oldText, newText) -> markValid(ageField));

        nameField.setOnAction(event -> submit());
        ageField.setOnAction(event -> submit());

        saveButton = createButton("Adicionar", "primary-button", this::submit);
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setTooltip(new Tooltip("Enter"));
        deleteButton = createButton("Remover", "danger-button", this::deletePerson);
        deleteButton.setTooltip(new Tooltip("Delete (com o registro selecionado na tabela)"));
        cancelButton = createButton("Limpar", "ghost-button", this::clearForm);
        cancelButton.setTooltip(new Tooltip("Esc  ·  Ctrl+N para uma nova pessoa"));

        HBox secondaryActions = new HBox(10, deleteButton, spacer(), cancelButton);
        secondaryActions.setAlignment(Pos.CENTER_LEFT);

        formTitle.getStyleClass().add("section-title");
        editBadge.getStyleClass().add("edit-badge");
        HBox formHeader = new HBox(10, formTitle, editBadge);
        formHeader.setAlignment(Pos.CENTER_LEFT);
        formHint.getStyleClass().add("section-subtitle");
        formHint.setWrapText(true);

        // altura fixa pro card não pular quando a msg aparece/some
        feedbackLabel.getStyleClass().add("feedback");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMinHeight(36);
        feedbackLabel.setAlignment(Pos.TOP_LEFT);
        feedbackLabel.setOpacity(0);

        VBox fields = new VBox(14, fieldGroup("Nome", nameField), fieldGroup("Idade", ageField));
        VBox actions = new VBox(10, saveButton, secondaryActions);

        formCard = new VBox(18, new VBox(4, formHeader, formHint), fields, actions, feedbackLabel);
        formCard.getStyleClass().addAll("sheet", "form-card");
        formCard.setMinWidth(FORM_WIDTH);
        formCard.setPrefWidth(FORM_WIDTH);
        formCard.setMaxWidth(FORM_WIDTH);
    }

    // a cor vem do nome, então cada pessoa mantém sempre a mesma
    private static HBox nameWithAvatar(String name) {
        // mistura os bits do hash, só o hashCode repetia muito a cor
        int hash = name.hashCode() * 0x9E3779B1;
        String color = AVATAR_COLORS[Math.floorMod(hash ^ (hash >>> 16), AVATAR_COLORS.length)];

        Label initials = new Label(initials(name));
        initials.getStyleClass().add("avatar-text");
        initials.setStyle("-fx-text-fill: " + color + ";");

        Circle circle = new Circle(15, Color.web(color, 0.08));
        circle.setStroke(Color.web(color, 0.75));
        circle.setStrokeWidth(1.4);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("name-text");

        HBox box = new HBox(11, new StackPane(circle, initials), nameLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // "Maria Clara da Silva" -> "MS", "Zé" -> "Z"
    private static String initials(String name) {
        String[] words = name.split(" ");
        String first = words[0].substring(0, 1);
        String last = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private VBox fieldGroup(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        return new VBox(6, label, field);
    }

    private Button createButton(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("action-button", styleClass);
        button.setOnAction(event -> action.run());
        installHoverEffect(button);
        return button;
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void submit() {
        if (isEditing()) {
            updatePerson();
        } else {
            addPerson();
        }
    }

    private void addPerson() {
        Person person = readPersonFromForm();
        if (person == null) {
            return;
        }

        commit(() -> crud.create(person), "Pessoa adicionada.");
        clearForm();
        animateCard();
        nameField.requestFocus();
    }

    private void updatePerson() {
        Person selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // com busca/ordenação o índice da tabela != índice do Crud, então pega de people
        int index = people.indexOf(selected);
        if (index < 0) {
            return;
        }

        Person person = readPersonFromForm();
        if (person == null) {
            return;
        }

        commit(() -> crud.update(index, person), "Pessoa atualizada.");
        table.getSelectionModel().select(people.get(index));
        if (table.getSelectionModel().getSelectedItem() == null) {
            // saiu do filtro da busca
            clearForm();
        }
        animateCard();
    }

    private void deletePerson() {
        Person selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        int index = people.indexOf(selected);
        if (index < 0 || !confirmDelete(selected)) {
            return;
        }

        commit(() -> crud.delete(index), "Pessoa removida.");
        clearForm();
        animateCard();
    }

    private void commit(Runnable change, String successMessage) {
        try {
            change.run();
            showFeedback(successMessage, false);
        } catch (UncheckedIOException e) {
            showFeedback("Alterado na tela, mas não foi possível salvar no arquivo.", true);
        }
        refreshTable();
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar pessoas");
        chooser.setInitialFileName("pessoas.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha CSV", "*.csv"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }
        try {
            PersonStore.write(file.toPath(), crud.read());
            showFeedback(plural(crud.size()) + " exportados para " + file.getName() + ".", false);
        } catch (IOException e) {
            showFeedback("Não foi possível exportar: " + e.getMessage(), true);
        }
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        nameField.clear();
        ageField.clear();
        clearValidation();
        setEditing(false);
    }

    private void refreshTable() {
        people.setAll(crud.read());
        updateSummary();
    }

    private void handleShortcuts(KeyEvent event) {
        if (FIND.match(event)) {
            searchField.requestFocus();
            searchField.selectAll();
        } else if (NEW.match(event)) {
            clearForm();
            nameField.requestFocus();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            if (searchField.isFocused() && !searchField.getText().isEmpty()) {
                searchField.clear();
            } else {
                clearForm();
            }
        } else {
            return;
        }
        event.consume();
    }

    private boolean isEditing() {
        return table.getSelectionModel().getSelectedItem() != null;
    }

    private void setEditing(boolean editing) {
        deleteButton.setVisible(editing);
        deleteButton.setManaged(editing);
        saveButton.setText(editing ? "Salvar alterações" : "Adicionar");
        cancelButton.setText(editing ? "Cancelar" : "Limpar");
        editBadge.setVisible(editing);
        editBadge.setManaged(editing);
        formTitle.setText(editing ? "Editar pessoa" : "Nova pessoa");
        formHint.setText(editing
                ? "Altere os dados e pressione Enter para salvar."
                : "Preencha os dados e pressione Enter.");
        formCard.getStyleClass().remove("editing");
        if (editing) {
            formCard.getStyleClass().add("editing");
        }
    }

    private void applyFilter(String query) {
        String normalizedQuery = normalize(query);
        filteredPeople.setPredicate(person ->
                normalizedQuery.isEmpty() || normalize(person.getNome()).contains(normalizedQuery));
        updateSummary();
    }

    private void updateSummary() {
        int total = people.size();
        int shown = filteredPeople.size();
        countLabel.setText(shown == total ? plural(total) : shown + " de " + total);

        // média só de quem aparece no filtro
        double average = filteredPeople.stream().mapToInt(Person::getIdade).average().orElse(-1);
        averageLabel.setVisible(average >= 0);
        averageLabel.setManaged(average >= 0);
        averageLabel.setText("Média " + formatAge((int) Math.round(average)));

        if (total == 0) {
            placeholderTitle.setText("Nenhuma pessoa cadastrada ainda");
            placeholderHint.setText("Use o formulário ao lado para adicionar a primeira.");
        } else {
            placeholderTitle.setText("Ninguém encontrado para \"" + searchField.getText().trim() + "\"");
            placeholderHint.setText("Confira a grafia ou pressione Esc para limpar a busca.");
        }
    }

    private static String plural(int total) {
        return total == 1 ? "1 registro" : total + " registros";
    }

    private static String formatAge(int age) {
        return age == 1 ? "1 ano" : age + " anos";
    }

    // ignora acento e maiúscula: "jose" acha "José"
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private Person readPersonFromForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            reject(nameField, "Informe o nome da pessoa.");
            return null;
        }

        String ageText = ageField.getText().trim();
        if (ageText.isEmpty()) {
            reject(ageField, "Informe a idade.");
            return null;
        }

        // o TextFormatter só deixa dígito, o parse não estoura
        int age = Integer.parseInt(ageText);
        if (age > Person.MAX_AGE) {
            reject(ageField, "A idade máxima é " + Person.MAX_AGE + " anos.");
            return null;
        }

        return new Person(name, age);
    }

    private void showLoadError() {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Não foi possível ler " + store.getFile() + ".\n\n" + loadError.getMessage()
                        + "\n\nO app vai abrir em modo temporário e não vai mexer nesse arquivo. "
                        + "Corrija ou apague o arquivo e abra o app de novo.",
                ButtonType.OK);
        alert.initOwner(primaryStage);
        alert.setTitle("Arquivo de dados com problema");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(stylesheetUrl);
        pane.getStyleClass().add("app-dialog");
        pane.lookupButton(ButtonType.OK).getStyleClass().addAll("action-button", "secondary-button");
        alert.showAndWait();
    }

    private void reject(TextField field, String message) {
        if (!field.getStyleClass().contains("invalid")) {
            field.getStyleClass().add("invalid");
        }
        field.requestFocus();
        shake(field);
        showFeedback(message, true);
    }

    private void markValid(TextField field) {
        field.getStyleClass().remove("invalid");
    }

    private void clearValidation() {
        markValid(nameField);
        markValid(ageField);
    }

    private void showFeedback(String message, boolean error) {
        if (feedbackAnimation != null) {
            feedbackAnimation.stop();
        }

        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-success");
        feedbackLabel.getStyleClass().add(error ? "feedback-error" : "feedback-success");
        feedbackLabel.setOpacity(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(error ? 4 : 2.6));
        FadeTransition fade = new FadeTransition(Duration.millis(400), feedbackLabel);
        fade.setToValue(0);

        feedbackAnimation = new SequentialTransition(hold, fade);
        feedbackAnimation.play();
    }

    private boolean confirmDelete(Person person) {
        ButtonType remove = new ButtonType("Remover", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remover \"" + person.getNome() + "\" da lista? Essa ação não pode ser desfeita.",
                remove, cancel);
        alert.initOwner(primaryStage);
        alert.setTitle("Remover pessoa");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(stylesheetUrl);
        pane.getStyleClass().add("app-dialog");
        pane.lookupButton(remove).getStyleClass().addAll("action-button", "danger-button");
        pane.lookupButton(cancel).getStyleClass().addAll("action-button", "secondary-button");

        return alert.showAndWait().filter(result -> result == remove).isPresent();
    }

    private void animateEntrance(Node node) {
        node.setOpacity(0);
        node.setTranslateY(14);

        FadeTransition fade = new FadeTransition(Duration.millis(480), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(480), node);
        slide.setFromY(14);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void installHoverEffect(Button button) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), button);
        button.setOnMouseEntered(event -> scaleTo(transition, button.isDisabled() ? 1 : 1.03));
        button.setOnMouseExited(event -> scaleTo(transition, 1));
    }

    private void scaleTo(ScaleTransition transition, double scale) {
        transition.stop();
        transition.setToX(scale);
        transition.setToY(scale);
        transition.playFromStart();
    }

    private void animateCard() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(180), listCard);
        pulse.setFromX(0.992);
        pulse.setFromY(0.992);
        pulse.setToX(1);
        pulse.setToY(1);
        pulse.play();
    }

    private void shake(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), node);
        shake.setFromX(0);
        shake.setToX(6);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> node.setTranslateX(0));
        shake.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}