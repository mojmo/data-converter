package main.java.com.mugtaba.dataconverter;

import main.java.com.mugtaba.dataconverter.converters.DynamicConverter;
import main.java.com.mugtaba.dataconverter.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                System.exit(1);
            }

            Map<String, String> arguments = parseArguments(args);
            validateArguments(arguments);

            String inputFile = arguments.get("input");
            String outputFormat = arguments.get("output");
            String inputFormat = arguments.get("format");

            // Auto-detect input format if not specified
            if (inputFormat == null) {
                inputFormat = detectInputFormat(inputFile);
            }

            convertFile(inputFile, inputFormat, outputFormat);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parses command line arguments into a key-value map
     */
    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    arguments.put(parts[0].toLowerCase(), parts[1]);
                } else {
                    throw new IllegalArgumentException("Invalid argument format: " + arg +
                            ". Expected format: --key=value");
                }
            } else {
                throw new IllegalArgumentException("Arguments must start with '--': " + arg);
            }
        }

        return arguments;
    }

    /**
     * Validates that required arguments are present and valid
     */
    private static void validateArguments(Map<String, String> arguments) {
        // Check required arguments
        if (!arguments.containsKey("input")) {
            throw new IllegalArgumentException("Missing required argument: --input");
        }

        if (!arguments.containsKey("output")) {
            throw new IllegalArgumentException("Missing required argument: --output");
        }

        // Validate input file exists
        String inputFile = arguments.get("input");
        if (!Files.exists(Paths.get(inputFile))) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile);
        }

        // Validate output format
        String outputFormat = arguments.get("output").toLowerCase();
        if (!outputFormat.equals("json") && !outputFormat.equals("xml")) {
            throw new IllegalArgumentException("Output format must be 'json' or 'xml', got: " + outputFormat);
        }

        // Validate input format if specified
        String inputFormat = arguments.get("format");
        if (inputFormat != null) {
            inputFormat = inputFormat.toLowerCase();
            if (!inputFormat.equals("json") && !inputFormat.equals("xml")) {
                throw new IllegalArgumentException("Input format must be 'json' or 'xml', got: " + inputFormat);
            }
        }
    }

    /**
     * Auto-detects input format based on file extension
     */
    private static String detectInputFormat(String inputFile) {
        String fileName = inputFile.toLowerCase();
        if (fileName.endsWith(".json")) {
            return "json";
        } else if (fileName.endsWith(".xml")) {
            return "xml";
        } else {
            throw new IllegalArgumentException(
                    "Cannot auto-detect format for file: " + inputFile +
                            ". Please specify --format=json or --format=xml");
        }
    }

    /**
     * Converts file from input format to output format
     */
    private static void convertFile(String inputFile, String inputFormat, String outputFormat)
            throws IOException {

        // Validate conversion is needed
        if (inputFormat.equalsIgnoreCase(outputFormat)) {
            throw new IllegalArgumentException(
                    "Input and output formats are the same (" + inputFormat + "). No conversion needed.");
        }

        // Read input file
        System.out.println("Reading input file: " + inputFile);
        String inputContent = FileUtils.readFile(inputFile);

        if (inputContent.isEmpty()) {
            throw new IllegalArgumentException("Input file is empty: " + inputFile);
        }

        // Perform conversion
        String convertedContent;
        String rootElementName = extractRootElementName(inputFile);

        try {
            if (inputFormat.equalsIgnoreCase("json") && outputFormat.equalsIgnoreCase("xml")) {
                System.out.println("Converting JSON to XML...");
                convertedContent = DynamicConverter.jsonToXml(inputContent, rootElementName);
            } else if (inputFormat.equalsIgnoreCase("xml") && outputFormat.equalsIgnoreCase("json")) {
                System.out.println("Converting XML to JSON...");
                convertedContent = DynamicConverter.xmlToJson(inputContent);
            } else {
                throw new IllegalArgumentException("Unsupported conversion: " + inputFormat + " to " + outputFormat);
            }
        } catch (Exception e) {
            throw new IOException("Conversion failed: " + e.getMessage(), e);
        }

        // Generate output file path
        String outputFile = generateOutputFileName(inputFile, outputFormat);

        // Write output file
        System.out.println("Writing output file: " + outputFile);
        FileUtils.writeFile(outputFile, convertedContent);

        System.out.println("Conversion completed successfully!");
        System.out.println("Input:  " + inputFile + " (" + inputFormat.toUpperCase() + ")");
        System.out.println("Output: " + outputFile + " (" + outputFormat.toUpperCase() + ")");
    }

    /**
     * Extracts root element name from file name or path
     */
    private static String extractRootElementName(String inputFile) {
        Path path = Paths.get(inputFile);
        String fileName = path.getFileName().toString();
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

        // Convert to PascalCase for XML root element
        return toPascalCase(nameWithoutExtension);
    }

    /**
     * Converts a string to PascalCase
     */
    private static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return "Root";
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }

        return !result.isEmpty() ? result.toString() : "Root";
    }

    /**
     * Generates output file name based on input file and target format
     */
    private static String generateOutputFileName(String inputFile, String outputFormat) {
        Path inputPath = Paths.get(inputFile);
        String fileName = inputPath.getFileName().toString();
        String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        String newExtension = "." + outputFormat.toLowerCase();

        Path parentDir = inputPath.getParent();
        String outputFileName = nameWithoutExtension + "_converted" + newExtension;

        if (parentDir != null) {
            return parentDir.resolve(outputFileName).toString();
        } else {
            return outputFileName;
        }
    }

    /**
     * Prints usage information
     */
    private static void printUsage() {
        System.out.println("Data Converter - JSON/XML Conversion Tool");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar data-converter.jar --input=<file> --output=<format> [--format=<format>]");
        System.out.println();
        System.out.println("Required Arguments:");
        System.out.println("  --input=<file>     Path to input file");
        System.out.println("  --output=<format>  Output format (json|xml)");
        System.out.println();
        System.out.println("Optional Arguments:");
        System.out.println("  --format=<format>  Input format (json|xml). Auto-detected if not specified.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar data-converter.jar --input=data.json --output=xml");
        System.out.println("  java -jar data-converter.jar --input=config.xml --output=json");
        System.out.println("  java -jar data-converter.jar --input=data.txt --format=json --output=xml");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  - Output file will be created in the same directory as input file");
        System.out.println("  - Output file name: <input_name>_converted.<output_format>");
        System.out.println("  - Input format is auto-detected from file extension if not specified");
    }
}