package mituran.gglua.tool.apktools;

import com.apk.axml.serializableItems.XMLEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * AndroidManifest.xml修改器
 * 用于修改已解码的XML内容
 */
public class ManifestModifier {

    private List<XMLEntry> xmlEntries;

    public ManifestModifier(List<XMLEntry> xmlEntries) {
        this.xmlEntries = new ArrayList<>(xmlEntries);
    }

    /**
     * 设置包名
     * 需要修改:
     * 1. manifest的package属性
     * 2. receiver的intent-filter下的name属性
     * 3. provider的authorities属性
     * 4. instrumentation的targetPackage属性
     */
    public void setPackageName(String newPackageName) {
        String oldPackageName = getPackageNameInternal();

        // 1. 修改manifest标签的package属性
        modifyManifestPackage(newPackageName);

        // 2. 修改receiver中的包名引用
        if (oldPackageName != null) {
            modifyReceiverPackageReferences(oldPackageName, newPackageName);

            // 3. 修改provider的authorities
            modifyProviderAuthorities(oldPackageName, newPackageName);

            // 4. 修改instrumentation的targetPackage
            modifyInstrumentationTargetPackage(oldPackageName, newPackageName);
        }
    }

    /**
     * 获取当前包名（公开方法）
     */
    public String getCurrentPackageName() {
        return getPackageNameInternal();
    }

    /**
     * 获取当前包名（内部方法）
     */
    private String getPackageNameInternal() {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<manifest") || tag.equals("<manifest")) {
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if (attrTag.equals("package") && attrEntry.getMiddleTag().equals("=\"")) {
                        return attrEntry.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 修改manifest的package属性
     */
    private void modifyManifestPackage(String newPackageName) {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<manifest") || tag.equals("<manifest")) {
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if (attrTag.equals("package") && attrEntry.getMiddleTag().equals("=\"")) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                newPackageName,
                                attrEntry.getEndTag()
                        ));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 修改receiver中的包名引用
     * 主要修改intent-filter中的android:name属性
     */
    private void modifyReceiverPackageReferences(String oldPackage, String newPackage) {
        boolean inReceiver = false;

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            // 检测是否进入receiver标签
            if (tag.startsWith("<receiver") || tag.equals("<receiver")) {
                inReceiver = true;
            } else if (tag.startsWith("</receiver")) {
                inReceiver = false;
            }

            // 在receiver内部查找包含旧包名的属性
            if (inReceiver) {
                // 检查所有属性值
                String value = entry.getValue();
                if (value != null && value.startsWith(oldPackage)) {
                    String newValue = value.replace(oldPackage, newPackage);
                    xmlEntries.set(i, new XMLEntry(
                            entry.getTag(),
                            entry.getMiddleTag(),
                            newValue,
                            entry.getEndTag()
                    ));
                }
            }
        }
    }

    /**
     * 修改provider的authorities属性
     */
    private void modifyProviderAuthorities(String oldPackage, String newPackage) {
        boolean inProvider = false;

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<provider") || tag.equals("<provider")) {
                inProvider = true;
            } else if (tag.startsWith("</provider") || tag.equals("</provider")) {
                inProvider = false;
            }

