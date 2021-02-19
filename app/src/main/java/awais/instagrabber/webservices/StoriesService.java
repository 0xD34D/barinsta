package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.repositories.StoriesRepository;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.StoryStickerResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StoriesService extends BaseService {
    private static final String TAG = "StoriesService";

    private final StoriesRepository repository;

    private static StoriesService instance;

    private StoriesService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(StoriesRepository.class);
    }

    public static StoriesService getInstance() {
        if (instance == null) {
            instance = new StoriesService();
        }
        return instance;
    }

    public void fetch(final long mediaId,
                      final ServiceCallback<StoryModel> callback) {
        final Call<String> request = repository.fetch(mediaId);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call,
                                   @NonNull final Response<String> response) {
                if (callback == null) return;
                final String body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject itemJson = new JSONObject(body).getJSONArray("items").getJSONObject(0);
                    callback.onSuccess(ResponseBodyUtils.parseStoryItem(itemJson, false, false, null));
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void getFeedStories(final ServiceCallback<List<FeedStoryModel>> callback) {
        final Call<String> response = repository.getFeedStories();
        response.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "getFeedStories: body is empty");
                    return;
                }
                parseStoriesBody(body, callback);
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private void parseStoriesBody(final String body, final ServiceCallback<List<FeedStoryModel>> callback) {
        try {
            final List<FeedStoryModel> feedStoryModels = new ArrayList<>();
            final JSONArray feedStoriesReel = new JSONObject(body).getJSONArray("tray");
            for (int i = 0; i < feedStoriesReel.length(); ++i) {
                final JSONObject node = feedStoriesReel.getJSONObject(i);
                final JSONObject userJson = node.getJSONObject(node.has("user") ? "user" : "owner");
                try {
                    final User user = new User(userJson.getLong("pk"),
                                               userJson.getString("username"),
                                               userJson.optString("full_name"),
                                               userJson.optBoolean("is_private"),
                                               userJson.getString("profile_pic_url"),
                                               null,
                                               new FriendshipStatus(
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false,
                                                       false
                                               ),
                                               userJson.optBoolean("is_verified"),
                                               false,
                                               false,
                                               false,
                                               false,
                                               null,
                                               null,
                                               0,
                                               0,
                                               0,
                                               0,
                                               null,
                                               null,
                                               0,
                                               null,
                                               null
                    );
                    final String id = node.getString("id");
                    final long timestamp = node.getLong("latest_reel_media");
                    final int mediaCount = node.getInt("media_count");
                    final boolean fullyRead = !node.isNull("seen") && node.getLong("seen") == timestamp;
                    final JSONObject itemJson = node.has("items") ? node.getJSONArray("items").optJSONObject(0) : null;
                    final boolean isBestie = node.optBoolean("has_besties_media", false);
                    StoryModel firstStoryModel = null;
                    if (itemJson != null) {
                        firstStoryModel = ResponseBodyUtils.parseStoryItem(itemJson, false, false, null);
                    }
                    feedStoryModels.add(new FeedStoryModel(id, user, fullyRead, timestamp, firstStoryModel, mediaCount, false, isBestie));
                }
                catch (Exception e) {} // to cover promotional reels with non-long user pk's
            }
            final JSONArray broadcasts = new JSONObject(body).getJSONArray("broadcasts");
            for (int i = 0; i < broadcasts.length(); ++i) {
                final JSONObject node = broadcasts.getJSONObject(i);
                final JSONObject userJson = node.getJSONObject("broadcast_owner");
                // final ProfileModel profileModel = new ProfileModel(false, false, false,
                //         userJson.getString("pk"),
                //         userJson.getString("username"),
                //         null, null, null,
                //         userJson.getString("profile_pic_url"),
                //         null, 0, 0, 0, false, false, false, false, false);
                final User user = new User(userJson.getLong("pk"),
                                           userJson.getString("username"),
                                           userJson.optString("full_name"),
                                           userJson.optBoolean("is_private"),
                                           userJson.getString("profile_pic_url"),
                                           null,
                                           new FriendshipStatus(
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false,
                                                   false
                                           ),
                                           userJson.optBoolean("is_verified"),
                                           false,
                                           false,
                                           false,
                                           false,
                                           null,
                                           null,
                                           0,
                                           0,
                                           0,
                                           0,
                                           null,
                                           null,
                                           0,
                                           null,
                                           null
                );
                final String id = node.getString("id");
                final long timestamp = node.getLong("published_time");
                // final JSONObject itemJson = node.has("items") ? node.getJSONArray("items").getJSONObject(0) : null;
                final StoryModel firstStoryModel = ResponseBodyUtils.parseBroadcastItem(node);
                // if (itemJson != null) {
                // }
                feedStoryModels.add(new FeedStoryModel(id, user, false, timestamp, firstStoryModel, 1, true, false));
            }
            callback.onSuccess(sort(feedStoryModels));
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing json", e);
        }
    }

    public void fetchHighlights(final long profileId,
                                final ServiceCallback<List<HighlightModel>> callback) {
        final Call<String> request = repository.fetchHighlights(profileId);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {
                        callback.onSuccess(null);
                        return;
                    }
                    final JSONArray highlightsReel = new JSONObject(body).getJSONArray("tray");

                    final int length = highlightsReel.length();
                    final List<HighlightModel> highlightModels = new ArrayList<>();

                    for (int i = 0; i < length; ++i) {
                        final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                        highlightModels.add(new HighlightModel(
                                highlightNode.getString("title"),
                                highlightNode.getString(Constants.EXTRAS_ID),
                                highlightNode.getJSONObject("cover_media")
                                             .getJSONObject("cropped_image_version")
                                             .getString("url"),
                                highlightNode.getLong("latest_reel_media"),
                                highlightNode.getInt("media_count")
                        ));
                    }
                    callback.onSuccess(highlightModels);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void fetchArchive(final String maxId,
                             final ServiceCallback<ArchiveFetchResponse> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("include_suggested_highlights", "false");
        form.put("is_in_archive_home", "true");
        form.put("include_cover", "1");
        if (!TextUtils.isEmpty(maxId)) {
            form.put("max_id", maxId); // NOT TESTED
        }
        final Call<String> request = repository.fetchArchive(form);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {
                        callback.onSuccess(null);
                        return;
                    }
                    final JSONObject data = new JSONObject(body);
                    final JSONArray highlightsReel = data.getJSONArray("items");

                    final int length = highlightsReel.length();
                    final List<HighlightModel> highlightModels = new ArrayList<>();

                    for (int i = 0; i < length; ++i) {
                        final JSONObject highlightNode = highlightsReel.getJSONObject(i);
                        highlightModels.add(new HighlightModel(
                                null,
                                highlightNode.getString(Constants.EXTRAS_ID),
                                highlightNode.getJSONObject("cover_image_version").getString("url"),
                                highlightNode.getLong("latest_reel_media"),
                                highlightNode.getInt("media_count")
                        ));
                    }
                    callback.onSuccess(new ArchiveFetchResponse(highlightModels,
                                                                data.getBoolean("more_available"),
                                                                data.getString("max_id")));
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void getUserStory(final StoryViewerOptions options,
                             final ServiceCallback<List<StoryModel>> callback) {
        final String url = buildUrl(options);
        final Call<String> userStoryCall = repository.getUserStory(url);
        final boolean isLoc = options.getType() == StoryViewerOptions.Type.LOCATION;
        final boolean isHashtag = options.getType() == StoryViewerOptions.Type.HASHTAG;
        final boolean isHighlight = options.getType() == StoryViewerOptions.Type.HIGHLIGHT;
        userStoryCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                JSONObject data;
                try {
                    final String body = response.body();
                    if (body == null) {
                        Log.e(TAG, "body is null");
                        return;
                    }
                    data = new JSONObject(body);

                    if (!isHighlight) {
                        data = data.optJSONObject((isLoc || isHashtag) ? "story" : "reel");
                    } else {
                        data = data.getJSONObject("reels").optJSONObject(options.getName());
                    }

                    String username = null;
                    if (data != null
                            // && localUsername == null
                            && !isLoc
                            && !isHashtag) {
                        username = data.getJSONObject("user").getString("username");
                    }

                    JSONArray media;
                    if (data != null
                            && (media = data.optJSONArray("items")) != null
                            && media.length() > 0 && media.optJSONObject(0) != null) {

                        final int mediaLen = media.length();
                        final List<StoryModel> models = new ArrayList<>();
                        for (int i = 0; i < mediaLen; ++i) {
                            data = media.getJSONObject(i);
                            models.add(ResponseBodyUtils.parseStoryItem(data, isLoc, isHashtag, username));
                        }
                        callback.onSuccess(models);
                    } else {
                        callback.onSuccess(null);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing string");
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private void respondToSticker(final String storyId,
                                  final String stickerId,
                                  final String action,
                                  final String arg1,
                                  final String arg2,
                                  final long userId,
                                  final String csrfToken,
                                  final ServiceCallback<StoryStickerResponse> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("mutation_token", UUID.randomUUID().toString());
        form.put("client_context", UUID.randomUUID().toString());
        form.put("radio_type", "wifi-none");
        form.put(arg1, arg2);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<StoryStickerResponse> request =
                repository.respondToSticker(storyId, stickerId, action, signedForm);
        request.enqueue(new Callback<StoryStickerResponse>() {
            @Override
            public void onResponse(@NonNull final Call<StoryStickerResponse> call,
                                   @NonNull final Response<StoryStickerResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<StoryStickerResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    // RespondAction.java
    public void respondToQuestion(final String storyId,
                                  final String stickerId,
                                  final String answer,
                                  final long userId,
                                  final String csrfToken,
                                  final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_question_response", "response", answer, userId, csrfToken, callback);
    }

    // QuizAction.java
    public void respondToQuiz(final String storyId,
                              final String stickerId,
                              final int answer,
                              final long userId,
                              final String csrfToken,
                              final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_quiz_answer", "answer", String.valueOf(answer), userId, csrfToken, callback);
    }

    // VoteAction.java
    public void respondToPoll(final String storyId,
                              final String stickerId,
                              final int answer,
                              final long userId,
                              final String csrfToken,
                              final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_poll_vote", "vote", String.valueOf(answer), userId, csrfToken, callback);
    }

    public void respondToSlider(final String storyId,
                                final String stickerId,
                                final double answer,
                                final long userId,
                                final String csrfToken,
                                final ServiceCallback<StoryStickerResponse> callback) {
        respondToSticker(storyId, stickerId, "story_slider_vote", "vote", String.valueOf(answer), userId, csrfToken, callback);
    }

    @Nullable
    private String buildUrl(@NonNull final StoryViewerOptions options) {
        final StringBuilder builder = new StringBuilder();
        builder.append("https://i.instagram.com/api/v1/");
        final StoryViewerOptions.Type type = options.getType();
        String id = null;
        switch (type) {
            case HASHTAG:
                builder.append("tags/");
                id = options.getName();
                break;
            case LOCATION:
                builder.append("locations/");
                id = String.valueOf(options.getId());
                break;
            case USER:
                builder.append("feed/user/");
                id = String.valueOf(options.getId());
                break;
            case HIGHLIGHT:
                builder.append("feed/reels_media/?user_ids=");
                id = options.getName();
                break;
            case STORY:
                break;
            // case FEED_STORY_POSITION:
            //     break;
            // case STORY_ARCHIVE:
            //     break;
        }
        if (id == null) {
            return null;
        }
        final String userId = id.replace(":", "%3A");
        builder.append(userId);
        if (type != StoryViewerOptions.Type.HIGHLIGHT) {
            builder.append("/story/");
        }
        return builder.toString();
    }

    private List<FeedStoryModel> sort(final List<FeedStoryModel> list) {
        final List<FeedStoryModel> listCopy = new ArrayList<>(list);
        Collections.sort(listCopy, (o1, o2) -> {
            int result;
            switch (Utils.settingsHelper.getString(Constants.STORY_SORT)) {
                case "1":
                    result = Long.compare(o2.getTimestamp(), o1.getTimestamp());
                    break;
                case "2":
                    result = Long.compare(o1.getTimestamp(), o2.getTimestamp());
                    break;
                default:
                    result = 0;
            }
            return result;
        });
        return listCopy;
    }

    public static class ArchiveFetchResponse {
        private final List<HighlightModel> archives;
        private final boolean hasNextPage;
        private final String nextCursor;

        public ArchiveFetchResponse(final List<HighlightModel> archives, final boolean hasNextPage, final String nextCursor) {
            this.archives = archives;
            this.hasNextPage = hasNextPage;
            this.nextCursor = nextCursor;
        }

        public List<HighlightModel> getResult() {
            return archives;
        }

        public boolean hasNextPage() {
            return hasNextPage;
        }

        public String getNextCursor() {
            return nextCursor;
        }
    }
}
