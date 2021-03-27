package awais.instagrabber.repositories.responses;

import java.io.Serializable;

import awais.instagrabber.models.enums.FollowingType;

public final class Hashtag implements Serializable {
    private final FollowingType following; // 0 false 1 true; not on search results
    private final long mediaCount;
    private final String id;
    private final String name;
    private final String searchResultSubtitle; // shows how many posts there are on search results

    public Hashtag(final String id,
                   final String name,
                   final long mediaCount,
                   final FollowingType following,
                   final String searchResultSubtitle) {
        this.id = id;
        this.name = name;
        this.mediaCount = mediaCount;
        this.following = following;
        this.searchResultSubtitle = searchResultSubtitle;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getMediaCount() {
        return mediaCount;
    }

    public FollowingType getFollowing() {
        return following;
    }

    public String getSubtitle() {
        return searchResultSubtitle;
    }
}