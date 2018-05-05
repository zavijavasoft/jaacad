package com.zavijavasoft.jaacad;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yandex.authsdk.YandexAuthException;
import com.yandex.authsdk.YandexAuthSdk;
import com.yandex.authsdk.YandexAuthToken;
import com.zavijavasoft.jaacad.auth.AuthService;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;


public class GalleryActivity extends AppCompatActivity
        implements OperationalDelegate, AppBarLayout.OnOffsetChangedListener {

    public static final String TAG_GALLERY_FRAGMENT = "gallery_fragment";
    public static final String KEY_INTERNET_APPROVED = "KEY_INTERNET_APPROVED";
    private static final String KEY_IS_LOADING_STATE = "com.zavijavasoft.jaacad.IS_LOADING_STATE";
    private static final String TAG = "GalleryActivity";
    private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f;
    private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f;
    private static final int ALPHA_ANIMATIONS_DURATION = 200;
    private final Handler handler = new Handler();
    boolean blockNetworkExceptions = false;
    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;
    private boolean loadingProgressPending = false;
    private boolean connectedToInternet = false;
    private AuthService authService;
    private GalleryResultReceiver resultReceiver;
    private int queryResultSize = 0;
    private int entityCounter = 0;
    private boolean isLoading;


    private LinearLayout galleryContainer;
    private LinearLayout footer;
    private TextView mTitle;
    private AppBarLayout appBarLayout;
    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    private AppCompatSpinner spinner;
    private ProgressBar mainProgressBar;
    private ImageButton queryButton;
    private CircleImageView avatarView;

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authService = AuthService.getInstance(getApplicationContext());

        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(KEY_IS_LOADING_STATE);
            resultReceiver = savedInstanceState.getParcelable(CoreService.KEY_INTENT_RECEIVER);
        } else {
            resultReceiver = new GalleryResultReceiver();
        }
        resultReceiver.setActivity(this);

        setContentView(R.layout.activity_gallery);

        galleryContainer = findViewById(R.id.gallery_container);
        appBarLayout = findViewById(R.id.main_appbar);
        toolbar = findViewById(R.id.main_toolbar);
        footer = findViewById(R.id.footer);

        mTitle = findViewById(R.id.main_textview_title);


        // получаем экземпляр FragmentTransaction
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        if (null == fragmentManager.findFragmentByTag(TAG_GALLERY_FRAGMENT)) {
            // добавляем фрагмент
            GalleryFragment galleryFragment = new GalleryFragment();
            fragmentTransaction.add(R.id.gallery_container, galleryFragment, TAG_GALLERY_FRAGMENT);
        }
        fragmentTransaction.commit();

        //appBarLayout.addOnOffsetChangedListener(this);

        //toolbar.inflateMenu(R.menu.menu_main);
        setSupportActionBar(toolbar);

        //startAlphaAnimation(mTitle, 0, View.INVISIBLE);

        avatarView = findViewById(R.id.circle_avatar_view);
        mainProgressBar = findViewById(R.id.main_progress_bar);
        mainProgressBar.setVisibility(View.INVISIBLE);
        setUpSpinner();
        setUpQueryButton();

        startAuthorization();
    }

    @Override
    protected void onResume() {
        performCheckInternet();
        super.onResume();
    }

    private void startAuthorization() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = preferences.getString(AuthService.USERNAME, null);
        final String token = preferences.getString(AuthService.TOKEN, null);

        if (token != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    authService.setCredentials(username, token);
                    String sFileName = getResources().getString(R.string.avatar_filename_template, username);
                    File avatarFileName = new File(getApplicationContext().getFilesDir(),
                            new File(sFileName).getName());
                    avatarView.setImageDrawable(Drawable.createFromPath(avatarFileName.toString()));
                    authService.setAuthorized(true);
                    performLoadCached();
                }
            });
        } else {
            authService.setDefaultCredentials();
            authService.setAuthorized(false);
            performCommandByDefault();

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        final YandexAuthSdk sdk = authService.getYandexAuthSdk();
        if (requestCode == 1) {
            try {
                final YandexAuthToken yandexAuthToken =
                        sdk.extractToken(resultCode, data);
                if (yandexAuthToken != null) {

                    performAuthRequest(yandexAuthToken.getValue());
                }
            } catch (YandexAuthException e) {
                Log.d("auth.err", "ayth.ere", e);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void setUpQueryButton() {
        queryButton = findViewById(R.id.make_query_button);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSelectedCommand();
            }
        });
    }

    private void performSelectedCommand() {
        String sQuery = (String) spinner.getSelectedItem();
        int cmd = getQueryCommandByString(sQuery);
        if (!CoreService.queryNeedAuthorization(cmd) || authService.isAuthorized()) {
            GalleryFragment fragment =
                    (GalleryFragment) getFragmentManager().findFragmentByTag(TAG_GALLERY_FRAGMENT);
            fragment.performClear();
            Intent intent = new Intent(getApplicationContext(), CoreService.class);
            intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
            intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, cmd);
            fragment.performClear();
            startService(intent);
        } else {
            Set<String> scopes = new HashSet<>();
            startActivityForResult(authService.getYandexAuthSdk()
                    .createLoginIntent(getApplicationContext(), scopes), 1);
        }
    }

    public void performCheckInternet() {
        Intent intent = new Intent(getApplicationContext(), CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.CHECK_INTERNET_CONNECTION);
        startService(intent);
    }


    public void performAuthRequest(String token) {
        Intent intent = new Intent(getApplicationContext(), CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.GET_LOGIN_INFO);
        intent.putExtra(CoreService.KEY_INTENT_AUTH_TOKEN, token);
        startService(intent);
    }

    private void updateGalleryFragment(GalleryEntity ge) {
        GalleryFragment fragment = (GalleryFragment) getFragmentManager().findFragmentByTag(TAG_GALLERY_FRAGMENT);
        fragment.performUpdate(ge);
    }

    private int getQueryCommandByString(String sQuery) {
        int nResourceId = -1;
        if (sQuery.equals(getResources().getString(R.string.query_100_firstauthorized)))
            nResourceId = R.string.query_100_firstauthorized;

        else if (sQuery.equals(getResources().getString(R.string.query_100_lastauthorized)))
            nResourceId = R.string.query_100_lastauthorized;

        else if (sQuery.equals(getResources().getString(R.string.query_100_firstpublic)))
            nResourceId = R.string.query_100_firstpublic;

        else if (sQuery.equals(getResources().getString(R.string.query_100_lastpublic)))
            nResourceId = R.string.query_100_lastpublic;

        else if (sQuery.equals(getResources().getString(R.string.query_100_randomauthorized)))
            nResourceId = R.string.query_100_randomauthorized;
        return CoreService.getQueryCmdByResourceId(nResourceId);
    }

    private void setUpSpinner() {
        spinner = findViewById(R.id.query_spinner);
        final String[] queries = getResources().getStringArray(R.array.queries);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_row, R.id.spinner_row_text, queries);

        // Вызываем адаптер
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (authService.isAuthorized())
                    return;

                String[] queries = getResources().getStringArray(R.array.queries);

                int cmd = getQueryCommandByString(queries[position]);
                if (CoreService.queryNeedAuthorization(cmd)) {
                    Snackbar.make(spinner, R.string.need_authorization_message,
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_clear_cache:
                Intent intent = new Intent(getApplicationContext(), CoreService.class);
                intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
                intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.CLEAR_CACHE);
                startService(intent);
                return true;
            case R.id.action_logout:
                logoutAuthorization();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        outState.putBoolean(KEY_IS_LOADING_STATE, isLoading);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void showImage(String thumbnailFilename, String fileName, String id) {
        if (loadingProgressPending)
            return;
        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.KEY_THUMBNAIL_FILENAME, thumbnailFilename);
        intent.putExtra(ImageActivity.KEY_IMAGE_FILENAME, fileName);
        intent.putExtra(ImageActivity.KEY_IMAGE_ID, id);
        startActivity(intent);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if (!mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        } else {

            if (mIsTheTitleVisible) {
                startAlphaAnimation(mTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                startAlphaAnimation(galleryContainer, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(galleryContainer, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    public void onProgressBegin(int count) {
        loadingProgressPending = true;
        mainProgressBar.setVisibility(View.VISIBLE);
        mainProgressBar.setMax(count);
        mainProgressBar.setProgress(0);
    }


    public void onProgress(int count, int total) {
        if (mainProgressBar.getMax() != total)
            mainProgressBar.setMax(total);
        mainProgressBar.setProgress(count);
    }


    public void onProgressComplete() {
        loadingProgressPending = false;
        mainProgressBar.setVisibility(View.INVISIBLE);
    }

    private int performLoadCached(){
        Intent intent = new Intent(this, CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_CACHED);
        startService(intent);
    }

    private int performCommandByDefault() {
        boolean bAuthorized =
                AuthService.getInstance(getApplicationContext())
                        .isAuthorized();
        int nCmd = bAuthorized
                ? CoreService.LOAD_LAST_100_AUTHORIZED
                : CoreService.LOAD_LAST_100_PUBLIC;
        Intent intent = new Intent(this, CoreService.class);
        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, resultReceiver);
        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, nCmd);
        startService(intent);
        return nCmd;
    }


    public void setQueryCommand(int queryCmd) {
        String sValue = getResources().getString(CoreService.getQueryResourceIdByCmd(queryCmd));
        if (sValue != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(sValue)) {
                    spinner.setSelection(i, true);
                    break;
                }
            }
        }
    }

    public void logoutAuthorization() {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.remove(AuthService.USERNAME);
        editor.remove(AuthService.TOKEN);
        editor.apply();
        authService.setDefaultCredentials();
        authService.setAuthorized(false);
        avatarView.setImageDrawable(getResources().getDrawable(R.drawable.ic_gallery_icon));
    }

    public void completeAuthorization(String displayName, String prettyName, String
            avatarName, String token) {
        avatarView.setImageDrawable(Drawable.createFromPath(avatarName));
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString(AuthService.USERNAME, displayName);
        editor.putString(AuthService.TOKEN, token);
        editor.apply();
        authService.setCredentials(displayName, token);
        authService.setAuthorized(true);
        performSelectedCommand();
    }


    private static class GalleryResultReceiver extends ResultReceiver {

        transient private GalleryActivity A;

        public GalleryResultReceiver() {
            super(new Handler());
        }

        public void setActivity(GalleryActivity activity) {
            A = activity;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (A == null)
                return;

            switch (resultCode) {
                case CoreService.INTERNET_OK: {
                    if (!A.connectedToInternet) {
                        A.connectedToInternet = true;
                        A.startAlphaAnimation(A.footer, 1000, View.GONE);
                    }
                    break;
                }
                case CoreService.NETWORK_EXCEPTION:
                case CoreService.INTERNET_LOST: {
                    if (A.connectedToInternet) {
                        A.connectedToInternet = false;
                        A.startAlphaAnimation(A.footer, 1000, View.VISIBLE);
                    }
                    A.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            A.performCheckInternet();
                        }
                    }, 5000);
                    break;
                }
                case CoreService.RESULT_SIZE: {
                    A.queryResultSize = resultData.getInt(CoreService.KEY_RESULT_SIZE);
                    A.entityCounter = 0;
                    if (A.queryResultSize == 0) {
                        A.performCommandByDefault();
                    } else {
                        A.onProgressBegin(A.queryResultSize);
                    }
                    break;
                }
                case CoreService.CHUNK_LOADED: {
                    int nShift = resultData.getInt(CoreService.KEY_RESULT_SIZE);
                    if (nShift > 0) {
                        int nDiff = A.queryResultSize - A.entityCounter;
                        int nPermil = (5 * nDiff) / 100;
                        A.entityCounter += nPermil;
                        A.onProgress(A.entityCounter, A.queryResultSize);
                    } else {
                        A.onProgressComplete();
                    }
                    break;
                }
                case CoreService.CACHED_ENTITY_LOADED: {
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    A.updateGalleryFragment(ge);
                    if (ge.getState() == GalleryEntity.State.ONCE_LOADED) {
                        Intent intent = new Intent(A, CoreService.class);
                        intent.putExtra(CoreService.KEY_INTENT_RECEIVER, A.resultReceiver);
                        intent.putExtra(CoreService.KEY_INTENT_QUERY_TYPE, CoreService.LOAD_SINGLE_THUMBNAIL);
                        intent.putExtra(CoreService.KEY_REQUEST_ID, ge.getResourceId());
                        A.startService(intent);
                    } else {
                        A.entityCounter++;
                        A.onProgress(A.entityCounter, A.queryResultSize);
                        if (A.entityCounter == A.queryResultSize)
                            A.onProgressComplete();
                    }
                    break;
                }
                case CoreService.THUMBNAIL_LOADED:
                    GalleryEntity ge = resultData.getParcelable(CoreService.KEY_RESULT_GALLERY_ENTITY);
                    A.updateGalleryFragment(ge);
                    A.entityCounter++;

                    A.onProgress(A.entityCounter, A.queryResultSize);
                    if (A.entityCounter == A.queryResultSize)
                        A.onProgressComplete();
                    break;

                case CoreService.RESOURCE_MISSED:
                    if (A.queryResultSize == 0)
                        break;
                    A.queryResultSize--;
                    if (A.queryResultSize == 0) {
                        A.onProgressComplete();
                        A.performCommandByDefault();
                    } else
                        A.onProgress(A.entityCounter, A.queryResultSize);
                    break;
                case CoreService.LOGIN_INFO: {
                    String sAvatarFileName = resultData.getString(CoreService.KEY_RESULT_LOGIN_AVATAR_FILENAME);
                    String sDisplayName = resultData.getString(CoreService.KEY_RESULT_LOGIN_DISPLAY_NAME);
                    String sPrettyName = resultData.getString(CoreService.KEY_RESULT_LOGIN_PRETTY_NAME);
                    String sToken = resultData.getString(CoreService.KEY_INTENT_AUTH_TOKEN);
                    A.completeAuthorization(sDisplayName, sPrettyName, sAvatarFileName, sToken);
                    break;
                }
            }
            super.onReceiveResult(resultCode, resultData);
        }

    }

}



