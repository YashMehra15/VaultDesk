package com.vaultdesk.controller;

import com.vaultdesk.dao.SecurityLogDAO;
import com.vaultdesk.model.SecurityLog;
import com.vaultdesk.model.User;
import com.vaultdesk.service.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SecurityLogController {

    private static final int MAX_EVENTS_SHOWN = 200;

    @FXML private TableView<SecurityLog> eventTable;
    @FXML private TableColumn<SecurityLog, String> eventColumn;
    @FXML private TableColumn<SecurityLog, String> timestampColumn;

    private final SecurityLogDAO securityLogDAO = new SecurityLogDAO();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    @FXML
    private void initialize() {
        eventColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        timestampColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTimestamp() == null ? "" : c.getValue().getTimestamp().format(FORMAT)));

        loadEvents();
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
    }

    private void loadEvents() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<SecurityLog> logs = securityLogDAO.findRecentByUser(user.getUserId(), MAX_EVENTS_SHOWN);
        ObservableList<SecurityLog> rows = FXCollections.observableArrayList(logs);
        eventTable.setItems(rows);
    }
}
