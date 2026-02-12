package mituran.gglua.tool.model;

public class LuaFunction {
    private String name;
    private String description;
    private String syntax;

    public LuaFunction(String name, String description, String syntax) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSyntax() {
        return syntax;
    }
}