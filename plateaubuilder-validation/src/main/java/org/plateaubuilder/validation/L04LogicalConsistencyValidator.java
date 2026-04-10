package org.plateaubuilder.validation;

import org.plateaubuilder.core.citymodel.CityModelView;
import org.plateaubuilder.validation.constant.MessageError;
import org.plateaubuilder.validation.constant.TagName;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class L04LogicalConsistencyValidator implements IValidator {
    private CityModelView targetCityModelView;
    private final java.util.Map<String, java.util.Set<String>> codeSpaceValuesCache = new java.util.HashMap<>();
    @Override
    public List<ValidationResultMessage> validate(CityModelView cityModelView)
            throws ParserConfigurationException, IOException, SAXException {
        targetCityModelView = cityModelView;
        codeSpaceValuesCache.clear();

        List<ValidationResultMessage> messages = new ArrayList<>();

        NodeList buildings = CityGmlUtil.getXmlDocumentFrom(cityModelView).getElementsByTagName(TagName.BLDG_BUILDING);

        for (int i = 0; i < buildings.getLength(); i++) {
            List<GmlElementError> elementErrors = new ArrayList<>();
            Node building = buildings.item(i);
            Element buildingE = (Element) building;
            String buildingID = buildingE.getAttribute(TagName.GML_ID);

            List<Node> tagHaveCodeSpaces = new ArrayList<>();
            XmlUtil.recursiveFindNodeByAttribute(building, tagHaveCodeSpaces, TagName.ATTRIBUTE_CODE_SPACE);
            List<String> invalidCodeSpaces = this.getInvalidCodeSpaces(tagHaveCodeSpaces);
            if (invalidCodeSpaces.isEmpty()) continue;
            elementErrors.add(new GmlElementError(
                    buildingID,
                    null,
                    null,
                    invalidCodeSpaces.toString(),
                    null,
                    0
            ));

            StringBuilder errorMessage = new StringBuilder(MessageError.ERR_L04_002_1);
            for (String invalid : invalidCodeSpaces) {
                errorMessage.append(MessageFormat.format(MessageError.ERR_L04_002_2, invalid));
            }

            if (!errorMessage.toString().equals(MessageError.ERR_L04_002_1)) {
                messages.add(new ValidationResultMessage(ValidationResultMessageType.Error, errorMessage.toString(), elementErrors));
            }
        }

        return messages;
    }

    private List<String> getInvalidCodeSpaces(List<Node> tagHaveCodeSpaces) throws ParserConfigurationException, IOException, SAXException {
        List<String> result = new ArrayList<>();
        for (Node tag : tagHaveCodeSpaces) {
            Element element = (Element) tag;
            if (!this.checkTagValid(element)) {
                String linkCodeSpace = element.getAttribute(TagName.ATTRIBUTE_CODE_SPACE).trim();
                result.add(linkCodeSpace);
            }
        }

        return result;
    }

    private boolean checkTagValid(Element tagInput) throws ParserConfigurationException, IOException, SAXException {
        String linkCodeSpace = tagInput.getAttribute(TagName.ATTRIBUTE_CODE_SPACE).trim();
        if (linkCodeSpace.isBlank()) return false;

        File codeSpaceFile = resolveCodeSpaceFile(linkCodeSpace);
        if (codeSpaceFile == null || !codeSpaceFile.isFile()) return false;

        String key = codeSpaceFile.getAbsolutePath();
        java.util.Set<String> allowed = codeSpaceValuesCache.get(key);
        if (allowed == null) {
            allowed = loadCodeSpaceNames(codeSpaceFile);
            codeSpaceValuesCache.put(key, allowed);
        }

        String contentInput = tagInput.getTextContent().trim();
        return allowed.contains(contentInput);
    }
    private File resolveCodeSpaceFile(String linkCodeSpace) {
        String[] paths = linkCodeSpace.split("/");
        File base = new File(targetCityModelView.getGmlPath()).getParentFile();
        if (base == null) return null;

        File cur = base;
        StringBuilder rel = new StringBuilder();

        for (String part : paths) {
            if (part.isEmpty() || ".".equals(part)) continue;

            if ("..".equals(part)) {
                cur = cur.getParentFile();
                if (cur == null) return null;
                continue;
            }

            if (rel.length() == 0) rel.append(part);
            else rel.append("/").append(part);
        }

        return rel.length() == 0 ? null : new File(cur, rel.toString());
    }

    private java.util.Set<String> loadCodeSpaceNames(File codeSpaceFile)
            throws ParserConfigurationException, IOException, SAXException {
        NodeList codeSpaces = XmlUtil.getAllTagFromXmlFile(codeSpaceFile, TagName.GML_NAME);
        java.util.Set<String> values = new java.util.HashSet<>(Math.max(16, codeSpaces.getLength() * 2));

        for (int i = 0; i < codeSpaces.getLength(); i++) {
            Node n = codeSpaces.item(i);
            if (n instanceof Element) {
                String v = n.getTextContent();
                if (v != null) {
                    v = v.trim();
                    if (!v.isEmpty()) values.add(v);
                }
            }
        }
        return values;
    }
}
