import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.geometry.*;
import java.io.*;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class ResultWindow {
    private static final String QVTitle = "Q-Values (or Adjusted P-values)";
    private static final String PVTitle = "Significant Tests";

    public static void display(ArrayList<Double> list, double[] arr, boolean isQV, double size) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle((isQV) ? QVTitle : PVTitle);

        TableView table = new TableView<>();
        table.setEditable(true);

        TableColumn<TableData, String> c1 = new TableColumn("1");
        TableColumn<TableData, String> c2 = new TableColumn("2");
        TableColumn<TableData, String> c3 = new TableColumn("3");
        TableColumn<TableData, String> c4 = new TableColumn("4");
        TableColumn<TableData, String> c5 = new TableColumn("5");
        c1.setMinWidth(125);
        c2.setMinWidth(125);
        c3.setMinWidth(125);
        c4.setMinWidth(125);
        c5.setMinWidth(125);
        c1.setCellValueFactory(new PropertyValueFactory<>("d1"));
        c2.setCellValueFactory(new PropertyValueFactory<>("d2"));
        c3.setCellValueFactory(new PropertyValueFactory<>("d3"));
        c4.setCellValueFactory(new PropertyValueFactory<>("d4"));
        c5.setCellValueFactory(new PropertyValueFactory<>("d5"));

        ObservableList<TableData> data = populate(list, arr);

        table.setItems(data);
        table.getColumns().addAll(c1, c2, c3, c4, c5);

        // instantiate menubar
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem save = new MenuItem("Export to *.txt");
        save.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Data");
                File file = fileChooser.showSaveDialog(window);
                if (file != null) {
                    try {
                        PrintWriter p = new PrintWriter(new BufferedWriter(new FileWriter(file.getAbsolutePath(), true)));
                        printData(list, arr, p);
                        p.close();
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
            }
        });
        MenuItem qv = new MenuItem("Find q-values");
        qv.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                long start = System.currentTimeMillis();
                double[] result = QV.qValues(size, list);
                long end = System.currentTimeMillis();
                ResultWindow.display(null, result, true, size);
                System.out.println("Elapsed time (ms): " + (end - start));
            }
        });
        if (!isQV) menuFile.getItems().addAll(save, qv);
        else menuFile.getItems().addAll(save);
        menuBar.getMenus().addAll(menuFile);
        VBox vBox = new VBox();
        vBox.getChildren().addAll(menuBar, table);
        Scene scene = new Scene(vBox);
        window.setScene(scene);
        window.show();
    }

    public static ObservableList<TableData> populate(ArrayList<Double> list, double[] arr) {
        ObservableList<TableData> result = FXCollections.observableArrayList();
        
        int size = (list != null) ? list.size() : arr.length;
        for (int i = 0; i < size; i+=5)
            result.add(new TableData(list, arr, i, Math.min(i+5, size)));
        return result;
    }

    private static double get(ArrayList<Double> list, double[] arr, int index) {
        return (list != null) ? list.get(index) : arr[index];
    }

    private static void printData(ArrayList<Double> list, double[] arr, PrintWriter pw) {
        int size = (list != null) ? list.size() : arr.length;
        int format = 0;
        for (int i = 0; i < size; i++) {
            double p = get(list, arr, i);        
            pw.printf((format%5==4) ? "%.7e\n" : "%.7e ", p);
            format++;
        }
    }

}