package com.example.xyzreader.ui.fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.util.PaletteTransformation;
import com.makeramen.roundedimageview.RoundedImageView;
import com.makeramen.roundedimageview.RoundedTransformationBuilder;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by curtis on 1/18/16.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String TAG = "ArticleDetailFragment";
    public static final String ARTICLE_ID = "article_id";

    @Bind(R.id.photo)
    ImageView mPhotoView;
    @Bind(R.id.meta_bar)
    LinearLayout mMetaLayout;
    @Bind(R.id.article_title)
    TextView mTitle;
    @Bind(R.id.article_byline)
    TextView mSubTitle;
    @Bind(R.id.body)
    TextView mBody;
    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbar;
    @Bind(R.id.detail_toolbar)
    Toolbar mToolBar;
    @Bind(R.id.fab)
    FloatingActionButton mFab;
    @Bind(R.id.photo_placeholder)
    ImageView mThumbPlaceHolder;

    private RoundedImageView mThumb;
    private Long mArticleId;
    private Cursor mCursor;
    private int mMutedColor;

    //Custom transformation for the photo thumb
    private Transformation mThumbTransformation = new RoundedTransformationBuilder()
            .borderColor(Color.TRANSPARENT)
            .borderWidthDp(1)
            .cornerRadiusDp(3)
            .oval(false)
            .build();

    public static Fragment getInstance(long articleId) {
        ArticleDetailFragment frag = new ArticleDetailFragment();
        Bundle bnd = new Bundle();
        bnd.putLong(ARTICLE_ID, articleId);
        frag.setArguments(bnd);
        return frag;
    }

    public ArticleDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Toolbar stuff.
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolBar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_detail,container,false);
        mArticleId = getArguments().getLong(ARTICLE_ID);
        ButterKnife.bind(this, rootView);
        mThumb = (RoundedImageView) rootView.findViewById(R.id.thumbnail);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AnimatedVectorDrawable anim = (AnimatedVectorDrawable) getActivity().getDrawable(R.drawable.fab_animated_vector);
                    mFab.setImageDrawable(anim);
                    if (anim != null)
                        anim.start();
                }
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        inflateViews(rootView);
        return rootView;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mArticleId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        inflateViews(getView());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
        inflateViews(getView());
    }

    private void inflateViews(View root) {
        if (root == null || mCursor == null)
            return;
        mTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        mSubTitle.setText(Html.fromHtml(
                DateUtils.getRelativeTimeSpanString(
                        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>"));
        mBody.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
        loadPicassoImage(mCursor.getString(ArticleLoader.Query.PHOTO_URL), mCursor.getString(ArticleLoader.Query.THUMB_URL));
    }

    private void loadPicassoImage(String pUrl, String thumbUrl) {
        Picasso pic = Picasso.with(getActivity());
        pic.setIndicatorsEnabled(true);
        pic.load(pUrl)
                .transform(PaletteTransformation.instance())
                .into(mPhotoView);
        pic.load(thumbUrl)
                .transform(PaletteTransformation.instance())
                .into(mThumbPlaceHolder, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap bitmap = ((BitmapDrawable) mThumbPlaceHolder.getDrawable()).getBitmap();
                        Palette p = PaletteTransformation.getPalette(bitmap);
                        Palette.Swatch swatch = p.getVibrantSwatch();
                        if (swatch == null)
                            return;
                        setColorTinting(swatch);
                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "Error fetching image");
                    }
                });
        if(mThumb != null) {
            pic.load(thumbUrl)
                    .transform(mThumbTransformation)
                    .into(mThumb);
            mThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.addToBackStack(null);
                    ExplodedImageFragment frag = ExplodedImageFragment.instance(mCursor.getString(ArticleLoader.Query.PHOTO_URL));
                    frag.show(ft, "exploded_image");
                }
            });
        }
    }

    private void setColorTinting(Palette.Swatch swatch) {
        Log.d(TAG, "swatch: " + swatch);
        mMutedColor = swatch.getRgb();
        mMetaLayout.setBackgroundColor(mMutedColor);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
             &&getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mCollapsingToolbar.setContentScrimColor(mMutedColor);
        }
        mFab.setBackgroundTintList(ColorStateList.valueOf(swatch.getRgb()));
    }

    public static class ExplodedImageFragment extends DialogFragment {
        public static final String IMAGE_URL = "image_url";

        @Bind(R.id.exploded_imageview)
        ImageView mImageView;

        public static ExplodedImageFragment instance(String photoUrl) {
            ExplodedImageFragment instance = new ExplodedImageFragment();
            Bundle bnd = new Bundle();
            bnd.putString(IMAGE_URL, photoUrl);
            instance.setArguments(bnd);
            return instance;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.exploded_image, container, false);
            ButterKnife.bind(this, view);
            Picasso pic = Picasso.with(getActivity());
            pic.setIndicatorsEnabled(true);
            pic.load(getArguments().getString(IMAGE_URL))
                    .into(mImageView);
            return view;
        }
    }
}
