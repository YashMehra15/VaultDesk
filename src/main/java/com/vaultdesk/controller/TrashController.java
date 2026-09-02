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
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * "Recently Deleted" - version-history/recovery module. Items removed from
 * Passwords, Notes, or Documents are soft-deleted (moved here) rather than
 * erased outright, and can be restored or permanently purged.
 */
public class TrashController {

    @FXML private TableView<VaultItem> trashTable;
    @FXML private TableColumn<VaultItem, String> titleColumn;
    @FXML private TableColumn<VaultItem, String> typeColumn;
    @FXML private TableColumn<VaultItem, String> categoryColumn;
    @FXML private TableColumn<VaultItem, String> deletedAtColumn;
    @FXML private Label statusLabel;

    private final VaultItemDAO vaultItemDAO = new VaultItemDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final FileVaultService fileVaultService = new FileVaultService();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    @FXML
    private void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemType().name()));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        deletedAtColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDeletedAt() == null ? "" : c.getValue().getDeletedAt().format(FORMAT)));

        loadTrash();
    }

    @FXML
    private void handleRefresh() {
        loadTrash();
    }

    private void loadTrash() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        List<VaultItem> deleted = vaultItemDAO.findDeletedByUser(user.getUserId());
        ObservableList<VaultItem> rows = FXCollections.observableArrayList(deleted);
        trashTable.setItems(rows);
    }

    @FXML
    private void handleRestore() {
        VaultItem selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an item first.");
            return;
        }
        vaultItemDAO.restore(selected.getItemId());
        securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                "Restored from trash: " + selected.getTitle());
        loadTrash();
        statusLabel.setText("Restored: " + selected.getTitle());
    }

    @FXML
    private void handlePermanentDelete() {
        VaultItem selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select an item first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete \"" + selected.getTitle() + "\"? This CANNOT be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || !result.get().getButtonData().isDefaultButton()) return;

        // For documents, also remove the stored file copy from disk before the DB row goes away.
        if (selected.getItemType() == VaultItem.ItemType.DOCUMENT) {
            documentDAO.findByItemId(selected.getItemId())
                    .map(Document::getFilePath)
                    .ifPresent(fileVaultService::deleteFile);
        }

        vaultItemDAO.permanentlyDelete(selected.getItemId());
        securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                "Permanently deleted: " + selected.getTitle());
        loadTrash();
        statusLabel.setText("Permanently deleted.");
    }
}
