package club.sk1er.mods.hypixelautocomplete.suggestions;

/**
 * Represents a username entry in the suggestion history
 * Stores the username with original casing and the number of occurrences
 */
public class UsernameEntry {
    private String username;
    private int occurrences;

    public UsernameEntry(String username, int occurrences) {
        this.username = username;
        this.occurrences = occurrences;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void incrementOccurrences() {
        this.occurrences++;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }
}
