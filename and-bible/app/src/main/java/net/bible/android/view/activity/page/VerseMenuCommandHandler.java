package net.bible.android.view.activity.page;

import android.app.Activity;
import android.content.Intent;

import net.bible.android.activity.R;
import net.bible.android.control.ControlFactory;
import net.bible.android.control.PassageChangeMediator;
import net.bible.android.control.page.PageControl;
import net.bible.android.view.activity.base.ActivityBase;
import net.bible.android.view.activity.base.IntentHelper;
import net.bible.android.view.activity.comparetranslations.CompareTranslations;
import net.bible.android.view.activity.footnoteandref.FootnoteAndRefActivity;

import org.crosswire.jsword.passage.Verse;

/** Handle requests from the selected verse action menu
 * 
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author.
 */
public class VerseMenuCommandHandler {

	private final Activity mainActivity;

	private final PageControl pageControl;

	private final IntentHelper intentHelper = new IntentHelper();

	private static final String TAG = "VerseMenuCommandHandler";

	public VerseMenuCommandHandler(Activity mainActivity, PageControl pageControl) {
		super();
		this.mainActivity = mainActivity;
		this.pageControl = pageControl;
	}
	
	/**
     * on Click handler for Selected verse menu
     */
    public boolean handleMenuRequest(int menuItemId, Verse currentVerse) {
        boolean isHandled = false;

    	{
			Intent handlerIntent = null;
			int requestCode = ActivityBase.STD_REQUEST_CODE;

			// Handle item selection
			switch (menuItemId) {
				case R.id.compareTranslations:
					handlerIntent = new Intent(mainActivity, CompareTranslations.class);
					isHandled = true;
					break;
				case R.id.notes:
					handlerIntent = new Intent(mainActivity, FootnoteAndRefActivity.class);
					isHandled = true;
					break;
				case R.id.add_bookmark:
				case R.id.delete_bookmark:
					ControlFactory.getInstance().getBookmarkControl().toggleBookmarkForVerse(currentVerse);
					// refresh view to show new bookmark icon
					PassageChangeMediator.getInstance().forcePageUpdate();
					isHandled = true;
					break;
				case R.id.myNoteAddEdit:
					ControlFactory.getInstance().getCurrentPageControl().showMyNote(currentVerse);
					isHandled = true;
					break;
				case R.id.copy:
					pageControl.copyToClipboard(currentVerse);
					isHandled = true;
					break;
				case R.id.shareVerse:
					pageControl.shareVerse(currentVerse);
					isHandled = true;
					break;
			}

			if (handlerIntent!=null) {
				intentHelper.updateIntentWithVerse(handlerIntent, currentVerse);
				mainActivity.startActivityForResult(handlerIntent, requestCode);
			}
    	}

        return isHandled;
    }
 }
