package com.mugtaba.dataconverter.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.dataformat.xml.*;

import java.util.Iterator;
import java.util.Map;

public class DynamicConverter {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper;

    static {
        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(true); // Needed for arrays
        xmlMapper = new XmlMapper(module);
    }

    /**
     * Converts a JSON string to an XML string with the specified root element name.
     *
     * @param json the JSON string to be converted
     * @param rootName the name of the root element in the resulting XML
     * @return the converted XML string
     * @throws JsonProcessingException if there is an error processing the JSON
     */
    public static String jsonToXml(String json, String rootName) throws JsonProcessingException {
        ObjectNode node = (ObjectNode) jsonMapper.readTree(json);
        return xmlMapper.writer()
                .withRootName(rootName)
                .withDefaultPrettyPrinter()
                .writeValueAsString(node);
    }

    /**
     * Converts an XML string to a JSON string.
     *
     * @param xml the XML string to be converted
     * @return the converted JSON string
     * @throws JsonProcessingException if there is an error processing the XML
     */
    public static String xmlToJson(String xml) throws JsonProcessingException {
        JsonNode xmlNode = xmlMapper.readTree(xml);
        JsonNode correctedNode = correctTypes(xmlNode);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(correctedNode);
    }

    // Fix numbers and booleans

    /**
     * Corrects the types of a JSON node, and all its descendants, where they are represented as strings in the XML.
     *
     * <p>If the node is an object, it is traversed recursively.</p>
     *
     * <p>If the node is an array, each element is traversed recursively.</p>
     *
     * <p>If the node is a string, it is attempted to be converted to one of the following types:</p>
     * <ul>
     * <li>Integer: if the string matches <code>-?\d+</code></li>
     * <li>Double: if the string matches <code>-?\d+\.\d+</code></li>
     * <li>Boolean: if the string is equal to "true" or "false" (case-insensitive)</li>
     * <li>Null: if the string is equal to "null" (case-insensitive)</li>
     * </ul>
     *
     * <p>If the node is not corrected, it is returned as is.</p>
     *
     * @param node the node to be corrected
     * @return the corrected node
     */
    private static JsonNode correctTypes(JsonNode node) {
        if (node.isObject()) {
            ObjectNode newObj = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                newObj.set(entry.getKey(), correctTypes(entry.getValue()));
            }
            return newObj;
        } else if (node.isArray()) {
            ArrayNode newArr = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : node) {
                newArr.add(correctTypes(item));
            }
            return newArr;
        } else if (node.isTextual()) {
            String text = node.asText();
            if (text.matches("-?\\d+")) {
                return new IntNode(Integer.parseInt(text));
            } else if (text.matches("-?\\d+\\.\\d+")) {
                return new DoubleNode(Double.parseDouble(text));
            } else if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                return BooleanNode.valueOf(Boolean.parseBoolean(text));
            } else if (text.equalsIgnoreCase("null")) {
                return NullNode.getInstance();
            } else {
                return node;
            }
        } else {
            return node;
        }
    }
}

