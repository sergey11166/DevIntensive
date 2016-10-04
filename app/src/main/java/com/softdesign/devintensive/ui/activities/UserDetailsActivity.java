package com.softdesign.devintensive.ui.activities;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.softdesign.devintensive.R;
import com.softdesign.devintensive.data.dto.UserDTO;
import com.softdesign.devintensive.ui.adapters.RepositoriesAdapter;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.softdesign.devintensive.ui.fragments.UserListFragment.PARCELABLE_USER_KEY;
import static com.softdesign.devintensive.utils.NavUtils.goToUrl;

/**
 * @author Sergey Vorobyev.
 */

public class UserDetailsActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    @BindView(R.id.collapsing_toolbar_layout) CollapsingToolbarLayout mCollapsingToolbarLayout;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.profile_photo) ImageView mUserPhoto;
    @BindView(R.id.rating) TextView mRating;
    @BindView(R.id.code_lines) TextView mCodeLines;
    @BindView(R.id.count_projects) TextView mProjects;
    @BindView(R.id.about) TextView mAbout;
    @BindView(R.id.list_repositories) ListView mRepositories;
    private Unbinder mUnbinder;

    private UserDTO mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        mUnbinder = ButterKnife.bind(this);
        setupToolbar();

        mUser = getIntent().getExtras().getParcelable(PARCELABLE_USER_KEY);
        if (mUser != null) initUI();
    }

    @Override
    protected void onDestroy() {
        mUnbinder.unbind();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        goToUrl(this, mUser.getRepos().get(position));
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initUI() {

        mRepositories.setAdapter(new RepositoriesAdapter(this, mUser.getRepos()));
        mRepositories.setOnItemClickListener(this);
        mCollapsingToolbarLayout.setTitle(mUser.getFullName());
        mRating.setText(String.valueOf(mUser.getRating()));
        mCodeLines.setText(String.valueOf(mUser.getCountCodeLines()));
        mProjects.setText(String.valueOf(mUser.getCountProjects()));
        mAbout.setText(String.valueOf(mUser.getBio()));

        Point photoSize = new Point(getResources().getDisplayMetrics().widthPixels,
                    getResources().getDimensionPixelSize(R.dimen.size_profile_photo_240));
        Picasso.with(this)
                .load(mUser.getPhoto())
                .placeholder(R.drawable.user_bg)
                .resize(photoSize.x, photoSize.y)
                .onlyScaleDown()
                .centerCrop()
                .into(mUserPhoto);
    }
}
