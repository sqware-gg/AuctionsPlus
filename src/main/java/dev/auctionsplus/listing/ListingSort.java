package dev.auctionsplus.listing;

public enum ListingSort {
    NEWEST("Newest"),
    PRICE_ASC("Price low"),
    PRICE_DESC("Price high");

    private final String label;

    ListingSort(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ListingSort next() {
        ListingSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
