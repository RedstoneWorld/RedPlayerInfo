package de.redstoneworld.redplayerinfo.bungee;

public class RedGroup implements Comparable<RedGroup> {

    private final String name;
    private final String displayName;
    private final String prefix;
    private final String suffix;
    private final int rank;

    public RedGroup(String name, String displayName, String prefix, String suffix, int rank) {
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.rank = rank;
    }

    public RedGroup() {
        this("", "", "", "", Integer.MIN_VALUE);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRank() {
        return rank;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public int compareTo(RedGroup o) {
        return Integer.compare(getRank(), o.getRank());
    }
}
