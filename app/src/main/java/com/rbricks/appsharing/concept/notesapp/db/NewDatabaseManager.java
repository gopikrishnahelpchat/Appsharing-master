package com.rbricks.appsharing.concept.notesapp.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.rbricks.appsharing.concept.Application.AppSharingApplication;
import com.rbricks.appsharing.concept.notesapp.contentprovider.NotesContentProvider;
import com.rbricks.appsharing.concept.notesapp.domains.NotesItem;
import com.rbricks.appsharing.concept.notesapp.utils.SqlliteTables.NotesListingTable;

import java.util.ArrayList;
import java.util.List;


import io.reactivex.Observable;

import static com.rbricks.appsharing.utils.CommonUtils.getCurrentDateTimeForDb;
import static com.rbricks.appsharing.utils.CommonUtils.isNullOrEmpty;

/**
 * Created by gopikrishna on 12/11/16.
 */

public class NewDatabaseManager {

    public static Observable<Long> insertNotes(NotesItem notes) {
        return Observable.create(subscriber -> {
            long result;
            try {
                String currentDateTimeForDb = getCurrentDateTimeForDb();
                ContentValues contentValues = new ContentValues();
                contentValues.put(NotesListingTable.TITLE, notes.getTitle());
                contentValues.put(NotesListingTable.DESCRIPTION, notes.getDescription());
                contentValues.put(NotesListingTable.UPDATED_ON, currentDateTimeForDb);
                contentValues.put(NotesListingTable.CREATED_ON, currentDateTimeForDb);
                Uri notesUri = getNotesUri();
                Uri insertedUri = AppSharingApplication.getInstance().getContentResolver().insert(notesUri, contentValues);
                String lastPathSegment = insertedUri.getLastPathSegment();
                result = Long.parseLong(lastPathSegment);
//                result = AppSharingApplication.getInstance().getWritableDB().insert(NotesListingTable.TABLE_NAME, null, contentValues);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("exception in insertion", e.getLocalizedMessage());
                result = -1;
            }
            subscriber.onNext(result);
        });
    }

    private static Uri getNotesUri() {
//        return Uri.parse("content://" + NotesContentProvider.PROVIDER_NAME);
        return NotesContentProvider.NOTES_URI;
    }

    public static Observable<Boolean> updateNotes(NotesItem notes) {
        return Observable.create(subscriber -> {
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(NotesListingTable.TITLE, notes.getTitle());
                contentValues.put(NotesListingTable.DESCRIPTION, notes.getDescription());
                contentValues.put(NotesListingTable.UPDATED_ON, getCurrentDateTimeForDb());

                /*int i = AppSharingApplication.getInstance().getWritableDB().update(NotesListingTable.TABLE_NAME,
                        contentValues,
                        NotesListingTable.ID + " = ?",
                        new String[]{String.valueOf(notes.getId())});*/
                int i = AppSharingApplication.getInstance().getContentResolver().update(getNotesUri(), contentValues, NotesListingTable.ID + " = ?",
                        new String[]{String.valueOf(notes.getId())});
                subscriber.onNext(i != -1);
            } catch (Exception e) {
                e.printStackTrace();
                subscriber.onNext(false);
            }
        });
    }

    public static boolean hardDeleteNotes(int id) {
        int i = AppSharingApplication.getInstance().getWritableDB().delete(NotesListingTable.TABLE_NAME, NotesListingTable.ID + " =?",
                new String[]{String.valueOf(id)});
        return i != -1;
    }

    public static Observable<Boolean> softDeleteNotes(int id) {
        return Observable.create(subscriber -> {
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(NotesListingTable.IS_DELETED, "1");

                int i = AppSharingApplication.getInstance().getContentResolver().update(getNotesUri(),
                        contentValues,
                        NotesListingTable.ID + " = ?",
                        new String[]{String.valueOf(id)});
                subscriber.onNext(i != -1);
            } catch (Exception e) {
                e.printStackTrace();
                subscriber.onNext(false);
            }
        });
    }

    public static Observable<List<NotesItem>> getAllNotes() {
        return Observable.create(subscriber -> {
            List<NotesItem> notesItems = new ArrayList<>();
            /*Cursor cursor = AppSharingApplication.getInstance().getReadableDb()
                    .rawQuery("SELECT * FROM " + NotesListingTable.TABLE_NAME + " WHERE " + NotesListingTable.IS_DELETED + " = ? ORDER BY " + NotesListingTable.UPDATED_ON + " DESC ", new String[]{"0"});*/
            Cursor cursor = AppSharingApplication.getInstance().getContentResolver()
                    .query(getNotesUri(), null, NotesListingTable.IS_DELETED + " = ?", new String[]{"0"}, NotesListingTable.UPDATED_ON + " DESC ");
            try {
                while (cursor.moveToNext()) {
                    NotesItem notesItem = new NotesItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(4), cursor.getInt(5));
                    notesItems.add(notesItem);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            subscriber.onNext(notesItems);
        });
    }

    public static Observable<List<NotesItem>> getAllSearchedNotes(String searchTerm) {
        if (isNullOrEmpty(searchTerm)) {
            return Observable.just(new ArrayList<>());
        }
        return Observable.create(subscriber -> {
            List<NotesItem> notesItems = new ArrayList<>();
            String regexSearchTerm = "%" + searchTerm + "%";
            Cursor cursor = AppSharingApplication.getInstance().getContentResolver()
                    .query(getNotesUri(), null, NotesListingTable.IS_DELETED + " = ? AND (" + NotesListingTable.TITLE + " LIKE ?  OR "
                                    + NotesListingTable.DESCRIPTION + " LIKE ? )"
                            , new String[]{"0", regexSearchTerm, regexSearchTerm}, NotesListingTable.UPDATED_ON + " DESC ");
            try {
                while (cursor.moveToNext()) {
                    NotesItem notesItem = new NotesItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(4), cursor.getInt(5));
                    notesItems.add(notesItem);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
            System.out.println("notesItems in getAllSearchedNotes = " + notesItems);
            subscriber.onNext(notesItems);
        });
    }

    public static Observable<NotesItem> getNoteById(long id) {
        return Observable.create(subscriber -> {
            Cursor cursor = AppSharingApplication.getInstance().getContentResolver()
                    .query(getNotesUri(), null, NotesListingTable.ID + " = ?", new String[]{id + ""}, NotesListingTable.UPDATED_ON + " DESC  limit 1 ");
            try {
                while (cursor.moveToNext()) {
                    NotesItem notesItem = new NotesItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(4), cursor.getInt(5));
                    subscriber.onNext(notesItem);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
            subscriber.onNext(null);
        });
    }

}
