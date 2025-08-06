package main.java.com.mugtaba.dataconverter.converters;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        module.setDefaultUseWrapper(false);
        xmlMapper = new XmlMapper(module);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
        JsonNode jsonNode = jsonMapper.readTree(json);

        // Build XML manually to have proper control over attributes
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<")
                .append(rootName)
                .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        buildXmlContent(jsonNode, xmlBuilder, 1);

        xmlBuilder.append("</").append(rootName).append(">");

        return formatXml(xmlBuilder.toString());
    }

    /**
     * Recursively builds XML content from JSON nodes
     * Builds the XML content from the JSON node and appends it to the StringBuilder
     *
     * @param node the JSON node
     * @param xml the StringBuilder to append the XML content
     * @param indent the current indentation level
     */
    private static void buildXmlContent(JsonNode node, StringBuilder xml, int indent) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                addIndent(xml, indent);

                if (value.isNull()) {
                    // Create a null element with xsi:nil attribute
                    xml.append("<").append(key).append(" xsi:nil=\"true\"/>\n");
                } else if (value.isArray()) {
                    // Handle arrays - each element gets the same tag name
                    for (JsonNode item : value) {

                        if (item.isNull()) {
                            xml.append("<").append(key).append(" xsi:nil=\"true\"/>\n");
                        } else if (item.isObject()) {
                            xml.append("<").append(key).append(">\n");
                            buildXmlContent(item, xml, indent + 1);
                            addIndent(xml, indent);
                            xml.append("</").append(key).append(">\n");
                        } else {
                            xml.append("<").append(key).append(">")
                                    .append(escapeXml(item.asText()))
                                    .append("</").append(key).append(">\n");
                        }
                        if (item != value.get(value.size() - 1))
                            addIndent(xml, indent);
                    }
                } else if (value.isObject()) {
                    xml.append("<").append(key).append(">\n");
                    buildXmlContent(value, xml, indent + 1);
                    addIndent(xml, indent);
                    xml.append("</").append(key).append(">\n");
                } else {
                    xml.append("<").append(key).append(">")
                            .append(escapeXml(value.asText()))
                            .append("</").append(key).append(">\n");
                }
            }
        }
    }

    /**
     * Adds proper indentation to XML
     *
     * @param xml the StringBuilder to append the indentation
     * @param indent the current indentation level
     */
    private static void addIndent(StringBuilder xml, int indent) {
        xml.append("  ".repeat(Math.max(0, indent)));
    }

    /**
     * Escapes special XML characters
     *
     * @param text the text to be escaped
     * @return the escaped text
     */
    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Simple XML formatting
     *
     * @param xml the XML string
     * @return the formatted XML string
     */
    private static String formatXml(String xml) {
        return xml;
    }

    /**
     * Converts an XML string to a JSON string.
     *
     * @param xml the XML string to be converted
     * @return the converted JSON string
     * @throws JsonProcessingException if there is an error processing the XML
     */
    public static String xmlToJson(String xml) throws JsonProcessingException {
        // Remove XML declaration if present
        String cleanXml = xml.replaceFirst("<\\?xml[^>]*\\?>\\s*", "");

        JsonNode xmlNode = xmlMapper.readTree(cleanXml);
        JsonNode correctedNode = correctTypes(xmlNode);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(correctedNode);
    }

    /**
     * Corrects the types of a JSON node, and all its descendants,
     * where they are represented as strings in the XML.
     *
     * @param node the JSON node
     * @return the corrected JSON node
     */
    private static JsonNode correctTypes(JsonNode node) {
        if (node.isObject()) {
            // Check for xsi:nil attribute
            if (node.has("@xsi:nil") && node.get("@xsi:nil").asText().equalsIgnoreCase("true")) {
                return NullNode.getInstance();
            }

            ObjectNode newObj = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();

                // Skip XML namespace attributes
                if (key.startsWith("@xmlns") || key.equals("@xsi:nil")) {
                    continue;
                }

                JsonNode value = entry.getValue();

                // Handle arrays that Jackson converts to single elements
                newObj.set(key, correctTypes(value));
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

            // Handle empty strings
            if (text.isEmpty()) {
                return node;
            }

            // Try to parse as integer
            if (text.matches("-?\\d+")) {
                try {
                    long longValue = Long.parseLong(text);
                    // Check for overflow
                    if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                        return new IntNode((int) longValue);
                    } else {
                        return new LongNode(longValue);
                    }
                } catch (NumberFormatException e) {
                    return node;
                }
            }
            // Try to parse as double
            else if (text.matches("-?\\d*\\.\\d+")) {
                try {
                    return new DoubleNode(Double.parseDouble(text));
                } catch (NumberFormatException e) {
                    return node;
                }
            }
            // Try to parse as boolean
            else if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                return BooleanNode.valueOf(Boolean.parseBoolean(text));
            }
            // Try to parse as null
            else if (text.equalsIgnoreCase("null")) {
                return NullNode.getInstance();
            }
            else {
                return node;
            }
        } else {
            return node;
        }
    }
}