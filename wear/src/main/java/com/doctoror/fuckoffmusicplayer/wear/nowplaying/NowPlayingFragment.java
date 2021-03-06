/*
 * Copyright (C) 2016 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.fuckoffmusicplayer.wear.nowplaying;

import com.doctoror.commons.playback.PlaybackState;
import com.doctoror.commons.util.StringUtils;
import com.doctoror.commons.wear.nano.WearPlaybackData;
import com.doctoror.fuckoffmusicplayer.R;
import com.doctoror.fuckoffmusicplayer.databinding.FragmentNowPlayingBinding;
import com.doctoror.fuckoffmusicplayer.wear.media.MediaHolder;
import com.doctoror.fuckoffmusicplayer.wear.media.eventbus.EventAlbumArt;
import com.doctoror.fuckoffmusicplayer.wear.media.eventbus.EventMedia;
import com.doctoror.fuckoffmusicplayer.wear.media.eventbus.EventPlaybackPosition;
import com.doctoror.fuckoffmusicplayer.wear.media.eventbus.EventPlaybackState;
import com.doctoror.fuckoffmusicplayer.wear.remote.RemoteControl;
import com.doctoror.fuckoffmusicplayer.wear.root.KeyEventFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.app.Fragment;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

/**
 * "Now Playing" screen
 */
public final class NowPlayingFragment extends KeyEventFragment {

    private final NowPlayingFragmentModelPlaybackState mModelPlaybackState
            = new NowPlayingFragmentModelPlaybackState();

    private final NowPlayingFragmentModelViewState mModelViewState
            = new NowPlayingFragmentModelViewState();

    private final NowPlayingFragmentModelMedia mModelMedia
            = new NowPlayingFragmentModelMedia();

    private final RemoteControl mRemoteControl = RemoteControl.getInstance();

    private MediaHolder mMediaHolder;

    private volatile boolean mSeekBarTracking;

    private View mBtnPrev;
    private View mBtnNext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModelViewState.setBtnPlayRes(R.drawable.ic_play_arrow_white_48dp);
        mMediaHolder = MediaHolder.getInstance(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final FragmentNowPlayingBinding binding = DataBindingUtil
                .inflate(inflater, R.layout.fragment_now_playing, container, false);
        binding.setPlaybackState(mModelPlaybackState);
        binding.setViewState(mModelViewState);
        binding.setMedia(mModelMedia);
        binding.seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListenerImpl());
        binding.btnPrev.setOnClickListener(v -> mRemoteControl.prev());
        binding.btnNext.setOnClickListener(v -> mRemoteControl.next());
        binding.btnPlayPause.setOnClickListener(v -> mRemoteControl.playPause());
        mBtnPrev = binding.btnPrev;
        mBtnNext = binding.btnNext;
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindArt(mMediaHolder.getAlbumArt());
        bindMedia(mMediaHolder.getMedia());
        bindPlaybackState(mMediaHolder.getPlaybackState());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventMedia(@NonNull final EventMedia event) {
        bindMedia(event.media);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventAlbumArt(@NonNull final EventAlbumArt event) {
        bindArt(event.albumArt);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventPlaybackState(@NonNull final EventPlaybackState event) {
        bindPlaybackState(event.playbackState);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventPlaybackPosition(@NonNull final EventPlaybackPosition event) {
        bindPlaybackPosition(event.playbackPosition);
    }

    private void bindMedia(@Nullable final WearPlaybackData.Media media) {
        if (media != null) {
            mModelMedia.setArtistAndAlbum(StringUtils.formatArtistAndAlbum(getResources(),
                    media.artist, media.album));
            mModelMedia.setTitle(media.title);
            mModelViewState.setNavigationButtonsVisible(true);
        } else {
            mModelMedia.setArtistAndAlbum(null);
            mModelMedia.setTitle(getText(R.string.Start_playing));

            bindArt(null);

            mModelPlaybackState.setProgress(0);
            mModelViewState.setNavigationButtonsVisible(false);
        }
    }

    private void bindArt(@Nullable final Bitmap albumArt) {
        if (albumArt == null) {
            mModelMedia.setArt(getActivity().getDrawable(R.drawable.album_art_placeholder));
        } else {
            mModelMedia.setArt(new BitmapDrawable(getResources(), albumArt));
        }
    }

    private void bindPlaybackState(@Nullable final WearPlaybackData.PlaybackState playbackState) {
        if (playbackState != null) {
            mModelViewState.setBtnPlayRes(playbackState.state == PlaybackState.STATE_PLAYING
                    || playbackState.state == PlaybackState.STATE_LOADING
                    ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_arrow_white_48dp);
        } else {
            mModelViewState.setBtnPlayRes(R.drawable.ic_play_arrow_white_48dp);
        }
    }

    private void bindPlaybackPosition(@Nullable final WearPlaybackData.PlaybackPosition pos) {
        if (pos != null) {
            bindProgress(pos.mediaId, pos.position);
        }
    }

    private void bindProgress(final long mediaId, final long elapsedTime) {
        final WearPlaybackData.Media media = mMediaHolder.getMedia();
        if (media != null && media.id == mediaId) {
            final long duration = media.duration;
            if (!mSeekBarTracking && duration > 0 && elapsedTime <= duration) {
                // Max is 200 so progress is a fraction of 200
                mModelPlaybackState
                        .setProgress((int) (((double) elapsedTime / (double) duration) * 200f));
            }
        }
    }

    @Override
    protected boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                mBtnPrev.performClick();
                return true;

            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                mBtnNext.performClick();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private final class OnSeekBarChangeListenerImpl implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int i, final boolean fromUser) {
            // stub
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
            mSeekBarTracking = true;
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            mSeekBarTracking = false;
            mRemoteControl.seek((float) seekBar.getProgress() / (float) seekBar.getMax());
        }
    }
}
