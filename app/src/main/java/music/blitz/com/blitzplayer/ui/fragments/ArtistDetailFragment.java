/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package music.blitz.com.blitzplayer.ui.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ListView;
import music.blitz.com.blitzplayer.Config;
import music.blitz.com.blitzplayer.R;
import music.blitz.com.blitzplayer.adapters.ArtistDetailAlbumAdapter;
import music.blitz.com.blitzplayer.adapters.ArtistDetailSongAdapter;
import music.blitz.com.blitzplayer.adapters.PagerAdapter;
import music.blitz.com.blitzplayer.cache.ImageFetcher;
import music.blitz.com.blitzplayer.menu.FragmentMenuItems;
import music.blitz.com.blitzplayer.model.Album;
import music.blitz.com.blitzplayer.model.Artist;
import music.blitz.com.blitzplayer.model.Song;
import music.blitz.com.blitzplayer.utils.AlbumPopupMenuHelper;
import music.blitz.com.blitzplayer.utils.ArtistPopupMenuHelper;
import music.blitz.com.blitzplayer.utils.MusicUtils;
import music.blitz.com.blitzplayer.utils.PopupMenuHelper;
import music.blitz.com.blitzplayer.utils.SongPopupMenuHelper;
import music.blitz.com.blitzplayer.widgets.IPopupMenuCallback;
import music.blitz.com.blitzplayer.widgets.LoadingEmptyContainer;

import java.util.TreeSet;

public class ArtistDetailFragment extends FadingBarFragment implements IChildFragment {
    private final int ALBUM_LOADER_ID = 0;
    private final int SONG_LOADER_ID = 1;

    private long mArtistId;
    private String mArtistName;

    private ImageView mHero;
    private View mHeader;

    private ListView mSongs;
    private ArtistDetailSongAdapter mSongAdapter;

    private RecyclerView mAlbums;
    private ArtistDetailAlbumAdapter mAlbumAdapter;

    private PopupMenuHelper mSongPopupMenuHelper;
    private PopupMenuHelper mAlbumPopupMenuHelper;

    private LoadingEmptyContainer mLoadingEmptyContainer;

    @Override
    protected int getLayoutToInflate() { return R.layout.activity_artist_detail; }

    @Override
    protected String getTitle() {
        return getArguments().getString(Config.ARTIST_NAME);
    }

    protected long getArtistId() {
        return getArguments().getLong(Config.ID);
    }

    @Override
    protected void onViewCreated() {
        super.onViewCreated();

        getContainingActivity().setFragmentPadding(false);

        Bundle arguments = getArguments();
        mArtistName = arguments.getString(Config.ARTIST_NAME);
        mArtistId = arguments.getLong(Config.ID);

        setupPopupMenuHelpers();
        setupSongList();
        setupAlbumList();
        setupHero(mArtistName);

        LoaderManager lm = getLoaderManager();
        lm.initLoader(ALBUM_LOADER_ID, arguments, mAlbumAdapter);
        lm.initLoader(SONG_LOADER_ID, arguments, mSongAdapter);
    }