            if (inProvider) {
                String attrTag = tag;
                if ((attrTag.equals("android:authorities") || attrTag.endsWith("android:authorities"))
                        && entry.getMiddleTag().equals("=\"")) {
                    String value = entry.getValue();
                    if (value != null && value.contains(oldPackage)) {
                        String newValue = value.replace(oldPackage, newPackage);
                        xmlEntries.set(i, new XMLEntry(
                                entry.getTag(),
                                entry.getMiddleTag(),
                                newValue,
                                entry.getEndTag()
                        ));
                    }
                }
            }
        }
    }

    /**
     * 修改instrumentation的targetPackage属性
     */
    private void modifyInstrumentationTargetPackage(String oldPackage, String newPackage) {
        boolean inInstrumentation = false;

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<instrumentation") || tag.equals("<instrumentation")) {
                inInstrumentation = true;
            } else if (tag.startsWith("</instrumentation") || tag.equals("</instrumentation")) {
                inInstrumentation = false;
            }

            if (inInstrumentation) {
                String attrTag = tag;
                if ((attrTag.equals("android:targetPackage") || attrTag.endsWith("android:targetPackage"))
                        && entry.getMiddleTag().equals("=\"")) {
                    xmlEntries.set(i, new XMLEntry(
                            entry.getTag(),
                            entry.getMiddleTag(),
                            newPackage,
                            entry.getEndTag()
                    ));
                }
            }
        }
    }

    /**
     * 设置版本名
     */
    public void setVersionName(String newVersionName) {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            // 在manifest标签的属性中查找android:versionName
            if (tag.startsWith("<manifest") || tag.equals("<manifest")) {
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if ((attrTag.equals("android:versionName") || attrTag.endsWith("android:versionName")) &&
                            attrEntry.getMiddleTag().equals("=\"")) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                newVersionName,
                                attrEntry.getEndTag()
                        ));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 设置版本号
     */
    public void setVersionCode(String newVersionCode) {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<manifest") || tag.equals("<manifest")) {
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if ((attrTag.equals("android:versionCode") || attrTag.endsWith("android:versionCode")) &&
                            attrEntry.getMiddleTag().equals("=\"")) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                newVersionCode,
                                attrEntry.getEndTag()
                        ));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 设置应用标签（应用名）
     * 需要修改:
     * 1. application的android:label
     * 2. 所有activity的android:label
     */
    public void setApplicationLabel(String newLabel) {
        // 1. 修改application的label
        modifyApplicationLabel(newLabel);

        // 2. 修改所有activity的label
        modifyAllActivityLabels(newLabel);
    }

    /**
     * 修改application的label
     */
    private void modifyApplicationLabel(String newLabel) {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<application") || tag.equals("<application")) {
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if ((attrTag.equals("android:label") || attrTag.endsWith("android:label")) &&
                            attrEntry.getMiddleTag().equals("=\"")) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                newLabel,
                                attrEntry.getEndTag()
                        ));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 修改所有activity的label
     */
    private void modifyAllActivityLabels(String newLabel) {
        boolean inActivity = false;

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            // 检测activity标签
            if (tag.startsWith("<activity") || tag.equals("<activity")) {
                inActivity = true;

                // 查找该activity的label属性
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    // 遇到下一个标签，结束该activity
                    if (attrTag.startsWith("<") && !attrTag.startsWith("</") &&
                            !attrTag.equals(attrEntry.getTag())) {
                        break;
                    }

                    // 找到label属性，修改它
                    if ((attrTag.equals("android:label") || attrTag.endsWith("android:label")) &&
                            attrEntry.getMiddleTag().equals("=\"")) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                newLabel,
                                attrEntry.getEndTag()
                        ));
                        break;
                    }
                }
            } else if (tag.startsWith("</activity")) {
                inActivity = false;
            }
        }
    }

    /**
     * 删除指定Activity
     */
    public void removeActivity(String activityName) {
        int startIndex = -1;
        int endIndex = -1;
        int depth = 0;

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<activity") || tag.equals("<activity")) {
                // 检查是否是目标activity
                boolean isTarget = false;
                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        break;
                    }

                    if ((attrTag.equals("android:name") || attrTag.endsWith("android:name")) &&
                            attrEntry.getValue().equals(activityName)) {
                        isTarget = true;
                        break;
                    }
                }

                if (isTarget) {
                    startIndex = i;
                    depth = 1;

                    // 查找对应的结束标签
                    for (int k = i + 1; k < xmlEntries.size(); k++) {
                        XMLEntry nextEntry = xmlEntries.get(k);
                        String nextTag = nextEntry.getTag().trim();

                        if (nextTag.startsWith("<") && !nextTag.startsWith("</")) {
                            // 自闭合标签（mEndTag 含 "/>"）不增加深度
                            String endTag = nextEntry.getEndTag();
                            if (endTag != null && endTag.trim().startsWith("/>")) {
                                // self-closing, depth unchanged
                            } else {
                                depth++;
                            }
                        } else if (nextTag.startsWith("</")) {
                            depth--;
                            if (depth == 0) {
                                endIndex = k;
                                break;
                            }
                        }
                    }

                    break;
                }
            }
        }

        // 删除activity相关的所有entry
        if (startIndex != -1 && endIndex != -1) {
            for (int i = endIndex; i >= startIndex; i--) {
                xmlEntries.remove(i);
            }
        }
    }

    /**
     * 修改Activity的硬件加速属性
     */
    public void setActivityHardwareAccelerated(String activityName, boolean enabled) {
        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<activity") || tag.equals("<activity")) {
                // 检查是否是目标activity
                boolean isTarget = false;
                int activityEndIndex = -1;

                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) {
                        activityEndIndex = j - 1;
                        break;
                    }

                    if ((attrTag.equals("android:name") || attrTag.endsWith("android:name")) &&
                            attrEntry.getValue().equals(activityName)) {
                        isTarget = true;
                    }

                    // 如果找到了hardwareAccelerated属性，直接修改
                    if (isTarget && (attrTag.equals("android:hardwareAccelerated") ||
                            attrTag.endsWith("android:hardwareAccelerated"))) {
                        xmlEntries.set(j, new XMLEntry(
                                attrEntry.getTag(),
                                attrEntry.getMiddleTag(),
                                enabled ? "true" : "false",
                                attrEntry.getEndTag()
                        ));
                        return;
                    }
                }

                // 如果没有找到该属性，添加一个
                if (isTarget && activityEndIndex != -1) {
                    XMLEntry lastAttr = xmlEntries.get(activityEndIndex);
                    String indent = getIndent(lastAttr.getTag());
                    xmlEntries.add(activityEndIndex + 1, new XMLEntry(
                            indent + "android:hardwareAccelerated",
                            "=\"",
                            enabled ? "true" : "false",
                            "\""
                    ));
                    return;
                }
            }
        }
    }

    /**
     * 获取缩进
     */
    private String getIndent(String tag) {
        int spaces = 0;
        for (char c : tag.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else {
                break;
            }
        }
        return " ".repeat(spaces);
    }

    /**
     * 获取修改后的XML条目
     */
    public List<XMLEntry> getModifiedEntries() {
        return xmlEntries;
    }

    // ==================== 启动入口分析 ====================

    /** 启动入口信息 */
    public static class LauncherActivity {
        public String activityName;   // android:name 值
        public boolean hardwareAccelerated; // true=HW加速, false=SW加速
    }

    public static class LauncherEntryInfo {
        public List<LauncherActivity> launcherActivities = new ArrayList<>();
        public boolean hasMultipleEntries; // 是否有多个启动入口

        /** 获取HW加速的启动入口 */
        public LauncherActivity getHwEntry() {
            for (LauncherActivity a : launcherActivities) {
                if (a.hardwareAccelerated) return a;
            }
            return null;
        }

        /** 获取SW加速的启动入口 */
        public LauncherActivity getSwEntry() {
            for (LauncherActivity a : launcherActivities) {
                if (!a.hardwareAccelerated) return a;
            }
            return null;
        }
    }

    /** 分析启动入口：扫描所有包含 LAUNCHER intent-filter 的 Activity */
    public LauncherEntryInfo analyzeLauncherEntries() {
        LauncherEntryInfo info = new LauncherEntryInfo();

        for (int i = 0; i < xmlEntries.size(); i++) {
            XMLEntry entry = xmlEntries.get(i);
            String tag = entry.getTag().trim();

            if (tag.startsWith("<activity") || tag.equals("<activity")) {
                // 获取 activity 的 name 和 hardwareAccelerated 属性
                String activityName = null;
                Boolean hwAccel = null;

                for (int j = i + 1; j < xmlEntries.size(); j++) {
                    XMLEntry attrEntry = xmlEntries.get(j);
                    String attrTag = attrEntry.getTag().trim();

                    if (attrTag.startsWith("<")) break; // 属性区域结束

                    if ((attrTag.equals("android:name") || attrTag.endsWith("android:name"))
                            && attrEntry.getMiddleTag().equals("=\"")) {
                        activityName = attrEntry.getValue();
                    }

                    if ((attrTag.equals("android:hardwareAccelerated") || attrTag.endsWith("android:hardwareAccelerated"))
                            && attrEntry.getMiddleTag().equals("=\"")) {
                        hwAccel = "true".equalsIgnoreCase(attrEntry.getValue());
                    }
                }

                if (activityName == null) continue;

                // 查找此 activity 的结束位置
                int endIndex = findActivityEndIndex(xmlEntries, i);
                if (endIndex < 0) continue;

                // 在 activity 范围内检查是否包含 LAUNCHER intent-filter
                boolean hasLauncherFilter = false;
                for (int k = i; k <= endIndex; k++) {
                    XMLEntry e = xmlEntries.get(k);
                    String val = e.getValue();
                    if (val != null && val.equals("android.intent.category.LAUNCHER")
                            && e.getMiddleTag().equals("=\"")) {
                        hasLauncherFilter = true;
                        break;
                    }
                }

                if (hasLauncherFilter) {
                    LauncherActivity la = new LauncherActivity();
                    la.activityName = activityName;
                    la.hardwareAccelerated = hwAccel != null ? hwAccel : false;
                    info.launcherActivities.add(la);
                }
            }
        }

        info.hasMultipleEntries = info.launcherActivities.size() > 1;
        return info;
    }

    /** 查找指定 activity 的开始位置至结束位置 */
    public static int findActivityEndIndex(List<XMLEntry> entries, int startIndex) {
        int depth = 1;
        for (int k = startIndex + 1; k < entries.size(); k++) {
            XMLEntry e = entries.get(k);
            String tag = e.getTag().trim();

            if (tag.startsWith("<") && !tag.startsWith("</")) {
                // 自闭合标签（mEndTag 含 "/>"）不改变深度
                String endTag = e.getEndTag();
                if (endTag != null && endTag.trim().startsWith("/>")) {
                    // self-closing, depth unchanged
                } else {
                    depth++;
                }
            } else if (tag.startsWith("</")) {
                depth--;
                if (depth == 0) return k;
            }
        }
        return -1;
    }

    /**
     * 打印XML内容（用于调试）
     */
    public String toXmlString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        for (XMLEntry entry : xmlEntries) {
            sb.append(entry.getTag())
                    .append(entry.getMiddleTag())
                    .append(entry.getValue())
                    .append(entry.getEndTag())
                    .append("\n");
        }
        return sb.toString();
    }
}