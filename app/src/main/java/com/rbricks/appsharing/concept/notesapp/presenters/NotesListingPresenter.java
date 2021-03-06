package com.rbricks.appsharing.concept.notesapp.presenters;

import android.app.Activity;
import android.content.Intent;
import android.util.Pair;
import android.widget.Toast;

import com.rbricks.appsharing.R;
import com.rbricks.appsharing.concept.notesapp.activities.NotesDetailsActivity;
import com.rbricks.appsharing.concept.notesapp.db.DatabaseManager;
import com.rbricks.appsharing.concept.notesapp.dialogs.NotesDeleteDialog;
import com.rbricks.appsharing.concept.notesapp.dialogs.NotesOptionsDialog;
import com.rbricks.appsharing.concept.notesapp.domains.NotesItem;
import com.rbricks.appsharing.concept.notesapp.interfaces.BasePresenterInterface;
import com.rbricks.appsharing.concept.notesapp.interfaces.MVPNotesInterface;
import com.rbricks.appsharing.concept.notesapp.utils.RxApiUtil;


import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

import static com.rbricks.appsharing.utils.CommonUtils.doUnsubscribe;

/**
 * Created by gopikrishna on 11/11/16.
 */

public class NotesListingPresenter implements BasePresenterInterface {

    private Activity context;
    private MVPNotesInterface mvpNotesInterface;
    private boolean isSubscribed;
    private CompositeDisposable lifeCycle;

    public NotesListingPresenter(MVPNotesInterface mvpNotesInterface, Activity context) {
        this.mvpNotesInterface = mvpNotesInterface;
        this.context = context;
        this.isSubscribed = true;
        lifeCycle = new CompositeDisposable();
    }

    public void fetchNotesList() {
        mvpNotesInterface.showProgress();
        lifeCycle.add(RxApiUtil.build(DatabaseManager.getAllNotes()).subscribe((list) -> {
            mvpNotesInterface.hideProgress();
            mvpNotesInterface.showAllNotes(list);
        }));
    }

    public void fetchAllNotesListAfterSearch() {
        mvpNotesInterface.showProgress();
        lifeCycle.add(RxApiUtil.build(DatabaseManager.getAllNotes()).subscribe((list) -> {
            mvpNotesInterface.hideProgress();
            // This will make refresh whole adapter.
            mvpNotesInterface.showAllNotes(list, true);
        }));
    }

    public void fetchSearchedNotesList(String searchString) {
        mvpNotesInterface.showProgress();
        lifeCycle.add(RxApiUtil.build(DatabaseManager.getAllSearchedNotes(searchString)).subscribe((list) -> {
            mvpNotesInterface.hideProgress();
            mvpNotesInterface.showAllNotes(list);
        }));
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void onAddClick() {
        Intent intent = new Intent(context, NotesDetailsActivity.class);
        intent.putExtra(NotesDetailsActivity.ADD_FLOW, true);
        context.startActivityForResult(intent, 1);
    }

    public void onItemLongClicked(Pair<NotesItem, Integer> pair) {
        NotesOptionsDialog dialog = new NotesOptionsDialog();
        PublishSubject<Boolean> deleteClickedSubject = dialog.createOptionsDialog(context, pair.first);
        lifeCycle.add(deleteClickedSubject.subscribe(s -> {
            NotesDeleteDialog deleteDialog = new NotesDeleteDialog();
            lifeCycle.add(deleteDialog.createDeleteItemAlertDialog(context).subscribe(bool -> {
                doItemRemoveLogic(pair);
            }));
        }));
    }

    public void onItemClicked(Pair<NotesItem, Integer> pair) {
        Intent intent = new Intent(context, NotesDetailsActivity.class);
        intent.putExtra(NotesDetailsActivity.NOTES_ITEM, pair.first);
        intent.putExtra(NotesDetailsActivity.ITEM_POSITION, pair.second);
        context.startActivityForResult(intent, 1);
    }

    public void onItemClickedInSearch(Pair<NotesItem, Integer> pair) {
        Intent intent = new Intent(context, NotesDetailsActivity.class);
        intent.putExtra(NotesDetailsActivity.NOTES_ITEM, pair.first);
        intent.putExtra(NotesDetailsActivity.SEARCH_FLOW, true);
        context.startActivityForResult(intent, 1);
    }

    private void doItemRemoveLogic(Pair<NotesItem, Integer> pair) {
        lifeCycle.add(RxApiUtil.build(DatabaseManager.softDeleteNotes(pair.first.getId())).subscribe(isSuccess -> {
            if (isSuccess) {
                int index = pair.second;
                mvpNotesInterface.removeNotesOfPosition(index);
            } else {
                Toast.makeText(context, R.string.oops_error, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    @Override
    public void unSubscribe() {
        isSubscribed = false;
        doUnsubscribe(lifeCycle);
    }

}