    @Override // DetailFragment
    protected PopupMenuHelper createActionMenuHelper() {
        return new ArtistPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            public Artist getArtist(int position) {
                return new Artist(mArtistId, mArtistName, 0, 0);
            }
        };
    }

    @Override // DetailFragment
    protected int getShuffleTitleId() { return R.string.menu_shuffle_artist; }

    @Override // DetailFragment
    protected void playShuffled() {
        MusicUtils.playArtist(getActivity(), mArtistId, -1, true);
    }

    private void setupHero(String artistName) {
        mHero = (ImageView)mHeader.findViewById(R.id.hero);
        mHero.setContentDescription(artistName);
        // initiate loading the artist image
        // since the artist image needs to be scaled to the image view bounds, we need to wait till the first layout
        // traversal to be able to get the image view dimensions in the helper method that scales the image
        mHero.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mHero.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mHero.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                ImageFetcher.getInstance(getActivity()).loadArtistImage(mArtistName, mHero, true);
            }
        });
    }

    private void setupAlbumList() {
        mAlbums = (RecyclerView) mHeader.findViewById(R.id.albums);
        mAlbums.setHasFixedSize(true);
        mAlbums.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mAlbumAdapter = new ArtistDetailAlbumAdapter(getActivity());
        mAlbumAdapter.setPopupMenuClickedListener(new IPopupMenuCallback.IListener() {
            @Override
            public void onPopupMenuClicked(View v, int position) {
                mAlbumPopupMenuHelper.showPopupMenu(v, position);
            }
        });
        mAlbums.setAdapter(mAlbumAdapter);
    }

    private void setupSongList() {
        mSongs = (ListView)mRootView.findViewById(R.id.songs);
        mHeader = LayoutInflater.from(getActivity()).
                inflate(R.layout.artist_detail_header, mSongs, false);
        mSongs.addHeaderView(mHeader);
        mSongs.setOnScrollListener(this);
        mSongAdapter = new ArtistDetailSongAdapter(getActivity()) {
            @Override
            protected void onLoading() {
                mLoadingEmptyContainer.showLoading();
            }

            @Override
            protected void onNoResults() {
                // no results - because the user deleted the last item - pop our fragment
                // from the stack
                getContainingActivity().postRemoveFragment(ArtistDetailFragment.this);
            }
        };
        mSongAdapter.setPopupMenuClickedListener(new IPopupMenuCallback.IListener() {
            @Override
            public void onPopupMenuClicked(View v, int position) {
                mSongPopupMenuHelper.showPopupMenu(v, position);
            }
        });
        mSongs.setAdapter(mSongAdapter);
        mSongs.setOnItemClickListener(mSongAdapter);
        mLoadingEmptyContainer =
                (LoadingEmptyContainer)mRootView.findViewById(R.id.loading_empty_container);
        mSongs.setEmptyView(mLoadingEmptyContainer);
    }

    private void setupPopupMenuHelpers() {
        mSongPopupMenuHelper = new SongPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Song getSong(int position) {
                return mSongAdapter.getItem(position);
            }

            @Override
            protected long getSourceId() {
                return getArtistId();
            }

            @Override
            protected Config.IdType getSourceType() {
                return Config.IdType.Artist;
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                // since we are already on the artist page, this item doesn't make sense
                set.remove(FragmentMenuItems.MORE_BY_ARTIST);
            }
        };

        mAlbumPopupMenuHelper = new AlbumPopupMenuHelper(getActivity(), getChildFragmentManager()) {
            @Override
            public Album getAlbum(int position) {
                return mAlbumAdapter.getItem(position);
            }

            @Override
            protected void updateMenuIds(PopupMenuType type, TreeSet<Integer> set) {
                super.updateMenuIds(type, set);

                // since we are already on the artist page, this item doesn't make sense
                set.remove(FragmentMenuItems.MORE_BY_ARTIST);
            }
        };
    }

    // TODO: change this class to use the same header strategy as PlaylistDetail
    protected int getHeaderHeight() { return mHero.getHeight(); }

    protected void setHeaderPosition(float y) {  }

    @Override
    public void restartLoader() {
        Bundle arguments = getArguments();
        LoaderManager lm = getLoaderManager();
        lm.restartLoader(ALBUM_LOADER_ID, arguments, mAlbumAdapter);
        lm.restartLoader(SONG_LOADER_ID, arguments, mSongAdapter);

        ImageFetcher.getInstance(getActivity()).loadArtistImage(mArtistName, mHero, true);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();

        mSongAdapter.setCurrentlyPlayingTrack(MusicUtils.getCurrentTrack());
    }

    @Override
    public PagerAdapter.MusicFragments getMusicFragmentParent() {
        return PagerAdapter.MusicFragments.ARTIST;
    }
}
