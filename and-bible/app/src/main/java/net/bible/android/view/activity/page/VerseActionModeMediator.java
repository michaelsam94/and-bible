package net.bible.android.view.activity.page;

import android.content.Intent;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import net.bible.android.activity.R;
import net.bible.android.control.ControlFactory;
import net.bible.android.control.event.window.CurrentWindowChangedEvent;
import net.bible.android.control.page.PageControl;
import net.bible.android.view.activity.base.IntentHelper;

import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.passage.VerseRange;
import org.crosswire.jsword.versification.Versification;

import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Control the verse selection action mode
 *
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author.
 */
public class VerseActionModeMediator {

	private final ActionModeMenuDisplay mainBibleActivity;

	private final VerseHighlightControl bibleView;

	private final PageControl pageControl;

	private final VerseMenuCommandHandler verseMenuCommandHandler;

	private VerseNoRange verseNoRange;

    boolean isVerseActionMode;

	private ActionMode actionMode;

	private IntentHelper intentHelper = new IntentHelper();

    private static final String TAG = "VerseActionModeMediator";

	public VerseActionModeMediator(ActionModeMenuDisplay mainBibleActivity, VerseHighlightControl bibleView, PageControl pageControl, VerseMenuCommandHandler verseMenuCommandHandler) {
		this.mainBibleActivity = mainBibleActivity;
		this.bibleView = bibleView;
		this.pageControl = pageControl;
		this.verseMenuCommandHandler = verseMenuCommandHandler;

		// Be notified if the associated window loses focus
		EventBus.getDefault().register(this);
	}

	public void verseLongPress(int verse) {
        Log.d(TAG, "Verse selected event:"+verse);
        startVerseActionMode(verse);
    }

	/**
	 * Handle selection and deselection of extra verses after initial verse
	 */
	public void verseTouch(int verse) {
		Log.d(TAG, "Verse touched event:"+verse);
		VerseNoRange origRange = verseNoRange.clone();
		verseNoRange.alter(verse);

		Set<Integer> toSelect = origRange.getExtrasIn(verseNoRange);
		Set<Integer> toDeselect = verseNoRange.getExtrasIn(origRange);

		for (Integer verseNo: toSelect) {
			bibleView.highlightVerse(verseNo);
		}
		for (Integer verseNo: toDeselect) {
			bibleView.unhighlightVerse(verseNo);
		}
	}

    private void startVerseActionMode(int verse) {
		if (isVerseActionMode) {
			Log.i(TAG, "Action mode already started so ignoring restart.");
			return;
		}

		Log.i(TAG, "Start verse action mode. verse no:"+verse);
		isVerseActionMode = true;
		bibleView.highlightVerse(verse);
		this.verseNoRange = new VerseNoRange(verse);
		actionMode = mainBibleActivity.showVerseActionModeMenu(actionModeCallbackHandler);
		bibleView.enableVerseTouchSelection();
    }

	/**
	 * Ensure all state is left tidy
	 */
	private void endVerseActionMode() {
		if (isVerseActionMode) {
			isVerseActionMode = false;
			bibleView.clearVerseHighlight();
			bibleView.disableVerseTouchSelection();
			verseNoRange = null;

			// prevent endless loop by onDestroyActionMode calling this calling onDestroyActionMode etc.
			if (actionMode != null) {
				ActionMode finishingActionMode = this.actionMode;
				actionMode = null;
				finishingActionMode.finish();
			}
		}
	}

	private Verse getStartVerse() {
		if (verseNoRange==null) {
			return null;
		} else {
			Verse mainVerse = pageControl.getCurrentBibleVerse();
			return new Verse(mainVerse.getVersification(), mainVerse.getBook(), mainVerse.getChapter(), verseNoRange.getStartVerseNo());
		}
	}

	private VerseRange getVerseRange() {
		Verse startVerse = getStartVerse();
		if (startVerse==null) {
			return null;
		} else {
			Versification v11n = startVerse.getVersification();
			Verse endVerse = new Verse(v11n, startVerse.getBook(), startVerse.getChapter(), verseNoRange.getEndVerseNo());
			return new VerseRange(v11n, startVerse, endVerse);
		}
	}

	private ActionMode.Callback actionModeCallbackHandler = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			// Inflate our menu from a resource file
			actionMode.getMenuInflater().inflate(R.menu.verse_action_mode_menu, menu);

			// Return true so that the action mode is shown
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			// if start verse already bookmarked then enable Delete Bookmark menu item else Add Bookmark
			Verse startVerse = getStartVerse();
			boolean isVerseBookmarked = startVerse!=null && ControlFactory.getInstance().getBookmarkControl().isBookmarkForKey(getStartVerse());
			menu.findItem(R.id.add_bookmark).setVisible(!isVerseBookmarked);
			menu.findItem(R.id.delete_bookmark).setVisible(isVerseBookmarked);

			// must return true if menu changed
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			Log.i(TAG, "Action menu item clicked: " + menuItem);
			// Similar to menu handling in Activity.onOptionsItemSelected()

			verseMenuCommandHandler.handleMenuRequest(menuItem.getItemId(), getStartVerse() /*getVerseRange()*/);

			endVerseActionMode();

			// handle all
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			Log.i(TAG, "On destroy action mode");
			VerseActionModeMediator.this.actionMode = null;
			endVerseActionMode();
		}
	};

	public interface ActionModeMenuDisplay {
		ActionMode showVerseActionModeMenu(ActionMode.Callback actionModeCallbackHandler);

		void startActivityForResult(Intent intent, int requestCode);
	}

	public interface VerseHighlightControl {
		void enableVerseTouchSelection();
		void disableVerseTouchSelection();
		void highlightVerse(int verse);
		void unhighlightVerse(int verse);
		void clearVerseHighlight();
	}

	public void onEvent(CurrentWindowChangedEvent event) {
		endVerseActionMode();
	}
}
