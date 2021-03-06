package com.softdesign.devintensive.ui.activities;

import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.softdesign.devintensive.R;
import com.softdesign.devintensive.data.managers.DataManager;
import com.softdesign.devintensive.data.network.response.ImageUploadedResponse;
import com.softdesign.devintensive.data.network.restmodels.User;
import com.softdesign.devintensive.ui.dialogs.ChangeImageDialog;
import com.softdesign.devintensive.ui.dialogs.NeedGrantPermissionDialog;
import com.softdesign.devintensive.ui.fragments.ProfileFragment;
import com.softdesign.devintensive.ui.fragments.UserListFragment;
import com.softdesign.devintensive.ui.views.transformations.CircleTransformation;
import com.softdesign.devintensive.utils.IOUtils;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.softdesign.devintensive.data.network.restmodels.User.createEmptyUser;
import static com.softdesign.devintensive.utils.Constants.DIALOG_FRAGMENT_TAG;
import static com.softdesign.devintensive.utils.Constants.LOG_TAG_PREFIX;
import static com.softdesign.devintensive.utils.IOUtils.filePathFromUri;
import static com.softdesign.devintensive.utils.NavUtils.goToAppSettings;
import static com.softdesign.devintensive.utils.NavUtils.goToCameraApp;
import static com.softdesign.devintensive.utils.NavUtils.goToGalleryApp;
import static com.softdesign.devintensive.utils.NetworkStatusChecker.isNetworkAvailable;
import static com.softdesign.devintensive.utils.UIUtils.showToast;
import static com.squareup.picasso.NetworkPolicy.OFFLINE;

