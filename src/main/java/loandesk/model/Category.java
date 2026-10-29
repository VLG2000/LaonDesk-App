package loandesk.model;

/**
 * The four allowed loan categories.
 */
public enum Category {
    TOOLS("Tools"),
    ELECTRONICS("Electronics"),
    OUTDOOR("Outdoor"),
    OTHER("Other");


    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * JavaFX ComboBoxes call toString() to display each item, so returning the
     * label here makes the drop-down show "Tools" instead of "TOOLS".
     */
    @Override
    public String toString() {
        return label;
    }
}
