package com.sotaynauan.ai.data.model;

public class VoiceCommand {
    private final String label;
    private final String commandText;

    public VoiceCommand(String label, String commandText) {
        this.label = label;
        this.commandText = commandText;
    }

    public String getLabel() {
        return label;
    }

    public String getCommandText() {
        return commandText;
    }
}
