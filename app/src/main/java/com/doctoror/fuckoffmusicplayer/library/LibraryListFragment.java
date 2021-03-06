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
package com.doctoror.fuckoffmusicplayer.library;

import com.doctoror.commons.util.Log;
import com.doctoror.fuckoffmusicplayer.R;
import com.doctoror.fuckoffmusicplayer.databinding.FragmentLibraryListBinding;
import com.doctoror.fuckoffmusicplayer.util.ObserverAdapter;
import com.doctoror.fuckoffmusicplayer.util.SearchViewUtils;
import com.doctoror.fuckoffmusicplayer.util.SoftInputManager;
import com.doctoror.fuckoffmusicplayer.util.ViewUtils;
import com.doctoror.fuckoffmusicplayer.widget.SwipeDirectionTouchListener;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;

import org.parceler.Parcel;
import org.parceler.Parcels;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Fragment used for library list
 */
public abstract class LibraryListFragment extends LibraryPermissionsFragment {

    private static final String TAG = "LibraryListFragment";

    private static final String KEY_INSTANCE_STATE = "LibraryListFragment.INSTANCE_STATE";

    private static final int ANIMATOR_CHILD_PROGRESS = 0;
    private static final int ANIMATOR_CHILD_PERMISSION_DENIED = 1;
    private static final int ANIMATOR_CHILD_EMPTY = 2;
    private static final int ANIMATOR_CHILD_ERROR = 3;
    private static final int ANIMATOR_CHILD_CONTENT = 4;

    private final BehaviorSubject<String> mSearchSubject = BehaviorSubject.create();
    private final LibraryListFragmentModel mModel = new LibraryListFragmentModel();

    private Subscription mOldSubscription;
    private Subscription mSubscription;

    private boolean mCanShowEmptyView = true;
    private boolean mSearchIconified = true;

    private RecyclerView mRecyclerView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    private void restoreInstanceState(@NonNull final Bundle savedInstanceState) {
        final InstanceState state = Parcels.unwrap(savedInstanceState
                .getParcelable(KEY_INSTANCE_STATE));
        if (state != null) {
            mSearchIconified = state.searchIconified;
            mSearchSubject.onNext(state.searchQuery);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        final InstanceState state = new InstanceState();
        state.searchIconified = mSearchIconified;
        state.searchQuery = mSearchSubject.getValue();
        outState.putParcelable(KEY_INSTANCE_STATE, Parcels.wrap(state));
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_library_list, menu);
        if (hasPermissions()) {
            final SearchView searchView = (SearchView) menu.findItem(R.id.actionFilter)
                    .getActionView();
            SearchViewUtils.setSearchIcon(searchView, R.drawable.ic_filter_list_white_24dp);
            searchView.setQueryHint(getText(R.string.Filter));

            searchView.setQuery(mSearchSubject.getValue(), false);
            searchView.setOnCloseListener(() -> {
                mSearchIconified = true;
                return false;
            });
            searchView.setOnSearchClickListener((v) -> mSearchIconified = false);
            searchView.setIconified(mSearchIconified);

            RxSearchView
                    .queryTextChanges(searchView)
                    .debounce(400, TimeUnit.MILLISECONDS)
                    .subscribe(t -> mSearchSubject.onNext(t.toString()));

        } else {
            menu.findItem(R.id.actionFilter).setVisible(false);
        }
    }

    @Override
    protected void onPermissionGranted() {
        mModel.setDisplayedChild(ANIMATOR_CHILD_PROGRESS);
        registerOnStartSubscription(mSearchSubject.asObservable().subscribe(mSearchQueryObserver));
        getActivity().invalidateOptionsMenu();
    }

    @Override
    protected void onPermissionDenied() {
        mModel.setDisplayedChild(ANIMATOR_CHILD_PERMISSION_DENIED);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        final FragmentLibraryListBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_library_list, container, false);
        setupRecyclerView(binding.recyclerView);
        binding.setModel(mModel);
        binding.recyclerView.setOnTouchListener(new SwipeDirectionTouchListener() {

            @Override
            protected void onSwipedDown() {
                SoftInputManager.hideSoftInput(getActivity());
            }
        });
        binding.getRoot().findViewById(R.id.btnRequest)
                .setOnClickListener(v -> requestPermission());
        mRecyclerView = binding.recyclerView;
        return binding.getRoot();
    }

    @Nullable
    protected final View getItemView(final int position) {
        return ViewUtils.getItemView(mRecyclerView, position);
    }

    protected void setupRecyclerView(@NonNull final RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onStop() {
        super.onStop();
        SoftInputManager.hideSoftInput(getActivity());
        onDataReset();
    }

    private void restartLoader(@Nullable final String searchFilter) {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mOldSubscription = mSubscription;
            mSubscription = registerOnStartSubscription(load(searchFilter)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mObserver));
        } else {
            Log.w(TAG, "restartLoader is called, READ_EXTERNAL_STORAGE is not granted");
        }
    }

    protected abstract Observable<Cursor> load(@Nullable final String filter);

    protected abstract void onDataLoaded(@NonNull Cursor data);

    protected abstract void onDataReset();

    protected final void setCanShowEmptyView(final boolean canShowEmptyView) {
        mCanShowEmptyView = canShowEmptyView;
    }

    protected final void setRecyclerAdapter(@Nullable final RecyclerView.Adapter<?> adapter) {
        mModel.setRecyclerAdapter(adapter);
    }

    protected final void setEmptyMessage(@Nullable final CharSequence emptyMessage) {
        mModel.setEmptyMessage(emptyMessage);
    }

    private final Observer<Cursor> mObserver = new ObserverAdapter<Cursor>() {

        @Override
        public void onError(final Throwable e) {
            if (mOldSubscription != null) {
                mOldSubscription.unsubscribe();
                mOldSubscription = null;
            }
            onDataReset();
            if (isAdded()) {
                mModel.setDisplayedChild(ANIMATOR_CHILD_ERROR);
            }
        }

        @Override
        public void onNext(final Cursor cursor) {
            onDataLoaded(cursor);
            if (mOldSubscription != null) {
                mOldSubscription.unsubscribe();
                mOldSubscription = null;
            }
            mModel.setDisplayedChild(cursor.getCount() == 0 && mCanShowEmptyView
                    ? ANIMATOR_CHILD_EMPTY : ANIMATOR_CHILD_CONTENT);
        }
    };

    private final Action1<String> mSearchQueryObserver = this::restartLoader;

    @Parcel
    static final class InstanceState {

        String searchQuery;
        boolean searchIconified;
    }

}
