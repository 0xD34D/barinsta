package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.adapters.viewholder.StoryListViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.utils.TextUtils;

public final class FeedStoriesListAdapter extends ListAdapter<FeedStoryModel, StoryListViewHolder> implements Filterable {
    private final OnFeedStoryClickListener listener;
    private List<FeedStoryModel> list;

    private final Filter filter = new Filter() {
        @Nullable
        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            final boolean isFilterEmpty = TextUtils.isEmpty(filter);
            final String query = isFilterEmpty ? null : filter.toString().toLowerCase();

            for (FeedStoryModel item : list) {
                if (isFilterEmpty) item.setShown(true);
                else item.setShown(item.getProfileModel().getUsername().toLowerCase().contains(query));
            }
            return null;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            submitList(list);
            notifyDataSetChanged();
        }
    };

    private static final DiffUtil.ItemCallback<FeedStoryModel> diffCallback = new DiffUtil.ItemCallback<FeedStoryModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull final FeedStoryModel oldItem, @NonNull final FeedStoryModel newItem) {
            return oldItem.getStoryMediaId().equals(newItem.getStoryMediaId()) && oldItem.isFullyRead().equals(newItem.isFullyRead());
        }
    };

    public FeedStoriesListAdapter(final OnFeedStoryClickListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void submitList(final List<FeedStoryModel> list) {
        super.submitList(list.stream().filter(i -> i.isShown()).collect(Collectors.toList()));
        this.list = list;
    }

    @NonNull
    @Override
    public StoryListViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new StoryListViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final StoryListViewHolder holder, final int position) {
        final FeedStoryModel model = getItem(position);
        holder.bind(model, position, listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(final FeedStoryModel model, final int position);

        void onProfileClick(final String username);
    }
}
