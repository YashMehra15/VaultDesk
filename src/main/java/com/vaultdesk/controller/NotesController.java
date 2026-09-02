package com.vaultdesk.controller;

import com.vaultdesk.dao.NoteDAO;
import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.TagDAO;
import com.vaultdesk.dao.VaultItemDAO;
import com.vaultdesk.model.SecureNote;
import com.vaultdesk.model.User;
import com.vaultdesk.model.VaultItem;
import com.vaultdesk.service.EncryptionService;
import com.vaultdesk.service.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NotesController {

    @FXML private TextField searchField;
    @FXML private ListView<String> notesListView;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextField tagsField;
    @FXML private TextArea contentArea;
    @FXML private Label statusLabel;

    private final VaultItemDAO vaultItemDAO = new VaultItemDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final TagDAO tagDAO = new TagDAO();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();

    private final Map<String, VaultItem> itemsByLabel = new HashMap<>();
    private final Map<Integer, SecureNote> notesByItemId = new HashMap<>();
    private List<VaultItem> allItems;

    private VaultItem selectedItem;

    @FXML
    private void initialize() {
        categoryBox.setItems(FXCollections.observableArrayList(
                "Personal", "College", "Work", "Technical", "Financial", "Other"));

        notesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadNoteIntoForm(newVal);
        });

        loadNotes();
    }

    private void loadNotes() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        itemsByLabel.clear();
        notesByItemId.clear();

        allItems = vaultItemDAO.findByUserAndType(user.getUserId(), VaultItem.ItemType.NOTE);
        List<SecureNote> notes = noteDAO.findAllByUser(user.getUserId());
        for (SecureNote n : notes) {
            notesByItemId.put(n.getItemId(), n);
        }

        populateList(allItems);
    }

    private void populateList(List<VaultItem> items) {
        itemsByLabel.clear();
        ObservableList<String> labels = FXCollections.observableArrayList();
        for (VaultItem item : items) {
            String label = item.getTitle() + "  [" + item.getCategory() + "]";
            itemsByLabel.put(label, item);
            labels.add(label);
        }
        notesListView.setItems(labels);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            populateList(allItems);
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();

        if (query.toLowerCase().startsWith("tag:")) {
            String tag = query.substring(4).trim();
            List<Integer> ids = tagDAO.findItemIdsByTag(user.getUserId(), tag);
            populateList(allItems.stream().filter(i -> ids.contains(i.getItemId())).toList());
        } else if (query.toLowerCase().startsWith("category:")) {
            String cat = query.substring(9).trim().toLowerCase();
            populateList(allItems.stream()
                    .filter(i -> i.getCategory() != null && i.getCategory().toLowerCase().equals(cat))
                    .toList());
        } else {
            String q = query.toLowerCase();
            populateList(allItems.stream()
                    .filter(i -> i.getTitle() != null && i.getTitle().toLowerCase().contains(q))
                    .toList());
        }
    }

    private void loadNoteIntoForm(String label) {
        VaultItem item = itemsByLabel.get(label);
        if (item == null) return;
        selectedItem = item;

        titleField.setText(item.getTitle());
        categoryBox.setValue(item.getCategory());
        tagsField.setText(String.join(", ", tagDAO.getTagsForItem(item.getItemId())));

        SecureNote note = notesByItemId.get(item.getItemId());
        if (note != null) {
            try {
                EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
                contentArea.setText(encryption.decrypt(note.getEncryptedContent()));
            } catch (Exception e) {
                contentArea.setText("(Could not decrypt this note)");
            }
        }
    }

    @FXML
    private void handleNew() {
        selectedItem = null;
        titleField.clear();
        categoryBox.setValue(null);
        tagsField.clear();
        contentArea.clear();
        notesListView.getSelectionModel().clearSelection();
        statusLabel.setText("New note - fill in the fields and save.");
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String category = categoryBox.getValue() == null ? "Other" : categoryBox.getValue();
        String content = contentArea.getText();
        List<String> tags = Arrays.asList((tagsField.getText() == null ? "" : tagsField.getText()).split(","));

        if (title.isEmpty()) {
            statusLabel.setText("Title is required.");
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
        String encryptedContent = encryption.encrypt(content);

        int itemId;
        if (selectedItem != null) {
            SecureNote existing = notesByItemId.get(selectedItem.getItemId());
            if (existing != null) {
                noteDAO.update(existing.getNoteId(), encryptedContent);
                vaultItemDAO.touchUpdatedAt(selectedItem.getItemId());
                itemId = selectedItem.getItemId();
                securityLogDAO.record(user.getUserId(), "Note updated: " + title);
                statusLabel.setText("Updated note: " + title);
            } else {
                itemId = selectedItem.getItemId();
            }
        } else {
            VaultItem item = vaultItemDAO.create(user.getUserId(), title, category, VaultItem.ItemType.NOTE);
            noteDAO.create(item.getItemId(), encryptedContent);
            itemId = item.getItemId();
            securityLogDAO.record(user.getUserId(), "Note added: " + title);
            statusLabel.setText("Saved note: " + title);
        }
        tagDAO.setTagsForItem(user.getUserId(), itemId, tags);

        loadNotes();
        handleNew();
    }

    @FXML
    private void handleDelete() {
        String selectedLabel = notesListView.getSelectionModel().getSelectedItem();
        if (selectedLabel == null) {
            statusLabel.setText("Select a note first.");
            return;
        }
        VaultItem item = itemsByLabel.get(selectedLabel);
        if (item == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Move note \"" + item.getTitle() + "\" to Trash?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
            vaultItemDAO.delete(item.getItemId());
            securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                    "Note moved to trash: " + item.getTitle());
            loadNotes();
            handleNew();
        }
    }
}
