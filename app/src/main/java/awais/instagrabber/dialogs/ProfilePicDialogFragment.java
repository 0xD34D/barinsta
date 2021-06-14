package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;

// import java.io.File;

import awais.instagrabber.R;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.customviews.drawee.DoubleTapGestureListener;
import awais.instagrabber.databinding.DialogProfilepicBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class ProfilePicDialogFragment extends DialogFragment {
    private static final String TAG = "ProfilePicDlgFragment";

    private long id;
    private String name;
    private String fallbackUrl;

    private boolean isLoggedIn;
    private DialogProfilepicBinding binding;
    private String url;

    public static ProfilePicDialogFragment getInstance(final long id, final String name, final String fallbackUrl) {
        final Bundle args = new Bundle();
        args.putLong("id", id);
        args.putString("name", name);
        args.putString("fallbackUrl", fallbackUrl);
        final ProfilePicDialogFragment fragment = new ProfilePicDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ProfilePicDialogFragment() {}

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = DialogProfilepicBinding.inflate(inflater, container, false);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog == null) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setLayout(width, height);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        fetchAvatar();
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            dismiss();
            return;
        }
        id = arguments.getLong("id");
        name = arguments.getString("name");
        fallbackUrl = arguments.getString("fallbackUrl");
        binding.download.setOnClickListener(v -> {
            final Context context = getContext();
            if (context == null) return;
            // if (ContextCompat.checkSelfPermission(context, DownloadUtils.PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
            downloadProfilePicture();
            // return;
            // }
            // requestPermissions(DownloadUtils.PERMS, 8020);
        });
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadProfilePicture();
        }
    }

    private void fetchAvatar() {
        if (isLoggedIn) {
            final UserRepository repository = UserRepository.Companion.getInstance();
            repository.getUserInfo(id, CoroutineUtilsKt.getContinuation((user, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                if (throwable != null) {
                    final Context context = getContext();
                    if (context == null) {
                        dismiss();
                        return;
                    }
                    Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                    return;
                }
                if (user != null) {
                    final String url = user.getHDProfilePicUrl();
                    if (TextUtils.isEmpty(url)) {
                        final Context context = getContext();
                        if (context == null) return;
                        Toast.makeText(context, R.string.no_profile_pic_found, Toast.LENGTH_LONG).show();
                        return;
                    }
                    setupPhoto(url);
                }
            }), Dispatchers.getIO()));
        } else setupPhoto(fallbackUrl);
    }

    private void setupPhoto(final String result) {
        if (TextUtils.isEmpty(result)) url = fallbackUrl;
        else url = result;
        final DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setUri(url)
                .setOldController(binding.imageViewer.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(final String id, final Throwable throwable) {
                        super.onFailure(id, throwable);
                        binding.download.setVisibility(View.GONE);
                        binding.progressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFinalImageSet(final String id,
                                                final ImageInfo imageInfo,
                                                final Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        binding.download.setVisibility(View.VISIBLE);
                        binding.progressView.setVisibility(View.GONE);
                    }
                })
                .build();
        binding.imageViewer.setController(controller);
        final AnimatedZoomableController zoomableController = (AnimatedZoomableController) binding.imageViewer.getZoomableController();
        zoomableController.setMaxScaleFactor(3f);
        zoomableController.setGestureZoomEnabled(true);
        zoomableController.setEnabled(true);
        binding.imageViewer.setZoomingEnabled(true);
        final DoubleTapGestureListener tapListener = new DoubleTapGestureListener(binding.imageViewer);
        binding.imageViewer.setTapListener(tapListener);
    }

    private void downloadProfilePicture() {
        if (url == null) return;
        // final File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        final Context context = getContext();
        if (context == null) return;
        // if (dir.exists() || dir.mkdirs()) {
        //
        // }
        final String fileName = name + '_' + System.currentTimeMillis() + ".jpg";
        // final File saveFile = new File(dir, fileName);
        final DocumentFile downloadDir = DownloadUtils.getDownloadDir();
        final DocumentFile saveFile = downloadDir.createFile(Utils.mimeTypeMap.getMimeTypeFromExtension("jpg"), fileName);
        DownloadUtils.download(context, url, saveFile);
        // return;
        // Toast.makeText(context, R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
    }
}
