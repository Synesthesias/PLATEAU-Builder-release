package org.plateaubuilder.validation;

import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.model.citygml.ade.ADEException;
import org.citygml4j.xml.io.writer.CityGMLWriteException;
import org.plateaubuilder.core.citymodel.CityModelView;
import org.plateaubuilder.core.io.gml.GmlExporter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;


public class CityGmlUtil {

    public static Logger logger = Logger.getLogger(CityGmlUtil.class.getName());

    public static String HOME_PATH = System.getProperty("user.home");

    /**
     * Get xml document from city model
     *
     * @param cityModelView city model
     */
    public static Document getXmlDocumentFrom(CityModelView cityModelView)
            throws ParserConfigurationException, IOException, SAXException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); outputStream) {
            GmlExporter.export(outputStream, cityModelView.getGML(),
                    cityModelView.getSchemaHandler());
            return XmlUtil.getXmlDocumentFrom(new ByteArrayInputStream(outputStream.toByteArray()));
        } catch (ADEException | CityGMLBuilderException | CityGMLWriteException e) {
            logger.severe("Error while exporting city model");
            throw new RuntimeException(e);
        }
    }

}
