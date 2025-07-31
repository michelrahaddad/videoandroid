package generalplus.com.GPCamLib;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Lightweight XML parser used by the GeneralPlus camera library to
 * extract camera settings from an XML configuration file.  The XML
 * format is structured into categories, settings and values.  This
 * class converts the XML tree into a set of nested Java objects for
 * easier consumption by the higher-level API.
 */
public class GPXMLParse {

    // Defines the maximum nesting level for categories, settings and
    // values.  These constants may be used by native code to
    // structure the resulting UI.
    public static final int CategoryLevel = 12;
    public static final int SettingLevel = 6;
    public static final int ValueLevel = 0;

    // Item IDs used by native code when retrieving specific settings.
    public static final int RecordResolution_Setting_ID = 0x00000000;
    public static final int CaptureResolution_Setting_ID = 0x00000100;
    public static final int Version_Setting_ID = 0x00000209;
    public static final int Version_Value_Index = 0;

    /** Internal representation of a value entry in the XML. */
    public static class GPXMLValue {
        public final String name;
        public final String id;
        public final int treeLevel;

        public GPXMLValue(String valueName, String valueID, int treeLevel) {
            this.name = valueName;
            this.id = valueID;
            this.treeLevel = treeLevel;
        }
    }

    /** Internal representation of a setting entry in the XML. */
    public static class GPXMLSetting {
        public final String name;
        public final String id;
        public final String type;
        public final String refresh;
        public final String defaultValue;
        public final String currentValue;
        public final int treeLevel;
        public final ArrayList<GPXMLValue> values;

        public GPXMLSetting(String settingName,
                            String settingID,
                            String settingType,
                            String settingRefresh,
                            String settingDefaultValue,
                            int treeLevel,
                            ArrayList<GPXMLValue> xmlValues) {
            this.name = settingName;
            this.id = settingID;
            this.type = settingType;
            this.refresh = settingRefresh;
            this.defaultValue = settingDefaultValue;
            this.treeLevel = treeLevel;
            this.values = new ArrayList<>(xmlValues);
            String current = null;
            for (GPXMLValue v : xmlValues) {
                if (v.id.equalsIgnoreCase(settingDefaultValue)) {
                    current = v.name;
                    break;
                }
            }
            this.currentValue = current;
        }
    }

    /** Internal representation of a category entry in the XML. */
    public static class GPXMLCategory {
        public final String name;
        public final int treeLevel;
        public final ArrayList<GPXMLSetting> settings;

        public GPXMLCategory(String categoryName, int treeLevel,
                             ArrayList<GPXMLSetting> xmlSettings) {
            this.name = categoryName;
            this.treeLevel = treeLevel;
            this.settings = new ArrayList<>(xmlSettings);
        }
    }

    private final ArrayList<GPXMLValue> mValues = new ArrayList<>();
    private final ArrayList<GPXMLSetting> mSettings = new ArrayList<>();
    private final ArrayList<GPXMLCategory> mCategories = new ArrayList<>();

    /**
     * Clears and returns the list of categories.  This method must be
     * called prior to parsing a new XML file.
     */
    public ArrayList<GPXMLCategory> getCategories() {
        mValues.clear();
        mSettings.clear();
        mCategories.clear();
        return mCategories;
    }

    /**
     * Parses the specified XML file and populates the category list.
     *
     * @param filePath absolute path to the XML file to parse
     * @return a list of top-level categories represented in the file
     */
    public ArrayList<GPXMLCategory> parse(String filePath) {
        getCategories();
        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            NodeList nodeListCategories = doc.getElementsByTagName("Categories");
            for (int iCategories = 0; iCategories < nodeListCategories.getLength(); iCategories++) {
                Node nodeCategories = nodeListCategories.item(iCategories);
                if (nodeCategories.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element elementCategories = (Element) nodeCategories;
                NodeList nodeListCategory = elementCategories.getElementsByTagName("Category");
                for (int iCategory = 0; iCategory < nodeListCategory.getLength(); iCategory++) {
                    Node nodeCategory = nodeListCategory.item(iCategory);
                    if (nodeCategory.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element elementCategory = (Element) nodeCategory;
                    mSettings.clear();

                    // Category name
                    String categoryName = "";
                    NodeList nodeListCategoryName = elementCategory.getElementsByTagName("Name");
                    if (nodeListCategoryName.getLength() > 0) {
                        Node nameNode = ((Element) nodeListCategoryName.item(0)).getFirstChild();
                        if (nameNode != null) {
                            categoryName = nameNode.getNodeValue();
                        }
                    }

                    // Settings
                    NodeList nodeListSettings = elementCategory.getElementsByTagName("Settings");
                    for (int iSettings = 0; iSettings < nodeListSettings.getLength(); iSettings++) {
                        Node nodeSettings = nodeListSettings.item(iSettings);
                        if (nodeSettings.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element elementSettings = (Element) nodeSettings;
                        NodeList nodeListSetting = elementSettings.getElementsByTagName("Setting");
                        for (int iSetting = 0; iSetting < nodeListSetting.getLength(); iSetting++) {
                            Node nodeSetting = nodeListSetting.item(iSetting);
                            if (nodeSetting.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            Element elementSetting = (Element) nodeSetting;
                            mValues.clear();
                            // Setting name
                            String settingName = getNodeValue(elementSetting, "Name");
                            // Setting ID
                            String settingID = getNodeValue(elementSetting, "ID");
                            // Setting type
                            String settingType = getNodeValue(elementSetting, "Type");
                            // Setting refresh
                            String settingRefresh = getNodeValue(elementSetting, "Reflash");
                            // Setting default
                            String settingDefault = getNodeValue(elementSetting, "Default");

                            // Values
                            NodeList nodeListValues = elementSetting.getElementsByTagName("Value");
                            for (int iValue = 0; iValue < nodeListValues.getLength(); iValue++) {
                                Node nodeValue = nodeListValues.item(iValue);
                                if (nodeValue.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                }
                                Element elementValue = (Element) nodeValue;
                                String valueName = getNodeValue(elementValue, "Name");
                                String valueID = getNodeValue(elementValue, "ID");
                                mValues.add(new GPXMLValue(valueName, valueID, ValueLevel));
                            }
                            mSettings.add(new GPXMLSetting(settingName, settingID,
                                    settingType, settingRefresh, settingDefault, SettingLevel,
                                    new ArrayList<>(mValues)));
                        }
                    }
                    mCategories.add(new GPXMLCategory(categoryName, CategoryLevel,
                            new ArrayList<>(mSettings)));
                }
            }
        } catch (Exception e) {
            Log.e(GPTag, "Failed to parse XML", e);
        }
        return mCategories;
    }

    private static String getNodeValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = ((Element) list.item(0)).getFirstChild();
            if (node != null) {
                return node.getNodeValue();
            }
        }
        return "";
    }

    private static final String GPTag = "GPXMLParseLog";
}