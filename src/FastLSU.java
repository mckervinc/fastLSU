import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.util.*;

public class FastLSU extends Application {

    public static void main(String[] args) {
        warmup();
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("FastLSU");
        Scene scene = new Scene(new VBox(), 400, 350);

        // instantiate the Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                System.exit(0);
            }
        });

        // Text Boxes
        TextField sigLevel = new TextField();
        TextField sampleSize = new TextField();
        TextField fileLocation = new TextField();
        sigLevel.setPrefWidth(50);      

        // file handling
        FileChooser fileChooser = new FileChooser();
        MenuItem open = new MenuItem("Open File");
        open.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                File file = fileChooser.showOpenDialog(stage);
                if (file != null) fileLocation.setText(file.getAbsolutePath());
            }
        });

        // General Information
        final Label title = new Label("FastLSU: Efficient Control of the False Discovery Rate");
        final Label description = new Label("This tool uses a linear time procedure to control " + 
            "the rate of Type 1 errors in a given data set. To use this tool, browse for the" + 
            "file to process in the File menu (press the appropriate button if you are calculating the LSU or the " +
            "q-values). Below that, indicate the significance level you are testing. " + 
            "The final result will appear in a new window.");
        title.setFont(new Font("Verdana Bold", 22));
        title.setWrapText(true);
        title.setTextAlignment(TextAlignment.CENTER);
        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.JUSTIFY);

        // Grid text
        Label a = new Label("Please insert a valid significance level between [0, 1]");
        Label b = new Label("Please insert a valid size");
        Label c = new Label("File does not exist");
        a.setWrapText(true);

        // for error handling of inputs
        VBox errors = new VBox();
        errors.setAlignment(Pos.CENTER);
        errors.setSpacing(10);
        errors.setPadding(new Insets(0, 10, 0, 10));

        // add our main buttons
        Button run = new Button("Find Results!");
        run.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                String alpha = sigLevel.getText();
                String loc = fileLocation.getText();
                String m = sampleSize.getText();
                File f = new File(loc);
                boolean perform = true;
                int row = 0;
                errors.getChildren().remove(a);
                errors.getChildren().remove(b);
                errors.getChildren().remove(c);
                if (!isDouble(alpha)) {
                    errors.getChildren().add(a);
                    row++;
                    perform = false;
                }
                if (!isDouble(m)) {
                    errors.getChildren().add(b);
                    row++;
                    perform = false;    
                }
                if (!f.exists()) {
                    errors.getChildren().add(c);
                    row++;
                    perform = false;
                }
                if (perform) {
                    double sig = Double.parseDouble(alpha);
                    double size = Double.parseDouble(m);
                    if (sig >= 0.0 || sig <= 1.0) {
                        FastBHConcurrent fbhc = new FastBHConcurrent(size, sig, loc);
                        try {
                            if (fbhc.fitsInMem()) {
                                PValues[] arr = fbhc.load();
                                long start = System.currentTimeMillis();
                                ArrayList<Double> result = fbhc.solver(arr);
                                long end = System.currentTimeMillis();
                                System.out.println("==============================");
                                System.out.println("Elapsed Time (ms): " + (end - start));
                                ResultWindow.display(result, null, false, size);
                            }
                            else {
                                long time = 0;
                                ArrayList<Double> result = null;
                                while (!fbhc.isFinished()) {
                                    PValues[] arr = fbhc.loadInMem();
                                    long start = System.currentTimeMillis();
                                    result = fbhc.solverInMem(arr);
                                    long end = System.currentTimeMillis();
                                    time += (end - start);
                                }
                                System.out.println("==============================");
                                System.out.println("Elapsed Time (ms): " + time);
                                ResultWindow.display(result, null, false, size);
                            }
                        } catch (Exception e) {e.printStackTrace();}
                    }
                    else errors.getChildren().add(a);
                }
            }
        });
        Button qv = new Button("Find q-values!");
        qv.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                String loc = fileLocation.getText();
                String m = sampleSize.getText();
                File f = new File(loc);
                boolean perform = true;
                int row = 0;
                errors.getChildren().remove(a);
                errors.getChildren().remove(b);
                errors.getChildren().remove(c);
                if (!isDouble(m)) {
                    errors.getChildren().add(b);
                    row++;
                    perform = false;    
                }
                if (!f.exists()) {
                    errors.getChildren().add(c);
                    row++;
                    perform = false;
                }
                if (perform) {
                    double size = Double.parseDouble(m);    
                    try {
                        ArrayList<Double> arr = QV.load(loc);
                        long start = System.currentTimeMillis();
                        double[] result = QV.qValues(size, arr);
                        long end = System.currentTimeMillis();
                        System.out.println("==============================");
                        System.out.println("Elapsed Time (ms): " + (end - start));
                        ResultWindow.display(null, result, true, size);
                    } catch (Exception e) {e.printStackTrace();}
                }
            }
        });

        // define the layout for the procedure
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(0, 10, 0, 10));
        grid.add(new Label("Significance Level in range [0,1]: "), 0, 1);
        grid.add(sigLevel, 1, 1);
        grid.add(new Label("Input Size (Precise): "), 0, 2);
        grid.add(sampleSize, 1, 2);
        grid.add(new Label("File Location: "), 0, 3);
        grid.add(fileLocation, 1, 3);
        grid.add(run, 0, 4);
        grid.add(qv, 1, 4);


        // define the layout for the main window
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(0, 10, 0, 10));
        vbox.getChildren().addAll(title, description);

        // add everything to the view
        menuFile.getItems().addAll(open, exit);
        menuBar.getMenus().addAll(menuFile);
        ((VBox) scene.getRoot()).getChildren().addAll(menuBar, vbox, grid, errors);

        stage.setScene(scene);
        stage.show();
    }

    // tokenizing check
    private boolean isDouble(String s) {
        if (s == null || s.length() == 0) return false;
        try {
            Double.parseDouble(s);
            return true;
        }
        catch (Exception e) {return false;}
    }

    // does random binomial given a certain probability
    private static int flip(double probability) {
        return (Math.random() < probability) ? 1 : 0;
    }

    // for JIT compilation optimization
    private static void warmup() {
        int m = (int)Math.pow(10, 6);
        int numCores = Runtime.getRuntime().availableProcessors();
        double alpha = 0.1;
        double[][] chunks = new double[numCores][2];
        double size = (double)m;
        PValues[] result = new PValues[numCores];
        FastBHConcurrent fbhc = new FastBHConcurrent(size, alpha, null);

        // divide 1million numbers into cores chunks
        for (int i = 0; i < numCores; i++) {
            chunks[i][0] = Math.floor((size * i) / numCores);
            chunks[i][1] = Math.floor((size * (i + 1)) / numCores - 1);
        }

        // load the values into a double array
        int coreCount = 0;
        PValues elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
        int elem_size = elem.size(), pointer = 0;
        double rci = 0.0, sum = 0.0, p = 0.0, oldrci = 0.0;
        for (int i = 0; i < m; i++) {
            try {
                p = Math.random() / Math.pow(10, flip(0.8));
                oldrci = rci;
                if (p < alpha) rci++;
                elem.array[pointer] = p;
                pointer++;
            }
            catch (ArrayIndexOutOfBoundsException e) {
                if (rci != oldrci) {
                    elem.prime = oldrci;
                    sum += oldrci;
                    rci = 1.0;
                }
                else {
                    elem.prime = rci;
                    sum += rci;
                    rci = 0.0;
                }
                result[coreCount] = elem;
                oldrci = rci;
                coreCount++;
                if (coreCount == numCores) {
                    
                    break;
                }
                pointer = 1;
                elem = new PValues(chunks[coreCount][1] - chunks[coreCount][0] + 1);
                elem_size = elem.size();
                elem.array[0] = p;
            }
        }

        elem.prime = rci;
        result[coreCount] = elem;
        sum += rci;
        fbhc.cSum(sum);

        // process it one time
        try {
            ArrayList<Double> x = fbhc.solver(result);
            x = null;
            result = null;
            fbhc = null;
            elem = null;
            chunks = null;
        }
        catch (Exception e) {e.printStackTrace();}
    }
}