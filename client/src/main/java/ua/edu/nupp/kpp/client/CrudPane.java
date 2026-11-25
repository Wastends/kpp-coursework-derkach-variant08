package ua.edu.nupp.kpp.client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

final class CrudPane<T> extends BorderPane {

    @FunctionalInterface
    interface Work<R> {
        R run() throws Exception;
    }

    private final HttpWaterQualityClient.Resource<T> api;
    private final TableView<T> table;
    private final EntryForm<T> form;
    private final ToLongFunction<T> id;
    private final ExecutorService executor;
    private final Label status = new Label();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final VBox editor;

    CrudPane(HttpWaterQualityClient.Resource<T> api, TableView<T> table, EntryForm<T> form,
             ToLongFunction<T> id, ExecutorService executor) {
        this.api = api;
        this.table = table;
        this.form = form;
        this.id = id;
        this.executor = executor;

        Button refresh = new Button("Оновити");
        refresh.setOnAction(event -> refresh(selectionId(), "", true));
        Button clear = new Button("Новий запис");
        clear.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            form.show(null);
        });
        Button add = new Button("Додати");
        add.getStyleClass().add("accent-button");
        add.setOnAction(event -> edit(true));
        Button update = new Button("Змінити");
        update.setOnAction(event -> edit(false));
        Button delete = new Button("Видалити");
        delete.setOnAction(event -> remove());

        for (Button button : List.of(refresh, clear, add)) {
            button.disableProperty().bind(busy);
        }
        disableWithoutSelection(update);
        disableWithoutSelection(delete);
        table.disableProperty().bind(busy);
        form.disableProperty().bind(busy);
        table.getSelectionModel().selectedItemProperty().addListener((value, old, selected) -> form.show(selected));

        table.setPlaceholder(new Label("Записів немає"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        setCenter(table);
        BorderPane.setMargin(table, new Insets(12, 0, 10, 14));
        setTop(new FlowPane(8, 8, refresh, clear));
        BorderPane.setMargin(getTop(), new Insets(13, 14, 0, 14));

        Label title = new Label("Дані запису");
        title.getStyleClass().add("editor-title");
        editor = new VBox(14, title, form, new FlowPane(8, 7, add, update, delete));
        editor.getStyleClass().add("editor");
        ScrollPane editorScroll = new ScrollPane(editor);
        editorScroll.setFitToWidth(true);
        editorScroll.setPrefWidth(390);
        editorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setRight(editorScroll);
        BorderPane.setMargin(editorScroll, new Insets(12, 14, 10, 12));

        status.getStyleClass().add("tab-status");
        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        setBottom(status);
        BorderPane.setMargin(status, new Insets(0, 14, 12, 14));
    }

    void extra(Node node) {
        editor.getChildren().add(node);
    }

    void disableWithoutSelection(Node node) {
        node.disableProperty().bind(Bindings.or(busy, table.getSelectionModel().selectedItemProperty().isNull()));
    }

    void disableWhileBusy(Node node) {
        node.disableProperty().bind(busy);
    }

    void onSelection(Consumer<T> consumer) {
        table.getSelectionModel().selectedItemProperty().addListener((property, old, value) -> consumer.accept(value));
    }

    long selectionId() {
        T selected = table.getSelectionModel().getSelectedItem();
        return selected == null ? 0L : id.applyAsLong(selected);
    }

    void refresh(long selectId, String prefix, boolean dialog) {
        run(api::list, rows -> {
            table.getSelectionModel().clearSelection();
            form.show(null);
            table.getItems().setAll(rows);
            if (selectId > 0) {
                rows.stream().filter(row -> id.applyAsLong(row) == selectId).findFirst()
                        .ifPresent(table.getSelectionModel()::select);
            }
            status.setText(prefix + "Записів " + rows.size() + ".");
        }, dialog, prefix);
    }

    <R> void run(Work<R> work, Consumer<R> success, boolean dialog, String errorPrefix) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        status.setText("З’єднання із сервером…");
        executor.submit(() -> {
            try {
                R result = work.run();
                Platform.runLater(() -> {
                    busy.set(false);
                    success.accept(result);
                });
            } catch (Exception failure) {
                Platform.runLater(() -> {
                    busy.set(false);
                    showError(errorPrefix + failureText(failure), dialog);
                });
            }
        });
    }

    void showError(String message, boolean dialog) {
        status.setText(message);
        if (dialog) {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.initOwner(getScene().getWindow());
            alert.setHeaderText("Операцію не виконано");
            alert.show();
        }
    }

    void showStatus(String message) {
        status.setText(message);
    }

    private String failureText(Exception failure) {
        if (failure instanceof InterruptedException) {
            return "Операцію перервано.";
        }
        return failure.getMessage() == null ? "Не вдалося виконати запит." : failure.getMessage();
    }

    private void edit(boolean create) {
        long selected = selectionId();
        if (!create && selected == 0) {
            return;
        }
        T value;
        try {
            value = form.value(create ? 0L : selected);
        } catch (IllegalArgumentException failure) {
            showError(failure.getMessage(), true);
            return;
        }
        run(() -> {
            if (create) {
                return api.add(value);
            }
            api.update(value);
            return selected;
        }, savedId -> refresh(savedId, "Запис збережено. ", false), true, "");
    }

    private void remove() {
        long id = selectionId();
        if (id == 0) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Видалити запис ID " + id + "?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(getScene().getWindow());
        confirm.setHeaderText("Видалення запису");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        run(() -> {
            api.delete(id);
            return null;
        }, ignored -> refresh(0L, "Запис видалено. ", false), true, "");
    }
}
