package com.jasonphillips.ribbit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonphillips.ribbit.Constants;
import com.jasonphillips.ribbit.FileHelper;
import com.jasonphillips.ribbit.R;
import com.jasonphillips.ribbit.adapters.UserAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RecipientsActivity extends Activity {

    private static final String TAG = RecipientsActivity.class.getSimpleName();
    private ParseUser mCurrentUser;
    private ParseRelation<ParseObject> mFriends;
    private List<ParseUser> mUsers;
    protected MenuItem mSendMenuItem;
    private Uri uri;
    private String mMediaType;
    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_grid);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mGridView = (GridView) findViewById(R.id.user_grid);
        mGridView.setOnItemClickListener(mOnItemClick);

        mGridView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        uri = getIntent().getData();
        mMediaType = getIntent().getStringExtra("fileType");

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyView);

    }

    protected AdapterView.OnItemClickListener mOnItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mGridView.getCheckedItemCount() > 0) {
                mSendMenuItem.setVisible(true);
            } else {
                mSendMenuItem.setVisible(false);
            }

            ImageView checkImageView = (ImageView) mGridView.findViewById(R.id.friend_checkbox);

            if (mGridView.isItemChecked(position)) {
                //add
                checkImageView.setVisibility(View.VISIBLE);
            } else {
                //remove
                checkImageView.setVisibility(View.INVISIBLE);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipients, menu);
        mSendMenuItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_send) {
            send();
        }

        return super.onOptionsItemSelected(item);
    }

    private void send() {
        ParseObject message = createMessage();
        if (message != null) {
            sendMessage(message);
            finish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.error_title)
                    .setMessage(getString(R.string.error_file_selected))
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();

        }
    }

    SaveCallback sendMessageCallback = new SaveCallback() {
        @Override
        public void done(ParseException e) {
            if (e == null) {
                Toast.makeText(RecipientsActivity.this, getString(R.string.success_message), Toast.LENGTH_LONG).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                builder.setTitle(R.string.error_title)
                        .setMessage(getString(R.string.error_sending_message))
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            }
        }
    };

    private void sendMessage(ParseObject message) {
        message.saveInBackground(sendMessageCallback);
    }

    private ParseObject createMessage() {
        ParseObject message = new ParseObject(Constants.PARSE_CLASS_MESSAGES);
        message.put(Constants.PARSE_KEY_MESSAGE_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(Constants.PARSE_KEY_MESSAGE_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        message.put(Constants.PARSE_KEY_MESSAGE_RECIPIENT_IDS, getRecipientIds());
        message.put(Constants.PARSE_KEY_MESSAGE_FILE, getFile());
        message.put(Constants.PARSE_KEY_MESSAGE_FILE_TYPE, mMediaType);

        return message;
    }

    private ParseFile getFile() {

        byte[] fileBytes = FileHelper.getByteArrayFromFile(this, uri);
        if (fileBytes == null) {
            return null;
        }

        if (mMediaType.equals(Constants.TYPE_IMAGE)) {
            fileBytes = FileHelper.reduceImageForUpload(fileBytes);
        }

        String fileName = FileHelper.getFileName(this, uri, mMediaType);
        return new ParseFile(fileName, fileBytes);
    }

    private ArrayList<String> getRecipientIds() {
        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < mGridView.getCount(); i++) {
            if (mGridView.isItemChecked(i)) {
                ids.add(mUsers.get(i).getObjectId());
            }
        }

        return ids;

    }

    @Override
    public void onResume() {
        super.onResume();

        mCurrentUser = ParseUser.getCurrentUser();
        mFriends = mCurrentUser.getRelation(Constants.PARSE_KEY_FRIENDS_RELATION);

        setProgressBarIndeterminateVisibility(true);

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.orderByAscending(Constants.PARSE_KEY_USERNAME);
        query.setLimit(1000);

        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                setProgressBarIndeterminateVisibility(false);
                if (e == null) {
                    //success
                    mUsers = parseUsers;
                    String[] usernames = new String[mUsers.size()];
                    int i = 0;
                    for (ParseUser user : mUsers) {
                        usernames[i] = user.getUsername();
                        i++;
                    }

                    if (mGridView.getAdapter() == null) {
                        UserAdapter adapter = new UserAdapter(
                                RecipientsActivity.this,
                                mUsers
                        );
                        mGridView.setAdapter(adapter);
                    } else {
                        ((UserAdapter) mGridView.getAdapter()).Refill(mUsers);
                    }


                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(e.getMessage())
                            .setTitle(R.string.error_title)
                            .setPositiveButton(getString(R.string.btn_ok), null);

                    builder.create().show();
                }
            }
        });
    }
}
