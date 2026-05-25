import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {

    private final TextArea statusDisplay = new TextArea();
    private final int TOTAL_ROOMS = 101; // Aapke pass 101 rooms hain

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hospital Management System");

        // --- Left Panel: Patient Admission form ---
        VBox admissionBox = new VBox(10);
        admissionBox.setPadding(new Insets(15));
        admissionBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");

        Label admitTitle = new Label("Admit New Patient");
        admitTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Patient Name");

        TextField diseaseInput = new TextField();
        diseaseInput.setPromptText("Disease");

        Button admitButton = new Button("Admit Patient");
        admitButton.setMaxWidth(Double.MAX_VALUE);
        admissionBox.getChildren().addAll(admitTitle, nameInput, diseaseInput, admitButton);

        // --- Right Panel: Patient Discharge form ---
        VBox dischargeBox = new VBox(10);
        dischargeBox.setPadding(new Insets(15));
        dischargeBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");

        Label dischargeTitle = new Label("Discharge Patient");
        dischargeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField dischargeNameInput = new TextField();
        dischargeNameInput.setPromptText("Enter Patient Name");

        Button dischargeButton = new Button("Discharge");
        dischargeButton.setMaxWidth(Double.MAX_VALUE);
        dischargeBox.getChildren().addAll(dischargeTitle, dischargeNameInput, dischargeButton);

        // --- Center Panel: Live Monitor Logs ---
        statusDisplay.setEditable(false);
        updateRoomStatusUI(); // Database se real-time status fetch karega

        // --- Event Button Actions ---
        admitButton.setOnAction(e -> {
            String name = nameInput.getText().trim();
            String disease = diseaseInput.getText().trim();

            if (!name.isEmpty() && !disease.isEmpty()) {
                int nextEmptyRoom = getNextAvailableRoom();

                if (nextEmptyRoom != -1) {
                    savePatientToDB(name, disease, nextEmptyRoom);
                    nameInput.clear();
                    diseaseInput.clear();
                } else {
                    showAlert(Alert.AlertType.WARNING, "Hospital Rooms are currently full!");
                }
                updateRoomStatusUI();
            }
        });

        dischargeButton.setOnAction(e -> {
            String targetName = dischargeNameInput.getText().trim();
            if (!targetName.isEmpty()) {
                boolean success = removePatientFromDB(targetName);
                if (success) {
                    dischargeNameInput.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Patient '" + targetName + "' not found active in any room.");
                }
                updateRoomStatusUI();
            }
        });

        // --- Layout assembly ---
        HBox topForms = new HBox(20, admissionBox, dischargeBox);
        topForms.setAlignment(Pos.CENTER);

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getChildren().addAll(topForms, new Label("Live Room Status:"), statusDisplay);

        Scene mainScene = new Scene(mainLayout, 550, 450);
        mainScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // 1. Database se check karna ki kaun sa room khali hai
    private int getNextAvailableRoom() {
        for (int i = 1; i <= TOTAL_ROOMS; i++) {
            String query = "SELECT id FROM patients WHERE room_number = ?";
            try (Connection con = DBConnection.connect();
                    PreparedStatement pst = con.prepareStatement(query)) {
                pst.setInt(1, i);
                ResultSet rs = pst.executeQuery();
                if (!rs.next()) {
                    return i; // Room number 'i' khali hai
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return -1; // Sab full hai
    }

    // 2. Patient ka data Database mein insert karna
    private void savePatientToDB(String name, String disease, int roomNumber) {
        String query = "INSERT INTO patients (name, disease, room_number) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.connect();
                PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, name);
            pst.setString(2, disease);
            pst.setInt(3, roomNumber);
            pst.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 3. Database se patient ko discharge (delete) karna
    private boolean removePatientFromDB(String name) {
        String query = "DELETE FROM patients WHERE LOWER(name) = LOWER(?)";
        try (Connection con = DBConnection.connect();
                PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, name);
            int rowsDeleted = pst.executeUpdate();
            return rowsDeleted > 0; // Agar delete hua toh true, nahi toh false
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // 4. UI Monitor Text ko Database ke data se dynamically refresh karna
    private void updateRoomStatusUI() {
        StringBuilder sb = new StringBuilder();
        sb.append("===============================\n");
        sb.append("   CURRENT ROOM MONITOR STATUS \n");
        sb.append("===============================\n");

        for (int i = 1; i <= TOTAL_ROOMS; i++) {
            String query = "SELECT name, disease FROM patients WHERE room_number = ?";
            try (Connection con = DBConnection.connect();
                    PreparedStatement pst = con.prepareStatement(query)) {
                pst.setInt(1, i);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    sb.append(" [Room ").append(i).append("] -> Occupied by: ")
                            .append(rs.getString("name")).append(" (Diagnosed: ")
                            .append(rs.getString("disease")).append(")\n");
                } else {
                    sb.append(" [Room ").append(i).append("] -> Empty\n");
                }
            } catch (Exception ex) {
                sb.append(" [Room ").append(i).append("] -> Error fetching data\n");
            }
        }
        statusDisplay.setText(sb.toString());
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}