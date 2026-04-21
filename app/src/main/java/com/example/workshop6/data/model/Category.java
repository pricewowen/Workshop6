// Contributor(s): Mason
// Main: Mason - Category id and label for browse filters.

package com.example.workshop6.data.model;

/**
 * Product tag row for browse and map filter chips mapped from Workshop 7 catalog tags.
 */
public class Category {
    private int tagId;
    private String tagName;

    /**
     * @param tagId Workshop 7 tag id or synthetic values from map filter chips.
     * @param tagName label shown on the chip.
     */
    public Category(int tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

    /** @return Workshop 7 tag id or synthetic map filter id. */
    public int getTagId() {
        return tagId;
    }

    /** @param tagId Workshop 7 tag id or synthetic map filter id. */
    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    /** @return Chip label text. */
    public String getTagName() {
        return tagName;
    }

    /** @param tagName chip label text. */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
