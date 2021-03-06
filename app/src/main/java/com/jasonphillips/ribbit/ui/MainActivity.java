package com.jasonphillips.ribbit.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.jasonphillips.ribbit.R;
import com.jasonphillips.ribbit.adapters.SectionsPagerAdapter;
import com.parse.ParseAnalytics;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int TAKE_PHOTO_REQ = 0;
    public static final int TAKE_VIDEO_REQ = 1;
    public static final int CHOOSE_PHOTO_REQ = 2;
    public static final int CHOOSE_VIDEO_REQ = 3;
    public static final int MEDIA_TYPE_IMAGE = 4;
    public static final int MEDIA_TYPE_VIDEO = 5;
    public static final int FILE_SIZE_LIMIT = 1024 * 1024 * 10; //10mb
    private List<ParseObject> mMessages;

    protected Uri mMediaUri;

    public ParseUser mCurrentUser = ParseUser.getCurrentUser();

    protected DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0: //take a picture
                    takePicture();
                    break;
                case 1: //take video
                    takeVideo();
                    break;
                case 2: //choose picture
                    choseWithMimeType("image/*", CHOOSE_PHOTO_REQ);
                    break;
                case 3: //choose video
                    choseWithMimeType("video/*", CHOOSE_VIDEO_REQ);
                    break;
            }
        }
    };

    private void choseWithMimeType(String mimeType, int type) {
        Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        choosePhotoIntent.setType(mimeType);
        if (type == CHOOSE_VIDEO_REQ) {
            Toast.makeText(this, getString(R.string.warning_video_size), Toast.LENGTH_LONG).show();
        }
        startActivityForResult(choosePhotoIntent, type);
    }

    private void takeVideo() {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
        if (mMediaUri == null) {
            Toast.makeText(this, getString(R.string.error_could_not_take_video), Toast.LENGTH_LONG).show();
        } else {
            videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
            videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
            videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            startActivityForResult(videoIntent, TAKE_PHOTO_REQ);
        }
    }

    private void takePicture() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        if (mMediaUri == null) {
            Toast.makeText(this, getString(R.string.error_external_storage), Toast.LENGTH_LONG).show();
        } else {
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
            startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQ);
        }
    }

    private boolean isStorageAvailable() {

        String state = Environment.getExternalStorageState();

        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }

    }

    private Uri getOutputMediaFileUri(int mediaType) {

        String appName = getString(R.string.app_name);

        if (isStorageAvailable()) {
            File mediaStorageDirectory = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appName);

            if (!mediaStorageDirectory.exists()) {
                if (!mediaStorageDirectory.mkdirs()) {
                    Log.e(TAG, getString(R.string.error_failed_mk_dir));
                    return null;
                }
            }

            File mediaFile;
            Date now = new Date();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now);
            String path = mediaStorageDirectory.getPath() + File.separator;
            if (mediaType == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(path + "IMG_" + timeStamp + ".jpg");
            } else if (mediaType == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(path + "IMG_" + timeStamp + ".mp4");
            } else {
                return null;
            }

            Log.i(TAG, "" + Uri.fromFile(mediaFile));
            return Uri.fromFile(mediaFile);


        }

        return null;
    }


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        if (mCurrentUser == null) {
            navigateToLogin();
        }

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sectichons adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            //.setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setIcon(mSectionsPagerAdapter.getIcon(i))
                            .setTabListener(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_PHOTO_REQ || requestCode == CHOOSE_VIDEO_REQ) {
                if (data == null) {

                } else {
                    mMediaUri = data.getData();
                }

                int fileSize = 0;
                InputStream inputStream = null;
                try {
                    inputStream = getContentResolver().openInputStream(mMediaUri);
                    fileSize = inputStream.available();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } finally {
                        /*assert inputStream != null;*/
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (fileSize >= FILE_SIZE_LIMIT) {
                    Toast.makeText(this, getString(R.string.error_file_too_large), Toast.LENGTH_LONG).show();
                    return;
                }


            } else {
                Intent mediaScannedIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScannedIntent.setData(mMediaUri);
                sendBroadcast(mediaScannedIntent);
            }

            Intent recipientsIntent = new Intent(this, RecipientsActivity.class);
            if (requestCode == CHOOSE_PHOTO_REQ || requestCode == TAKE_PHOTO_REQ) {
                recipientsIntent.putExtra("fileType", "image");
            } else {
                recipientsIntent.putExtra("fileType", "video");
            }
            recipientsIntent.setData(mMediaUri);
            startActivity(recipientsIntent);

        } else if (resultCode != RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.error_activity_result), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_logout:
                ParseUser.logOut();
                mCurrentUser = ParseUser.getCurrentUser();
                navigateToLogin();
                break;
            case R.id.action_edit_friends:
                Intent intent = new Intent(this, EditFriendsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_camera:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(R.array.camera_choices, mDialogListener);
                builder.create().show();
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

}
