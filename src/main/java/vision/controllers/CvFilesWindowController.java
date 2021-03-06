package vision.controllers;

import com.jfoenix.controls.JFXCheckBox;
import de.felixroske.jfxsupport.FXMLController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import vision.Start;
import vision.repository.FiledRepository;
import vision.service.ParsingService;
import vision.service.ScreensManager;
import vision.utils.CommonUtils;
import vision.utils.Props;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Yuriy on 24.07.2017.
 */
@FXMLController
public class CvFilesWindowController implements Initializable {
    private final ScreensManager screensManager;
    private final ParsingService parsingService;
    private final FiledRepository filedRepository;
    private final Props props;

    private DirectoryChooser directoryChooser;

    @FXML
    private JFXCheckBox defaultPath;
    @FXML
    private Pane dragDrop;
    @FXML
    private TableView<File> fileTable;
    @FXML
    private TableColumn<File, String> fileNameColumn;
    @FXML
    private TableColumn<File, String> fileLocationColumn;
    @FXML
    private TableColumn<File, String> fileExtensionColumn;

    @Getter
    ObservableList<File> observableFiles;
    private final FileChooser fileChooser = new FileChooser();

    @Autowired
    public CvFilesWindowController(ScreensManager screensManager, ParsingService parsingService, FiledRepository filedRepository, Props props) {
        this.screensManager = screensManager;
        this.parsingService = parsingService;
        this.filedRepository = filedRepository;
        this.props = props;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configuringFileChooser(fileChooser);
        initTable();
        initRightClick();
        initDragDrop();
        initDefaultChk();
    }

    @FXML
    private void addCvFile() {
        fileChooser.setInitialDirectory(new File(CommonUtils.USER_DIR));
        if (Boolean.valueOf(props.getDEFAULT_PATH_CHK())) {
            File file = new File(props.getDEFAULT_PATH());
            fileChooser.setInitialDirectory(file);
        }
        List<File> files = fileChooser.showOpenMultipleDialog(Start.getStage());
        if (files != null) {
            addFilesToTable(files);
        }
    }

    private void configuringFileChooser(FileChooser fileChooser) {
        fileChooser.setTitle("Select CV files (PDF or DOC)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF, DOC, DOCX, TXT, HTML, RTF",
                        "*.pdf", "*.doc", "*.docx", "*.txt", "*.html", "*.rtf"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("DOCX", "*.docx"),
                new FileChooser.ExtensionFilter("DOC", "*.doc"),
                new FileChooser.ExtensionFilter("TXT", "*.txt"),
                new FileChooser.ExtensionFilter("HTML", "*.html"),
                new FileChooser.ExtensionFilter("RTF", "*.rtf")
        );
    }

    private void initTable() {
        fileNameColumn.setCellValueFactory(param ->
                new SimpleStringProperty(FilenameUtils.getBaseName(param.getValue().getPath())));
        fileLocationColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getPath()));
        fileExtensionColumn.setCellValueFactory(param ->
                new SimpleStringProperty(FilenameUtils.getExtension(param.getValue().getPath())));
        fileTable.getColumns().setAll(fileNameColumn, fileLocationColumn, fileExtensionColumn);
        observableFiles = FXCollections.observableArrayList();
    }

    private void initRightClick() {
        fileTable.setRowFactory(param -> {
            final TableRow<File> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            MenuItem addNewFile = new MenuItem("Add File");
            addNewFile.setOnAction(event -> addCvFile());

            MenuItem removeFile = new MenuItem("Remove file");
            removeFile.setOnAction(event -> removeCvFile());

            rowMenu.getItems().addAll(removeFile, addNewFile);
            row.contextMenuProperty()
                    .bind(Bindings
                            .when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu) null));
            return row;
        });
    }

    private void initDragDrop() {
        List<String> validExtensions = new ArrayList<>();
        validExtensions.add("doc");
        validExtensions.add("docx");
        validExtensions.add("pdf");
        validExtensions.add("rtf");
        validExtensions.add("html");
        validExtensions.add("txt");

        dragDrop.setOnDragOver(event -> {
            if (event.getGestureSource() != dragDrop && event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                for (File file : files) {
                    String ext = FilenameUtils.getExtension(file.getName());
                    if (!validExtensions.contains(ext)) {
                        event.consume();
                        return;
                    }
                }
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dragDrop.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getGestureSource() != dragDrop && event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();
                addFilesToTable(files);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        dragDrop.setOnMouseClicked(event -> addCvFile());
    }

    private void addFilesToTable(List<File> files) {
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (!observableFiles.contains(file)) {
                fileList.add(file);
            }
        }
        observableFiles.addAll(fileList);
        parsingService.parseAllFiles(fileList);
        fileTable.setItems(observableFiles);
    }

    @FXML
    private void removeCvFile() {
        File file = fileTable.getSelectionModel().getSelectedItem();
        if (file != null) {
            observableFiles.remove(file);
            filedRepository.removeFiled(file.getPath());
        } else {
            screensManager.showMaterialDialog("File not selected",
                    "Please select file for removing", "OK");
        }
    }

    @FXML
    void removeAllCvFiles() {
        observableFiles.clear();
        filedRepository.clearFiledRepository();
    }

    @FXML
    void setDefaultSelector() {
        File selectedDirectory = directoryChooser.showDialog(Start.getStage());
        if (selectedDirectory != null)
            props.saveToPropertiesFile("DEFAULT_PATH", selectedDirectory.getPath());
    }

    private void initDefaultChk() {
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select default folder for selecting CVs...");
        defaultPath.setSelected(Boolean.parseBoolean(props.getDEFAULT_PATH_CHK()));
    }

    @FXML
    void defaultPathClk() {
        props.saveToPropertiesFile("DEFAULT_PATH_CHK", String.valueOf(defaultPath.isSelected()));
    }

    @FXML
    void nextPageClick() {
        screensManager.showParesedFiles();
    }

    @FXML
    void backPageClick() {
        screensManager.showHomeWindow();
    }
}
