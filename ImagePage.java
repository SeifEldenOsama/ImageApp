package Image_process;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImagePage extends Application {
    private final Rectangle2D bounds = Screen.getPrimary().getBounds();
    private final double width = bounds.getWidth();
    private final double height = bounds.getHeight();
    private ImageView imageView;
    private final int imageSize = 500;
    private File selectedFile;
    private ProgressIndicator progressIndicator;

    @Override
    public void start(Stage stage) {
        VBox rootVBox = new VBox(10);
        rootVBox.setPadding(new Insets(20));
        rootVBox.setAlignment(Pos.TOP_CENTER);
        rootVBox.setStyle("-fx-background-color: #065a5e");

        imageView = new ImageView();
        imageView.setFitWidth(imageSize);
        imageView.setFitHeight(imageSize);
        imageView.setPreserveRatio(false);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setPrefSize(imageSize, imageSize);
        imageContainer.setMaxSize(imageSize, imageSize);
        imageContainer.setStyle("-fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 15px;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setStyle("-fx-progress-color: #00E5FF;");
        imageContainer.getChildren().add(progressIndicator);
        StackPane.setAlignment(progressIndicator, Pos.CENTER);

        Button uploadButton = createStyledButton("Upload Image", "#042b2c", "#042b2c", "/Images/uploading.png");
        Button saveButton = createStyledButton("Download Image", "#042b2c", "#042b2c", "/Images/download.png");

        uploadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                try {
                    Image image = new Image(new FileInputStream(selectedFile));
                    imageView.setImage(image);
                } catch (FileNotFoundException ex) {
                    showError("File not found: " + ex.getMessage());
                }
            }
        });

        saveButton.setOnAction(e -> {
            if (imageView.getImage() != null) {
                File saveFile = new File("C:/seif/Other/downloaded_image.png");  // Save to specific path
                try {
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(imageView.getImage(), null);
                    ImageIO.write(bufferedImage, "PNG", saveFile);
                    showInfo("Image downloaded to: " + saveFile.getAbsolutePath());
                } catch (IOException ex) {
                    showError("Error saving image: " + ex.getMessage());
                }
            }
        });

        HBox topButtons = new HBox(20, uploadButton, saveButton);
        topButtons.setAlignment(Pos.CENTER);

        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(15);
        buttonGrid.setVgap(15);
        buttonGrid.setAlignment(Pos.CENTER);

        String[][] operations = {
            {"Add Salt & Pepper", "salt_pepper"},
            {"Add Gaussian Noise", "gaussian_noise"},
            {"Gaussian Filter", "gaussian_filter"},
            {"Mean Filter", "mean_filter"},
            {"Median Filter", "median_filter"},
            {"Erosion", "erosion"},
            {"Dilation", "dilation"},
            {"Region Filling", "region_fill"},
            {"Boundary Extraction", "boundary_extract"}
        };

        int col = 0, row = 0;
        for (String[] operation : operations) {
            Button opButton = createStyledButton(operation[0], "#042b2c", "#042b2c", "");
            String command = operation[1];
            opButton.setOnAction(e -> runPythonProcess(command));
            buttonGrid.add(opButton, col, row);
            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }

        row++;

        Button morphSegmentButton = createStyledButton("Morphological Segmentation", "#042b2c", "#042b2c", "");
        morphSegmentButton.setOnAction(e -> runPythonProcess("morph_segment"));

        Button watershedButton = createStyledButton("Watershed Segmentation", "#042b2c", "#042b2c", "");
        watershedButton.setOnAction(e -> runPythonProcess("watershed_segment"));

        HBox bottomButtonsBox = new HBox(15, morphSegmentButton, watershedButton);
        bottomButtonsBox.setAlignment(Pos.CENTER);
        buttonGrid.add(bottomButtonsBox, 0, row, 3, 1);

        rootVBox.getChildren().addAll(topButtons, imageContainer, buttonGrid);

        Scene scene = new Scene(rootVBox, width, height);
        stage.setTitle("Image Processor");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private Button createStyledButton(String text, String bgColor, String hoverColor, String path) {
        Button button = new Button(text);
        button.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-text-fill: #049096;" +
            "-fx-font-size: 16px;" +
            "-fx-padding: 12px 24px;" +
            "-fx-background-radius: 10px;"
        );
        button.setPrefWidth(200);

        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                "-fx-text-fill: #049096;" +
                "-fx-font-size: 16px;" +
                "-fx-padding: 12px 24px;" +
                "-fx-background-radius: 10px;"
            );
            button.setCursor(javafx.scene.Cursor.HAND);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-text-fill: #049096;" +
                "-fx-font-size: 16px;" +
                "-fx-padding: 12px 24px;" +
                "-fx-background-radius: 10px;"
            );
            button.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        if (!path.isEmpty()) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    ImageView image = new ImageView(new Image(is));
                    button.setGraphic(image);
                    button.setContentDisplay(ContentDisplay.RIGHT);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return button;
    }

    private void runPythonProcess(String operation) {
        if (selectedFile == null) return;

        progressIndicator.setVisible(true);

        new Thread(() -> {
            try {
                String workingDir = new File(System.getProperty("user.dir")).getAbsolutePath();
                String scriptPath = workingDir + "/src/API/pythonapi.py";

                // Define output file path (can be fixed or temp)
                File outputFile = new File("C:/seif/Other/processed_image.png");

                ProcessBuilder pb = new ProcessBuilder(
                    "python", scriptPath,
                    selectedFile.getAbsolutePath(),
                    outputFile.getAbsolutePath(),
                    operation
                );
                pb.directory(new File(workingDir));
                pb.redirectErrorStream(true);

                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    showError("Python script failed.");
                } else {
                    // Load the processed image (output file)
                    Image updated = new Image(new FileInputStream(outputFile));
                    javafx.application.Platform.runLater(() -> imageView.setImage(updated));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error running Python script.");
            } finally {
                javafx.application.Platform.runLater(() -> progressIndicator.setVisible(false));
            }
        }).start();
    }



    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
