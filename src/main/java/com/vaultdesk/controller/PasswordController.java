package com.vaultdesk.controller;

import com.vaultdesk.dao.CredentialDAO;
import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.dao.TagDAO;
import com.vaultdesk.dao.VaultItemDAO;
import com.vaultdesk.model.Credential;
import com.vaultdesk.model.User;
import com.vaultdesk.model.VaultItem;
import com.vaultdesk.service.EncryptionService;
import com.vaultdesk.service.PasswordGenerator;
import com.vaultdesk.service.SessionManager;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PasswordController {

    @FXML private TextField searchField;
    @FXML private TableView<Credential> credentialTable;
    @FXML private TableColumn<Credential, String> websiteColumn;
    @FXML private TableColumn<Credential, String> usernameColumn;
    @FXML private TableColumn<Credential, String> passwordColumn;
    @FXML private TableColumn<Credential, String> strengthColumn;
    @FXML private TableColumn<Credential, String> tagsColumn;

    @FXML private TextField websiteField;
    @FXML private TextField entryUsernameField;
    @FXML private TextField entryPasswordField;
    @FXML private TextField tagsField;
    @FXML private Slider lengthSlider;
    @FXML private CheckBox upperCheck;
    @FXML private CheckBox lowerCheck;
    @FXML private CheckBox digitsCheck;
    @FXML private CheckBox symbolsCheck;
    @FXML private Label formStatusLabel;

    private final VaultItemDAO vaultItemDAO = new VaultItemDAO();
    private final CredentialDAO credentialDAO = new CredentialDAO();
    private final TagDAO tagDAO = new TagDAO();
    private final PasswordGenerator generator = new PasswordGenerator();
    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();

    private final ObservableList<Credential> masterList = FXCollections.observableArrayList();
    private Credential selectedForEdit;
    private PauseTransition clipboardClearTimer;

    @FXML
    private void initialize() {
        websiteColumn.setCellValueFactory(new PropertyValueFactory<>("website"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        passwordColumn.setCellValueFactory(c -> new SimpleStringProperty("••••••••"));

        strengthColumn.setCellValueFactory(c -> {
            try {
                EncryptionService enc = SessionManager.getInstance().getEncryptionService();
                String plain = enc.decrypt(c.getValue().getEncryptedPassword());
                PasswordGenerator.Breakdown b = generator.analyze(plain);
                return new SimpleStringProperty(b.label + " (" + b.score + "%)");
            } catch (Exception e) {
                return new SimpleStringProperty("--");
            }
        });

        tagsColumn.setCellValueFactory(c -> {
            List<String> tags = tagDAO.getTagsForItem(c.getValue().getItemId());
            return new SimpleStringProperty(tags.isEmpty() ? "" : "#" + String.join(" #", tags));
        });

        loadCredentials();
    }

    private void loadCredentials() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        List<Credential> list = credentialDAO.findAllByUser(user.getUserId());
        masterList.setAll(list);
        credentialTable.setItems(masterList);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            credentialTable.setItems(masterList);
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        ObservableList<Credential> filtered = FXCollections.observableArrayList();

        if (query.toLowerCase().startsWith("tag:")) {
            String tag = query.substring(4).trim();
            List<Integer> itemIds = tagDAO.findItemIdsByTag(user.getUserId(), tag);
            for (Credential c : masterList) {
                if (itemIds.contains(c.getItemId())) filtered.add(c);
            }
        } else {
            String q = query.toLowerCase();
            for (Credential c : masterList) {
                String site = c.getWebsite() == null ? "" : c.getWebsite().toLowerCase();
                String uname = c.getUsername() == null ? "" : c.getUsername().toLowerCase();
                if (site.contains(q) || uname.contains(q)) filtered.add(c);
            }
        }
        credentialTable.setItems(filtered);
    }

    @FXML
    private void handleGenerate() {
        String generated = generator.generate(
                (int) lengthSlider.getValue(),
                upperCheck.isSelected(),
                lowerCheck.isSelected(),
                digitsCheck.isSelected(),
                symbolsCheck.isSelected()
        );
        entryPasswordField.setText(generated);
    }

    @FXML
    private void handleCopy() {
        Credential selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Select an entry first.");
            return;
        }
        EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
        try {
            String plain = encryption.decrypt(selected.getEncryptedPassword());

            ClipboardContent content = new ClipboardContent();
            content.putString(plain);
            Clipboard.getSystemClipboard().setContent(content);

            securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                    "Password copied: " + selected.getWebsite());

            if (clipboardClearTimer != null) clipboardClearTimer.stop();
            formStatusLabel.setText("✓ Copied. Clipboard clears in 30 seconds.");
            clipboardClearTimer = new PauseTransition(Duration.seconds(30));
            clipboardClearTimer.setOnFinished(e -> {
                ClipboardContent empty = new ClipboardContent();
                empty.putString("");
                Clipboard.getSystemClipboard().setContent(empty);
                formStatusLabel.setText("Clipboard cleared.");
            });
            clipboardClearTimer.play();
        } catch (Exception e) {
            formStatusLabel.setText("Could not decrypt entry.");
        }
    }

    @FXML
    private void handleView() {
        Credential selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Select an entry first.");
            return;
        }
        EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
        try {
            String plain = encryption.decrypt(selected.getEncryptedPassword());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password");
            alert.setHeaderText(selected.getWebsite() + " (" + selected.getUsername() + ")");
            alert.setContentText(plain);
            alert.showAndWait();
        } catch (Exception e) {
            formStatusLabel.setText("Could not decrypt entry.");
        }
    }

    @FXML
    private void handleEdit() {
        Credential selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Select an entry first.");
            return;
        }
        selectedForEdit = selected;
        websiteField.setText(selected.getWebsite());
        entryUsernameField.setText(selected.getUsername());
        try {
            EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
            entryPasswordField.setText(encryption.decrypt(selected.getEncryptedPassword()));
        } catch (Exception e) {
            entryPasswordField.clear();
        }
        List<String> tags = tagDAO.getTagsForItem(selected.getItemId());
        tagsField.setText(String.join(", ", tags));
        formStatusLabel.setText("Editing: " + selected.getWebsite());
    }

    @FXML
    private void handleDelete() {
        Credential selected = credentialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Select an entry first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Move the credential for " + selected.getWebsite() + " to Trash?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
            vaultItemDAO.delete(selected.getItemId());
            securityLogDAO.record(SessionManager.getInstance().getCurrentUser().getUserId(),
                    "Password moved to trash: " + selected.getWebsite());
            loadCredentials();
            formStatusLabel.setText("Moved to Trash.");
        }
    }

    @FXML
    private void handleSave() {
        String website = websiteField.getText().trim();
        String username = entryUsernameField.getText().trim();
        String password = entryPasswordField.getText();

        if (website.isEmpty() || password.isEmpty()) {
            formStatusLabel.setText("Website and password are required.");
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        EncryptionService encryption = SessionManager.getInstance().getEncryptionService();
        String encryptedPassword = encryption.encrypt(password);
        List<String> tags = Arrays.asList((tagsField.getText() == null ? "" : tagsField.getText()).split(","));

        int itemId;
        if (selectedForEdit != null) {
            credentialDAO.update(selectedForEdit.getCredentialId(), website, username, encryptedPassword);
            vaultItemDAO.touchUpdatedAt(selectedForEdit.getItemId());
            itemId = selectedForEdit.getItemId();
            securityLogDAO.record(user.getUserId(), "Password updated: " + website);
            formStatusLabel.setText("Updated " + website);
        } else {
            VaultItem item = vaultItemDAO.create(user.getUserId(), website, "Login", VaultItem.ItemType.CREDENTIAL);
            credentialDAO.create(item.getItemId(), website, username, encryptedPassword);
            itemId = item.getItemId();
            securityLogDAO.record(user.getUserId(), "Password added: " + website);
            formStatusLabel.setText("Saved " + website);
        }
        tagDAO.setTagsForItem(user.getUserId(), itemId, tags);

        handleClear();
        loadCredentials();
    }

    @FXML
    private void handleClear() {
        selectedForEdit = null;
        websiteField.clear();
        entryUsernameField.clear();
        entryPasswordField.clear();
        tagsField.clear();
    }
}
