package awais.instagrabber.adapters.viewholder.directmessages;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutDmActionLogBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemActionLog;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.TextUtils;

public class DirectItemActionLogViewHolder extends DirectItemViewHolder {

    private final LayoutDmActionLogBinding binding;

    public DirectItemActionLogViewHolder(@NonNull final LayoutDmBaseBinding baseBinding,
                                         final LayoutDmActionLogBinding binding,
                                         final User currentUser,
                                         final DirectThread thread,
                                         final MentionClickListener mentionClickListener,
                                         final View.OnClickListener onClickListener) {
        super(baseBinding, currentUser, thread, onClickListener);
        this.binding = binding;
        setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(final DirectItem directItemModel, final MessageDirection messageDirection) {
        final DirectItemActionLog actionLog = directItemModel.getActionLog();
        final String text = actionLog.getDescription();
        final SpannableStringBuilder sb = new SpannableStringBuilder(text);
        final List<DirectItemActionLog.TextRange> bold = actionLog.getBold();
        if (bold != null && !bold.isEmpty()) {
            for (final DirectItemActionLog.TextRange textRange : bold) {
                final StyleSpan boldStyleSpan = new StyleSpan(Typeface.BOLD);
                sb.setSpan(boldStyleSpan, textRange.getStart(), textRange.getEnd(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
        final List<DirectItemActionLog.TextRange> textAttributes = actionLog.getTextAttributes();
        if (textAttributes != null && !textAttributes.isEmpty()) {
            for (final DirectItemActionLog.TextRange textAttribute : textAttributes) {
                if (!TextUtils.isEmpty(textAttribute.getColor())) {
                    final ForegroundColorSpan colorSpan = new ForegroundColorSpan(itemView.getResources().getColor(R.color.deep_orange_400));
                    sb.setSpan(colorSpan, textAttribute.getStart(), textAttribute.getEnd(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (!TextUtils.isEmpty(textAttribute.getIntent())) {
                    final ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull final View widget) {

                        }
                    };
                    sb.setSpan(clickableSpan, textAttribute.getStart(), textAttribute.getEnd(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        }
        binding.tvMessage.setText(sb);
    }

    @Override
    protected boolean allowMessageDirectionGravity() {
        return false;
    }

    @Override
    protected boolean showMessageInfo() {
        return false;
    }
}