/**
 * @author Sergey Vorobyev
 */

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = LOG_TAG_PREFIX + "MainActivity";

    // Activity requests
    public static final int REQUEST_CODE_CAMERA_AVATAR = 0;
    public static final int REQUEST_CODE_GALLERY_AVATAR = 1;
    public static final int REQUEST_CODE_CAMERA_USER_PHOTO = 2;
    public static final int REQUEST_CODE_GALLERY_USER_PHOTO = 3;
    public static final int REQUEST_CODE_SETTING_CAMERA_AVATAR = 4;
    public static final int REQUEST_CODE_SETTING_GALLERY_AVATAR = 5;
    public static final int REQUEST_CODE_SETTING_CAMERA_USER_PHOTO = 6;
    public static final int REQUEST_CODE_SETTING_GALLERY_USER_PHOTO = 7;

    // Permissions requests
    public static final int AVATAR_CAMERA_PERMISSION_REQUEST_CODE = 0;
    public static final int AVATAR_GALLERY_PERMISSION_REQUEST_CODE = 1;
    public static final int USER_PHOTO_CAMERA_PERMISSION_REQUEST_CODE = 2;
    public static final int USER_PHOTO_GALLERY_PERMISSION_REQUEST_CODE = 3;

    @BindView(R.id.navigation_view) NavigationView mNavigationView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    private Unbinder mUnbinder;
    private ImageView mAvatar;

    private DataManager mDataManager;
    private File mImageFile;
    private Uri mSelectedAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mUnbinder = ButterKnife.bind(this);

        mAvatar = (ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.avatar);
        mAvatar.setOnClickListener(v -> showChangeAvatarDialog());

        mDataManager = DataManager.getInstance();

        if (savedInstanceState == null) {
            setupNavigationView();
        }
        setupDrawer();

        User user = mDataManager.getPreferencesManager().loadUser();
        if (user != null) {
            String avatar = user.getPublicInfo().getAvatar();
            mSelectedAvatar = (avatar != null) ? Uri.parse(avatar) : Uri.parse("");
            loadImageAvatar(mSelectedAvatar);
        }
    }

    @Override
    protected void onDestroy() {
        mUnbinder.unbind();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) mDrawerLayout.openDrawer(GravityCompat.START);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_GALLERY_AVATAR:
                if (resultCode == RESULT_OK && data != null) mSelectedAvatar = data.getData();
                loadImageAvatar(mSelectedAvatar);
                uploadAvatar();
                break;
            case REQUEST_CODE_CAMERA_AVATAR:
                if (resultCode == RESULT_OK && mImageFile != null) mSelectedAvatar = Uri.fromFile(mImageFile);
                loadImageAvatar(mSelectedAvatar);
                uploadAvatar();
                break;
            case REQUEST_CODE_SETTING_CAMERA_AVATAR:
                checkAndRequestCameraPermission();
                break;
            case REQUEST_CODE_SETTING_GALLERY_AVATAR:
                checkAndRequestGalleryPermission();
                break;
        }

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AVATAR_CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
                    onCameraPermissionGranted();
                } else {
                    showNeedGrantPermissionDialog(R.string.dialog_message_need_grant_camera_permission,
                            (dialog, which) -> checkAndRequestCameraPermission(),
                            (dialog, which) -> dialog.dismiss(),
                            (dialog, which) -> goToAppSettings(this, REQUEST_CODE_SETTING_CAMERA_AVATAR));
                }
                break;
            case AVATAR_GALLERY_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PERMISSION_GRANTED) {
                    onGalleryPermissionGranted();
                } else {
                    showNeedGrantPermissionDialog(R.string.dialog_message_need_grant_gallery_permission,
                            (dialog, which) -> checkAndRequestGalleryPermission(),
                            (dialog, which) -> dialog.dismiss(),
                            (dialog, which) -> goToAppSettings(this, REQUEST_CODE_SETTING_GALLERY_AVATAR));
                }
                break;
        }

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null) fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Check if camera permissions not granted make request permissions
     */
    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {CAMERA, WRITE_EXTERNAL_STORAGE},
                    AVATAR_CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Check if gallery permissions not granted make request permissions
     */
    private void checkAndRequestGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            onGalleryPermissionGranted();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {READ_EXTERNAL_STORAGE},
                    AVATAR_GALLERY_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Call when camera permissions have been granted
     */
    private void onCameraPermissionGranted() {
        try {
            mImageFile = IOUtils.createImageFile(this);
        } catch (IOException e) {
            showError(getString(R.string.error_creation_file), e);
            showToast(this, getString(R.string.error_toast_creation_file));
        }
        goToCameraApp(this, mImageFile, REQUEST_CODE_CAMERA_AVATAR);
    }

    /**
     * Call when gallery permissions have been granted
     */
    private void onGalleryPermissionGranted() {
        goToGalleryApp(this, REQUEST_CODE_GALLERY_AVATAR);
    }

    private void setupNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.setCheckedItem(R.id.profile_item);
        onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
    }

    /**
     * Setup navigation drawer
     */
    private void setupDrawer() {
        mNavigationView.setNavigationItemSelectedListener(this);

        User user = mDataManager.getPreferencesManager().loadUser();

        TextView username = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.username);
        TextView email = (TextView) mNavigationView.getHeaderView(0).findViewById(R.id.email);

        username.setText(user.getFirstName() + " " + user.getSecondName());
        email.setText(user.getContacts().getEmail());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (id) {
            case R.id.profile_item:
                ProfileFragment profileFragment = (ProfileFragment) fragmentManager
                        .findFragmentByTag(ProfileFragment.FRAGMENT_TAG);
                if (profileFragment == null) profileFragment = new ProfileFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container_main, profileFragment, ProfileFragment.FRAGMENT_TAG)
                        .commit();
                break;
            case R.id.team_item:
                UserListFragment userListFragment = (UserListFragment) fragmentManager
                        .findFragmentByTag(UserListFragment.FRAGMENT_TAG);
                if (userListFragment == null) userListFragment = new UserListFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.container_main, userListFragment, UserListFragment.FRAGMENT_TAG)
                        .commit();
                break;
        }

        mNavigationView.setCheckedItem(id);
        mDrawerLayout.closeDrawer(GravityCompat.START);

        return false;
    }

    /**
     * Show {@link ChangeImageDialog}. Called when we want change avatar
     */
    private void showChangeAvatarDialog() {
        ChangeImageDialog d = new ChangeImageDialog();
        d.setOnClickListener((dialog, which) -> {
            switch (which) {
                case 0:
                    checkAndRequestCameraPermission();
                    break;
                case 1:
                    checkAndRequestGalleryPermission();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        d.show(getSupportFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    /**
     * Show {@link NeedGrantPermissionDialog} to explain to users why needed permissions.
     * @param message Message for users
     * @param onPositiveButtonClickListener Actions to handle press on positive button
     * @param onNegativeButtonClickListener Actions to handle press on negative button
     */
    private void showNeedGrantPermissionDialog(int message,
                                               OnClickListener onPositiveButtonClickListener,
                                               OnClickListener onNegativeButtonClickListener,
                                               OnClickListener onNeutralButtonClickListener) {

        NeedGrantPermissionDialog d = NeedGrantPermissionDialog.newInstance(message);
        d.setOnPositiveButtonClickListener(onPositiveButtonClickListener);
        d.setOnNegativeButtonClickListener(onNegativeButtonClickListener);
        d.setOnNeutralButtonClickListener(onNeutralButtonClickListener);
        d.show(getSupportFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    /**
     * Load image from server or cache or disc to {@link #mAvatar} by Picasso library
     * @param uri Object {@link Uri} of image
     */
    private void loadImageAvatar(Uri uri) {

        DataManager.getInstance().getPicasso()
                .load(uri)
                .placeholder(R.drawable.user_bg)
                .resizeDimen(R.dimen.size_avatar, R.dimen.size_avatar)
                .onlyScaleDown()
                .centerCrop()
                .transform(new CircleTransformation())
                .networkPolicy(OFFLINE)
                .into(mAvatar, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "avatar loaded from cache");
                    }

                    @Override
                    public void onError() {
                        DataManager.getInstance().getPicasso()
                                .load(uri)
                                .placeholder(R.drawable.user_bg)
                                .resizeDimen(R.dimen.size_avatar, R.dimen.size_avatar)
                                .onlyScaleDown()
                                .centerCrop()
                                .transform(new CircleTransformation())
                                .into(mAvatar, new com.squareup.picasso.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "avatar loaded from server");
                                    }

                                    @Override
                                    public void onError() {
                                        Log.d(TAG, "Can't load avatar from server");
                                    }
                                });
                    }
                });
    }

    /**
     * Upload avatar to server
     */
    private void uploadAvatar() {
        if (mSelectedAvatar != null) {
            if (!isNetworkAvailable(this)) {
                showToast(this, getString(R.string.error_no_connection));
                return;
            }
            File file = new File(filePathFromUri(mSelectedAvatar));

            User user = mDataManager.getPreferencesManager().loadUser();
            if (user == null) user = createEmptyUser();
            final User finalUser = user;

            showProgress();
            mDataManager.uploadAvatar(file).enqueue(new Callback<ImageUploadedResponse>() {
                @Override
                public void onResponse(Call<ImageUploadedResponse> call, Response<ImageUploadedResponse> response) {
                    hideProgress();
                    if (response.isSuccessful()) {
                        finalUser.getPublicInfo().setAvatar(response.body().getData().getPhoto());
                        finalUser.getPublicInfo().setUpdated(response.body().getData().getUpdated());
                        mDataManager.getPreferencesManager().saveUser(finalUser);
                    }
                }

                @Override
                public void onFailure(Call<ImageUploadedResponse> call, Throwable t) {
                    hideProgress();
                    showError(getString(R.string.error_upload_image), t);
                }
            });
        }
    }
}
