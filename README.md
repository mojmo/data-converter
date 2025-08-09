# Data Converter

![Java Version](https://img.shields.io/badge/Java-21-orange)
![CLI](https://img.shields.io/badge/UI-CLI-yellow)
![License : Apache 2.0](https://img.shields.io/badge/License-Apache-blue)
[![GitHub Release](https://img.shields.io/github/v/release/mojmo/data-converter)](https://github.com/mojmo/data-converter/releases)

A simple Java command-line tool for seamless bidirectional conversion between JSON and XML formats, with proper null value handling and automatic data type preservation.

## Features

- **Bidirectional Conversion**: Convert JSON to XML and XML back to JSON
- **Automatic Format Detection**: Detects input format based on file extension
- **Custom Root Element**: Automatically generates XML root element names from filenames
- **Null Value Support**: Proper handling of null values using XML Schema Instance (`xsi:nil="true"`)
- **Type Preservation**: Maintains data types (integers, floats, booleans) during XML to JSON conversion
- **Array Handling**: Correctly processes JSON arrays and converts them to repeated XML elements
- **XML Escaping**: Properly escapes special characters in XML content
- **File Management**: Automatic output file naming and directory creation

## Technical Overview

### Architecture

The project follows a modular architecture with three main components:

- **Main.java**: Command-line interface and argument handling
- **DynamicConverter.java**: Core conversion logic with manual XML building
- **FileUtils.java**: File I/O operations with proper error handling

### Conversion Process

#### JSON to XML Conversion

The JSON to XML conversion process uses a **manual XML building approach** rather than relying solely on Jackson's XML mapping. This decision was made to overcome Jackson's limitation with XML null attributes (`xsi:nil="true"`).

**Process Flow:**
1. Parse JSON string into Jackson's JsonNode tree structure
2. Create XML declaration with UTF-8 encoding
3. Add root element with XML Schema Instance namespace
4. Recursively traverse JsonNode tree and build XML manually
5. Handle different node types:
   - **Null values**: Generate `<element xsi:nil="true"/>` 
   - **Objects**: Create nested XML elements
   - **Arrays**: Generate repeated elements with the same tag name
   - **Primitives**: Create simple text elements with proper XML escaping

**Key Implementation Details:**

```java
// Manual XML building for null values (Jackson doesn't support this)
if (value.isNull()) {
    xml.append("<").append(key).append(" xsi:nil=\"true\"/>\n");
}

// Array handling - each item gets the same element name
for (JsonNode item : arrayNode) {
    if (item.isNull()) {
        xml.append("<").append(key).append(" xsi:nil=\"true\"/>\n");
    } else {
        xml.append("<").append(key).append(">")
           .append(escapeXml(item.asText()))
           .append("</").append(key).append(">\n");
    }
}
```

#### XML to JSON Conversion

The XML to JSON conversion process combines Jackson's XML parsing with custom type correction logic:

**Process Flow:**
1. Remove XML declaration if present
2. Parse XML using Jackson's XmlMapper
3. Apply type correction algorithm to restore proper JSON data types
4. Handle special XML attributes:
   - Convert `xsi:nil="true"` back to JSON null
   - Remove namespace declarations and XML-specific attributes
5. Restore proper data types (integers, floats, booleans) that XML represents as strings

**Type Correction Algorithm:**
- **Integers**: Pattern matching with `-?\d+` regex
- **Floats**: Pattern matching with `-?\d*\.\d+` regex  
- **Booleans**: Case-insensitive "true"/"false" matching
- **Null values**: Detect `xsi:nil="true"` attribute or "null" strings

### Why Manual XML Building?

Jackson's XML module has several limitations that required a custom approach:

1. **No Native xsi:nil Support**: Jackson cannot generate `xsi:nil="true"` attributes for null values
2. **Array Handling**: Jackson wraps arrays in container elements, but we need repeated elements
3. **Namespace Control**: Manual building provides precise control over XML namespaces
4. **Custom Formatting**: Allows for consistent indentation and formatting

## Installation & Building

### Download Pre-built Release

Download the latest pre-built JAR from the [GitHub Releases](https://github.com/mojmo/data-converter/releases) page.

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Build from Source

```bash
# Clone the repository
git clone https://github.com/mojmo/data-converter.git
cd data-converter

# Build the project
mvn clean package

# The executable JAR will be created in the target directory
# target/data-converter-1.0.0.jar
```

### Dependencies

The project uses the following key dependencies:

- **Jackson Databind** (2.15.0): JSON processing and object mapping
- **Jackson XML Format** (2.15.0): XML parsing capabilities
- **Maven Shade Plugin**: Creates executable JAR with all dependencies

## Usage

### Command Line Interface

```bash
java -jar data-converter-1.0.0.jar --input=<file> --output=<format> [--format=<format>]
```

### Arguments

**Required:**
- `--input=<file>`: Path to the input file
- `--output=<format>`: Output format (`json` or `xml`)

**Optional:**
- `--format=<format>`: Input format (`json` or `xml`). Auto-detected if not specified
- `--help`: Display usage information and exit

### Examples

#### Convert JSON to XML
```bash
java -jar data-converter-1.0.0.jar --input=data.json --output=xml
```

**Input (data.json):**
```json
{
  "name": "John Doe",
  "age": 30,
  "email": null,
  "active": true,
  "hobbies": ["reading", "gaming", null]
}
```

**Output (data_converted.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <name>John Doe</name>
  <age>30</age>
  <email xsi:nil="true"/>
  <active>true</active>
  <hobbies>reading</hobbies>
  <hobbies>gaming</hobbies>
  <hobbies xsi:nil="true"/>
</Data>
```

#### Convert XML to JSON
```bash
java -jar data-converter-1.0.jar --input=config.xml --output=json
```

**Input (config.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <database>
    <host>localhost</host>
    <port>5432</port>
    <password xsi:nil="true"/>
  </database>
</Configuration>
```

**Output (config_converted.json):**
```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "password": null
  }
}
```

#### Specify Input Format Explicitly
```bash
java -jar data-converter-1.0.0.jar --input=data.txt --format=json --output=xml
```

#### Get Help
```bash
java -jar data-converter-1.0.0.jar --help
```

This will display detailed usage information including all available options and examples.

### File Naming Convention

Output files are automatically named using the pattern: `{original_name}_converted.{new_extension}`

- `data.json` → `data_converted.xml`
- `config.xml` → `config_converted.json`

### Root Element Naming

For JSON to XML conversion, the root element name is derived from the input filename:

- `user_profile.json` → `<UserProfile>`
- `api-config.json` → `<ApiConfig>`
- `settings.json` → `<Settings>`

The converter automatically converts filenames to PascalCase for XML root elements.

## Error Handling

The application provides comprehensive error handling for common scenarios:

- **File not found**: Clear error message with file path
- **Invalid format**: Validation of input/output format parameters
- **Parsing errors**: Detailed JSON/XML parsing error messages
- **I/O errors**: File read/write permission and access issues
- **Conversion errors**: Data type conversion and structure validation

## Technical Specifications

### Supported Data Types

| JSON Type | XML Representation | Notes |
|-----------|-------------------|--------|
| `null` | `<element xsi:nil="true"/>` | Custom implementation |
| `string` | `<element>text</element>` | XML-escaped content |
| `number` | `<element>123</element>` | Preserved during round-trip |
| `boolean` | `<element>true</element>` | Case-insensitive parsing |
| `array` | Multiple `<element>` tags | Repeated elements |
| `object` | Nested XML elements | Recursive structure |

### XML Namespace Handling

- **xsi:nil**: `http://www.w3.org/2001/XMLSchema-instance` for null values
- **Namespace cleanup**: Removes XML-specific attributes during JSON conversion
- **UTF-8 encoding**: Consistent character encoding across all operations

## Performance Considerations

- **Memory efficient**: Streaming approach for large files
- **Type optimization**: Intelligent data type detection and conversion
- **Error recovery**: Graceful handling of malformed input data
- **File I/O**: Optimized read/write operations with proper buffering

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the Apache-2.0 License - see the [LICENSE](LICENSE) file for details.

## Version History

- **1.0.0**: Initial release with bidirectional JSON/XML conversion and null value support
