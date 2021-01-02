package awais.instagrabber.adapters.viewholder;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.ProfileModel;

public final class FeedStoryViewHolder extends RecyclerView.ViewHolder {

    private final ItemHighlightBinding binding;

    public FeedStoryViewHolder(final ItemHighlightBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(final FeedStoryModel model,
                     final int position,
                     final FeedStoriesAdapter.OnFeedStoryClickListener listener) {
        if (model == null) return;
        binding.getRoot().setOnClickListener(v -> {
            if (listener == null) return;
            listener.onFeedStoryClick(model, position);
        });
        binding.getRoot().setOnLongClickListener(v -> {
            if (listener != null) listener.onFeedStoryLongClick(model, position);
            return true;
        });
        final ProfileModel profileModel = model.getProfileModel();
        binding.title.setText(profileModel.getUsername());
        binding.title.setAlpha(model.isFullyRead() ? 0.5F : 1.0F);
        binding.icon.setImageURI(profileModel.getSdProfilePic());
        binding.icon.setAlpha(model.isFullyRead() ? 0.5F : 1.0F);
    }
}