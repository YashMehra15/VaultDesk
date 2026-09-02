package com.vaultdesk.controller;

import com.vaultdesk.dao.DocumentDAO;
import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.VaultItemDAO;
import com.vaultdesk.model.Document;
import com.vaultdesk.model.User;
import com.vaultdesk.model.VaultItem;
import com.vaultdesk.service.FileVaultService;
import com.vaultdesk.service.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DocumentController {

    @FXML private TableView<DocumentRow> documentTable;
    @FXML private TableColumn<DocumentRow, String> fileNameColumn;
    @FXML private TableColumn<DocumentRow, String> categoryColumn;
    @FXML private TableColumn<DocumentRow, String> addedOnColumn;

    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private Label chosenFileLabel;
    @FXML private Label statusLabel;

    private final VaultItemDAO vaultItemDAO = new VaultItemDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final FileVaultService fileVaultService = new FileVaultService();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();

    private final ObservableList<DocumentRow> rows = FXCollections.observableArrayList();
    private File selectedFile;

    public static class DocumentRow {
        final VaultItem item;
        final Document document;

        DocumentRow(VaultItem item, Document document) {
            this.item = item;
            this.document = document;
        }

        public String getFileName() { return document.getFileName(); }
        public String getCategory() { return item.getCategory(); }
        public String getAddedOn() {
            return item.getCreatedAt() == null ? "" :
                    item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        }
    }

    @FXML
    private void initialize() {
        categoryBox.setItems(FXCollections.observableArrayList(
                "Career", "Education", "Personal", "Financial", "College", "Other"));

        fileNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFileName()));
        categoryColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        addedOnColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddedOn()));

        loadDocuments();
    }

    private void loadDocuments() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<Document> documents = documentDAO.findAllByUser(user.getUserId());
        java.util.Map<Integer, VaultItem> items = vaultItemDAO.findAllByUserAsMap(user.getUserId());

        rows.clear();
        for (Document d : documents) {
            VaultItem item = items.get(d.getItemId());
            if (item != null) rows.add(new DocumentRow(item, d));
        }
        documentTable.setItems(rows);
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a PDF or Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF and Images", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(documentTable.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            chosenFileLabel.setText("Selected: " + file.getName());
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                titleField.setText(file.getName());
            }
        }
    }

    @FXML
    private void handleSave() {
        if (selectedFile == null) {
            statusLabel.setText("Choose a PDF or image file first.");
            return;
        }
        String title = titleField.getText() == null || titleField.getText().isBlank()
                ? selectedFile.getName() : titleField.getText().trim();
        String category = categoryBox.getValue() == null ? "Other" : categoryBox.getValue();

        User user = SessionManager.getInstance().getCurrentUser();

        try {
            String storedPath = fileVaultService.storeFile(user.getUserId(), selectedFile);
            String hash = fileVaultService.computeSha256(selectedFile);

            VaultItem item = vaultItemDAO.create(user.getUserId(), title, category, VaultItem.ItemType.DOCUMENT);
            documentDAO.create(item.getItemId(), selectedFile.getName(), storedPath, hash);
            securityLogDAO.record(user.getUserId(), "Document added: " + selectedFile.getName());

            statusLabel.setText("Saved: " + selectedFile.getName());
            handleClear();
            loadDocuments();
        } catch (Exception e) {
            statusLabel.setText("Failed to save document: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpen() {
        DocumentRow selected = documentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a document first.");
            return;
        }
        try {
            File file = new File(selected.document.getFilePath());
            if (!file.exists()) {
                statusLabel.setText("File is missing from disk: " + file.getPath());
                return;
            }
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            statusLabel.setText("Could not open file: " + e.getMessage());
        }
    }

    @FXML
    private void handleVerify() {
        DocumentRow selected = documentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a document first.");
            return;
        }
        try {
            File file = new File(selected.document.getFilePath());
            if (!file.exists()) {
                statusLabel.setText("File is missing from disk - cannot verify.");
                return;
            }
            String currentHash = fileVaultService.computeSha256(file);
            boolean matches = currentHash.equalsIgnoreCase(selected.document.getFileHash());

            Alert alert = new Alert(matches ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
            alert.setTitle("Integrity Check");
            alert.setHeaderText(selected.getFileName());
            alert.setContentText(matches
                    ? "✓ File matches its stored SHA-256 hash. No tampering detected."
                    : "⚠ File hash does NOT match the original. The file may have been modified.");
            alert.showAndWait();
        } catch (Exception e) {
            statusLabel.setText("Verification failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        DocumentRow selected = documentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a document first.");
            return;
        }
        // Soft delete only - the stored file on disk is kept until the item is
        // permanently removed from Trash, so it can still be restored/opened.
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Move \"" + selected.getFileName() + "\" to Trash?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
            vaultItemDAO.delete(selected.item.getItemId());
            securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                    "Document moved to trash: " + selected.getFileName());
            loadDocuments();
            statusLabel.setText("Moved to Trash.");
        }
    }

    @FXML
    private void handleClear() {
        selectedFile = null;
        titleField.clear();
        categoryBox.setValue(null);
        chosenFileLabel.setText("No file selected");
    }
}
