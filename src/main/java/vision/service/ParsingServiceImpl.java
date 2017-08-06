package vision.service;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import javafx.collections.ObservableList;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import vision.controllers.ParsedFilesController;
import vision.models.Filed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * @author Yuriy on 01.08.2017.
 */
@Service
public class ParsingServiceImpl implements ParsingService {
    private FileInputStream inputStream;
    private String parsedStatus = "OK";
    private String extractedStatus = "In queue";
    private final ParsedFilesController parsedFilesController;
    private LanguageDetector languageDetector;
    private TextObjectFactory textObjectFactory;

    @Autowired
    ParsingServiceImpl(ParsedFilesController parsedFilesController) {
        this.parsedFilesController = parsedFilesController;
        try {
            initLanguageDetector();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void parseAllFiles(ObservableList<File> files) {
        new Thread(() -> {
            if (files.size() > 0) {
                for (File file : files) {
                    String text = parseToText(file);
                    parsedFilesController.addFiledToTable(new Filed(file, identifyLanguage(text), text, parsedStatus, null, extractedStatus));
                }
            }
        }).start();
    }

    @Override
    public String parseToText(File file) {
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext pcontext = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();
        try {
            parser.parse(inputStream, handler, metadata, pcontext);
            return handler.toString();
        } catch (IOException e) {
            parsedStatus = "IO error";
            e.printStackTrace();
        } catch (SAXException e) {
            parsedStatus = "SAX error";
            e.printStackTrace();
        } catch (TikaException e) {
            parsedStatus = "Tika error";
            e.printStackTrace();
        }
        return null;
    }

    private void initLanguageDetector() throws IOException {
        //load all languages:
        List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
        //build language detector:
        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();
        //create a text object factory
        textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }

    @Override
    public String identifyLanguage(String text) {
        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()) {
            return lang.get().getLanguage();
        } else {
            return "Can't detect";
        }
    }
}