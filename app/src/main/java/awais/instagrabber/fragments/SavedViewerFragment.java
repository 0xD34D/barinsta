package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.SavedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentSavedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class SavedViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = SavedViewerFragment.class.getSimpleName();

    private FragmentSavedBinding binding;
    private String username;
    private long profileId;
    private ActionMode actionMode;
    private SwipeRefreshLayout root;
    private AppCompatActivity fragmentActivity;
    private boolean isLoggedIn, shouldRefresh = true;
    private PostItemType type;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(final ActionMode mode) {
                    binding.posts.endSelection();
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (SavedViewerFragment.this.selectedFeedModels == null) return false;
                        final Context context = getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(SavedViewerFragment.this.selectedFeedModels));
                        binding.posts.endSelection();
                    }
                    return false;
                }
            });
    private final FeedAdapterV2.FeedItemCallback feedItemCallback = new FeedAdapterV2.FeedItemCallback() {
        @Override
        public void onPostClick(final Media feedModel) {
            openPostDialog(feedModel, -1);
        }

        @Override
        public void onSliderClick(final Media feedModel, final int position) {
            openPostDialog(feedModel, position);
        }

        @Override
        public void onCommentsClick(final Media feedModel) {
            final User user = feedModel.getUser();
            if (user == null) return;
            try {
                final NavDirections commentsAction = ProfileFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        user.getPk()
                );
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(commentsAction);
            } catch (Exception e) {
                Log.e(TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(final Media feedModel, final int childPosition, final View popupLocation) {
            final Context context = getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            try {
                final NavDirections action = ProfileFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(final Media feedModel) {
            final Location location = feedModel.getLocation();
            if (location == null) return;
            try {
                final NavDirections action = ProfileFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(final String mention) {
            navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(final Media feedModel) {
            navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onProfilePicClick(final Media feedModel) {
            final User user = feedModel.getUser();
            if (user == null) return;
            navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(final String url) {
            Utils.openURL(getContext(), url);
        }

        @Override
        public void onEmailClick(final String emailId) {
            Utils.openEmailAddress(getContext(), emailId);
        }

        private void openPostDialog(final Media feedModel, final int position) {
            try {
                final NavDirections action = SavedViewerFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "openPostDialog: ", e);
            }
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!onBackPressedCallback.isEnabled()) {
                final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
                onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            }
            if (actionMode == null) {
                actionMode = fragmentActivity.startActionMode(multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(final Set<Media> selectedFeedModels) {
            final String title = getString(R.string.number_selected, selectedFeedModels.size());
            if (actionMode != null) {
                actionMode.setTitle(title);
            }
            SavedViewerFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (onBackPressedCallback.isEnabled()) {
                onBackPressedCallback.setEnabled(false);
                onBackPressedCallback.remove();
            }
            if (actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentSavedBinding.inflate(getLayoutInflater(), container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        init();
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.saved_viewer_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.layout) {
            showPostsLayoutPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle();
    }

    @Override
    public void onRefresh() {
        binding.posts.refresh();
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final SavedViewerFragmentArgs fragmentArgs = SavedViewerFragmentArgs.fromBundle(arguments);
        username = fragmentArgs.getUsername();
        profileId = fragmentArgs.getProfileId();
        type = fragmentArgs.getType();
        layoutPreferences = Utils.getPostsLayoutPreferences(getPostsLayoutPreferenceKey());
        setupPosts();
    }

    private void setupPosts() {
        binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new SavedPostFetchService(profileId, type, isLoggedIn, null))
                     .setLayoutPreferences(layoutPreferences)
                     .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                     .setFeedItemCallback(feedItemCallback)
                     .setSelectionModeCallback(selectionModeCallback)
                     .init();
        binding.swipeRefreshLayout.setRefreshing(true);
    }

    @NonNull
    private String getPostsLayoutPreferenceKey() {
        switch (type) {
            case LIKED:
                return Constants.PREF_LIKED_POSTS_LAYOUT;
            case TAGGED:
                return Constants.PREF_TAGGED_POSTS_LAYOUT;
            case SAVED:
            default:
                return Constants.PREF_SAVED_POSTS_LAYOUT;
        }
    }

    private void setTitle() {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        final int titleRes;
        switch (type) {
            case LIKED:
                titleRes = R.string.liked;
                break;
            case TAGGED:
                titleRes = R.string.tagged;
                break;
            default:
            case SAVED:
                titleRes = R.string.saved;
                break;
        }
        actionBar.setTitle(titleRes);
        actionBar.setSubtitle(username);
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() ->
                binding.swipeRefreshLayout.setRefreshing(binding.posts.isFetching())
        );
    }

    private void navigateToProfile(final String username) {
        try {
            final NavDirections action = SavedViewerFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (Exception e) {
            Log.e(TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        final PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                getPostsLayoutPreferenceKey(),
                preferences -> {
                    layoutPreferences = preferences;
                    new Handler().postDelayed(() -> binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
    }
}